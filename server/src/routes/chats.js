const express = require('express');
const router = express.Router();
const { authenticateUser, db } = require('../middleware/auth');
const { broadcastMessage } = require('../websocket');

router.get('/:conversationId/messages', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { limit = 50 } = req.query;
    const snapshot = await db.collection('conversations').doc(conversationId).collection('messages')
      .orderBy('timestamp', 'desc').limit(parseInt(limit)).get();
    const messages = [];
    snapshot.forEach(doc => messages.push({ id: doc.id, ...doc.data() }));
    // Reverse to ascending order (oldest first) for chat UI
    messages.reverse();
    res.json({ messages });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.post('/:conversationId/messages', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { 
      content, 
      type = 'TEXT',
      senderId, 
      senderName,
      // File metadata
      fileName,
      fileSize,
      fileMimeType,
      // Reply fields
      replyToId,
      replyToContent,
      replyToSenderId,
      replyToSenderName,
      // Forward fields
      isForwarded,
      originalSenderId,
      originalSenderName,
      // Contact
      contactUserId,
      // Location
      latitude,
      longitude,
      locationName,
      locationAddress,
      // Live location
      liveLocationSessionId,
      // Sticker
      stickerId,
      stickerPackId,
      stickerUrl,
      isStickerAnimated
    } = req.body;
    
    // Build message object with only defined fields (no undefined!)
    const message = {
      content: content || '',
      type: type,
      senderId: senderId || req.user.uid,
      timestamp: Date.now(),
      isRead: false
    };
    
    // Add optional fields only if they exist
    if (senderName) message.senderName = senderName;
    
    // File metadata
    if (fileName) message.fileName = fileName;
    if (fileSize) message.fileSize = fileSize;
    if (fileMimeType) message.fileMimeType = fileMimeType;
    
    // Reply fields
    if (replyToId) {
      message.replyToId = replyToId;
      if (replyToContent) message.replyToContent = replyToContent;
      if (replyToSenderId) message.replyToSenderId = replyToSenderId;
      if (replyToSenderName) message.replyToSenderName = replyToSenderName;
    }
    
    // Forward fields
    if (isForwarded) {
      message.isForwarded = true;
      if (originalSenderId) message.originalSenderId = originalSenderId;
      if (originalSenderName) message.originalSenderName = originalSenderName;
    }
    
    // Contact
    if (contactUserId) message.contactUserId = contactUserId;
    
    // Location
    if (latitude !== undefined && longitude !== undefined) {
      message.latitude = latitude;
      message.longitude = longitude;
      if (locationName) message.locationName = locationName;
      if (locationAddress) message.locationAddress = locationAddress;
    }
    
    // Live location
    if (liveLocationSessionId) message.liveLocationSessionId = liveLocationSessionId;
    
    // Sticker
    if (stickerId) {
      message.stickerId = stickerId;
      if (stickerPackId) message.stickerPackId = stickerPackId;
      if (stickerUrl) message.stickerUrl = stickerUrl;
      if (isStickerAnimated !== undefined) message.isStickerAnimated = isStickerAnimated;
    }
    
    const messageRef = await db.collection('conversations').doc(conversationId)
      .collection('messages').add(message);
    
    await db.collection('conversations').doc(conversationId).update({
      lastMessage: content || `[${type}]`,
      lastMessageTime: message.timestamp
    });
    
    const fullMessage = { id: messageRef.id, ...message };
    fullMessage.conversationId = conversationId; // Add for WebSocket filtering
    
    if (global.io) {
        broadcastMessage(global.io, conversationId, fullMessage);

        // Notification for Home Screen Preview (emit to each user's room)
         try {
            // We need to fetch the conversation to get members if we don't have them handy
            // Or we can query db.collection('conversations').doc(conversationId)
            const conversationDoc = await db.collection('conversations').doc(conversationId).get();
            if (conversationDoc.exists) {
              const data = conversationDoc.data();
              // Support multiple field names for legacy compatibility
              const members = data.memberIds || data.participantIds || data.participants || [];
              
              console.log(`DEBUG_CHATS: Conversation ${conversationId} has members: ${JSON.stringify(members)}`);
              
              members.forEach(memberId => {
                global.io.to(`user:${memberId}`).emit('new_message', fullMessage);
                console.log(`DEBUG_CHATS: Emitted to user:${memberId}`);
              });
            }
          } catch (err) {
            console.error('Failed to send notifications to members in chats.js', err);
          }
    }
    
    res.json({ 
      success: true, 
      data: fullMessage 
    });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Recall a message
router.post('/:conversationId/messages/:messageId/recall', authenticateUser, async (req, res) => {
  try {
    const { conversationId, messageId } = req.params;
    
    const messageRef = db.collection('conversations').doc(conversationId)
      .collection('messages').doc(messageId);
    
    const messageDoc = await messageRef.get();
    if (!messageDoc.exists) {
      return res.status(404).json({ success: false, message: 'Message not found' });
    }
    
    // Update message to recalled state
    await messageRef.update({
      isRecalled: true,
      content: 'Tin nhắn đã bị thu hồi'
    });
    
    // Get updated message
    const updatedDoc = await messageRef.get();
    const updatedMessage = { id: updatedDoc.id, ...updatedDoc.data(), conversationId };
    
    // Broadcast update via WebSocket
    if (global.io) {
      console.log('Broadcasting message_updated for recall:', updatedMessage.id, 'to room:', `conversation:${conversationId}`);
      global.io.to(`conversation:${conversationId}`).emit('message_updated', updatedMessage);
    }
    
    res.json({ 
      success: true, 
      data: updatedMessage 
    });
  } catch (error) {
    console.error('Recall error:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// Add/update reaction to a message
router.post('/:conversationId/messages/:messageId/react', authenticateUser, async (req, res) => {
  try {
    const { conversationId, messageId } = req.params;
    const { userId, reactionType } = req.body;
    
    if (!userId || !reactionType) {
      return res.status(400).json({ success: false, message: 'userId and reactionType are required' });
    }
    
    const validReactions = ['heart', 'haha', 'sad', 'angry', 'wow', 'like'];
    if (!validReactions.includes(reactionType)) {
      return res.status(400).json({ success: false, message: 'Invalid reaction type' });
    }
    
    const messageRef = db.collection('conversations').doc(conversationId)
      .collection('messages').doc(messageId);
    
    const messageDoc = await messageRef.get();
    if (!messageDoc.exists) {
      return res.status(404).json({ success: false, message: 'Message not found' });
    }
    
    const messageData = messageDoc.data();
    const reactions = messageData.reactions || {};
    const reactionCounts = messageData.reactionCounts || {};
    
    // If user already had a different reaction, decrement old count
    const oldReaction = reactions[userId];
    if (oldReaction && oldReaction !== reactionType) {
      reactionCounts[oldReaction] = Math.max(0, (reactionCounts[oldReaction] || 0) - 1);
      if (reactionCounts[oldReaction] === 0) {
        delete reactionCounts[oldReaction];
      }
    }
    
    // Add/update new reaction
    reactions[userId] = reactionType;
    reactionCounts[reactionType] = (reactionCounts[reactionType] || 0) + 1;
    
    // Update Firestore
    await messageRef.update({ reactions, reactionCounts });
    
    // Get updated message
    const updatedDoc = await messageRef.get();
    const updatedMessage = { id: updatedDoc.id, ...updatedDoc.data(), conversationId };
    
    // Broadcast reaction update via WebSocket
    if (global.io) {
      console.log('Broadcasting reaction_updated:', messageId, 'to room:', `conversation:${conversationId}`);
      global.io.to(`conversation:${conversationId}`).emit('reaction_updated', {
        conversationId,
        messageId,
        userId,
        reactionType,
        reactions: updatedMessage.reactions,
        reactionCounts: updatedMessage.reactionCounts
      });
    }
    
    res.json({ 
      success: true, 
      data: updatedMessage 
    });
  } catch (error) {
    console.error('Reaction error:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// Remove reaction from a message
router.delete('/:conversationId/messages/:messageId/react', authenticateUser, async (req, res) => {
  try {
    const { conversationId, messageId } = req.params;
    const { userId } = req.query;
    
    if (!userId) {
      return res.status(400).json({ success: false, message: 'userId is required' });
    }
    
    const messageRef = db.collection('conversations').doc(conversationId)
      .collection('messages').doc(messageId);
    
    const messageDoc = await messageRef.get();
    if (!messageDoc.exists) {
      return res.status(404).json({ success: false, message: 'Message not found' });
    }
    
    const messageData = messageDoc.data();
    const reactions = messageData.reactions || {};
    const reactionCounts = messageData.reactionCounts || {};
    
    const oldReaction = reactions[userId];
    if (oldReaction) {
      // Remove user's reaction
      delete reactions[userId];
      
      // Decrement count
      reactionCounts[oldReaction] = Math.max(0, (reactionCounts[oldReaction] || 0) - 1);
      if (reactionCounts[oldReaction] === 0) {
        delete reactionCounts[oldReaction];
      }
      
      // Update Firestore
      await messageRef.update({ reactions, reactionCounts });
    }
    
    // Get updated message
    const updatedDoc = await messageRef.get();
    const updatedMessage = { id: updatedDoc.id, ...updatedDoc.data(), conversationId };
    
    // Broadcast reaction update via WebSocket
    if (global.io) {
      global.io.to(`conversation:${conversationId}`).emit('reaction_updated', {
        conversationId,
        messageId,
        userId,
        reactionType: null,
        reactions: updatedMessage.reactions,
        reactionCounts: updatedMessage.reactionCounts
      });
    }
    
    res.json({ 
      success: true, 
      data: updatedMessage 
    });
  } catch (error) {
    console.error('Remove reaction error:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

// Mark conversation as seen/read
router.post('/:conversationId/seen', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { userId } = req.body;
    
    if (!userId) {
      return res.status(400).json({ success: false, message: 'userId is required' });
    }
    
    const conversationRef = db.collection('conversations').doc(conversationId);
    const conversationDoc = await conversationRef.get();
    
    if (!conversationDoc.exists) {
      return res.status(404).json({ success: false, message: 'Conversation not found' });
    }
    
    const conversationData = conversationDoc.data();
    const seenBy = conversationData.seenBy || {};
    
    // Update seen status with timestamp
    seenBy[userId] = Date.now();
    
    await conversationRef.update({ seenBy });
    
    // Broadcast seen update via WebSocket
    if (global.io) {
      console.log('Broadcasting message_read:', conversationId, 'by user:', userId);
      global.io.to(`conversation:${conversationId}`).emit('message_read', {
        conversationId,
        userId,
        timestamp: seenBy[userId]
      });
    }
    
    res.json({ 
      success: true,
      data: { conversationId, userId, timestamp: seenBy[userId] }
    });
  } catch (error) {
    console.error('Mark seen error:', error);
    res.status(500).json({ success: false, message: error.message });
  }
});

module.exports = router;

