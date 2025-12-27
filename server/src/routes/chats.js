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
      isStickerAnimated,
      // Voice message
      voiceUrl,
      voiceDuration
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

    // Voice message
    if (voiceUrl) {
      message.voiceUrl = voiceUrl;
      if (voiceDuration !== undefined) message.voiceDuration = voiceDuration;
    }
    
    const messageRef = await db.collection('conversations').doc(conversationId)
      .collection('messages').add(message);
    
    await db.collection('conversations').doc(conversationId).update({
      lastMessage: content || `[${type}]`,
      lastMessageTime: message.timestamp,
      timestamp: message.timestamp // Update timestamp for sorting
    });
    
    // Increment unreadCounts for all members except sender
    const admin = require('firebase-admin');
    try {
      const convDoc = await db.collection('conversations').doc(conversationId).get();
      if (convDoc.exists) {
        const convData = convDoc.data();
        const members = convData.memberIds || convData.participantIds || [];
        const currentSenderId = senderId || req.user.uid;
        const unreadUpdates = {};
        
        console.log(`📊 [UNREAD] Conversation ${conversationId} members: ${JSON.stringify(members)}, sender: ${currentSenderId}`);
        
        members.forEach(memberId => {
          if (memberId !== currentSenderId) {
            unreadUpdates['unreadCounts.' + memberId] = admin.firestore.FieldValue.increment(1);
            console.log(`📊 [UNREAD] Will increment unreadCounts for member: ${memberId}`);
          }
        });
        
        if (Object.keys(unreadUpdates).length > 0) {
          await db.collection('conversations').doc(conversationId).update(unreadUpdates);
          console.log(`📊 [UNREAD] ✅ Successfully incremented unreadCounts for ${Object.keys(unreadUpdates).length} members`);
        }
      }
    } catch (unreadErr) {
      console.error('❌ [UNREAD] Failed to update unreadCounts:', unreadErr);
    }
    
    const fullMessage = { id: messageRef.id, ...message };
    fullMessage.conversationId = conversationId; // Add for WebSocket filtering
    
    if (global.io) {
        broadcastMessage(global.io, conversationId, fullMessage);

        // Notification for Home Screen Preview (emit to each user's room)
         try {
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
    
    console.log(`❤️ Adding/updating reaction: user=${userId}, type=${reactionType}, message=${messageId}`);
    
    const messageRef = db.collection('conversations').doc(conversationId)
      .collection('messages').doc(messageId);
    
    const messageDoc = await messageRef.get();
    if (!messageDoc.exists) {
      return res.status(404).json({ success: false, message: 'Message not found' });
    }
    
    const messageData = messageDoc.data();
    const reactions = messageData.reactions || {};
    const reactionCounts = messageData.reactionCounts || {};
    
    // Get user's current reaction (if any)
    const oldReaction = reactions[userId];
    
    // Only update if reaction changed or user has no reaction yet
    if (oldReaction !== reactionType) {
      // If user had a different reaction, decrement old count
      if (oldReaction) {
        reactionCounts[oldReaction] = Math.max(0, (reactionCounts[oldReaction] || 0) - 1);
        if (reactionCounts[oldReaction] === 0) {
          delete reactionCounts[oldReaction];
        }
        console.log(`📉 Decremented ${oldReaction} count`);
      }
      
      // Add new reaction and increment count
      reactions[userId] = reactionType;
      reactionCounts[reactionType] = (reactionCounts[reactionType] || 0) + 1;
      console.log(`📈 Set ${reactionType} for user, count now: ${reactionCounts[reactionType]}`);
      
      // Update Firestore
      await messageRef.update({ reactions, reactionCounts });
    } else {
      console.log(`ℹ️ User already has this reaction type, no change needed`);
    }
    
    // Get updated message
    const updatedDoc = await messageRef.get();
    const updatedMessage = { id: updatedDoc.id, ...updatedDoc.data(), conversationId };
    
    // Broadcast reaction update via WebSocket
    if (global.io) {
      const roomName = `conversation:${conversationId}`;
      const socketsInRoom = global.io.sockets.adapter.rooms.get(roomName);
      const numClientsInRoom = socketsInRoom ? socketsInRoom.size : 0;
      
      console.log(`📡 Broadcasting reaction_updated to room: ${roomName}`);
      console.log(`📡 Number of clients in room: ${numClientsInRoom}`);
      console.log(`📡 Data: messageId=${messageId}, reactions=${JSON.stringify(updatedMessage.reactions)}, counts=${JSON.stringify(updatedMessage.reactionCounts)}`);
      
      global.io.to(roomName).emit('reaction_updated', {
        conversationId,
        messageId,
        userId,
        reactionType,
        reactions: updatedMessage.reactions || {},
        reactionCounts: updatedMessage.reactionCounts || {}
      });
      
      console.log(`✅ Broadcast sent to ${numClientsInRoom} clients`);
    } else {
      console.log('⚠️ global.io is not set!');
    }
    
    res.json({ 
      success: true, 
      data: updatedMessage 
    });
  } catch (error) {
    console.error('Reaction error:', error);
    res.status(500).json({ success: false, message: error.message });;
  }
});

// Remove reaction from a message (removes ALL reactions of the specified user)
router.delete('/:conversationId/messages/:messageId/react', authenticateUser, async (req, res) => {
  try {
    const { conversationId, messageId } = req.params;
    const { userId } = req.query;
    
    if (!userId) {
      return res.status(400).json({ success: false, message: 'userId is required' });
    }
    
    console.log(`🗑️ Removing ALL reactions of user ${userId} on message ${messageId}`);
    
    const messageRef = db.collection('conversations').doc(conversationId)
      .collection('messages').doc(messageId);
    
    const messageDoc = await messageRef.get();
    if (!messageDoc.exists) {
      return res.status(404).json({ success: false, message: 'Message not found' });
    }
    
    const messageData = messageDoc.data();
    const reactions = messageData.reactions || {};
    const reactionCounts = messageData.reactionCounts || {};
    const reactionsDetailed = messageData.reactionsDetailed || {};
    
    let hasChanges = false;
    
    // Handle old format: reactions = { userId: reactionType }
    const oldReaction = reactions[userId];
    if (oldReaction) {
      // Remove user's reaction from simple map
      delete reactions[userId];
      
      // Decrement count for old reaction type
      reactionCounts[oldReaction] = Math.max(0, (reactionCounts[oldReaction] || 0) - 1);
      if (reactionCounts[oldReaction] === 0) {
        delete reactionCounts[oldReaction];
      }
      hasChanges = true;
    }
    
    // Handle new format: reactionsDetailed = { userId: { reactionType: count, ... } }
    if (reactionsDetailed[userId]) {
      const userReactions = reactionsDetailed[userId];
      
      // Decrement counts for all user's reaction types
      for (const reactionType in userReactions) {
        const count = userReactions[reactionType] || 0;
        reactionCounts[reactionType] = Math.max(0, (reactionCounts[reactionType] || 0) - count);
        if (reactionCounts[reactionType] === 0) {
          delete reactionCounts[reactionType];
        }
      }
      
      // Remove user from detailed reactions
      delete reactionsDetailed[userId];
      hasChanges = true;
    }
    
    if (hasChanges) {
      // Update Firestore
      await messageRef.update({ reactions, reactionCounts, reactionsDetailed });
      console.log(`✅ Removed all reactions of user ${userId}`);
    } else {
      console.log(`ℹ️ User ${userId} had no reactions to remove`);
    }
    
    // Get updated message
    const updatedDoc = await messageRef.get();
    const updatedMessage = { id: updatedDoc.id, ...updatedDoc.data(), conversationId };
    
    // Broadcast reaction update via WebSocket
    if (global.io) {
      const roomName = `conversation:${conversationId}`;
      const socketsInRoom = global.io.sockets.adapter.rooms.get(roomName);
      const numClientsInRoom = socketsInRoom ? socketsInRoom.size : 0;
      
      console.log(`📡 Broadcasting reaction_updated (remove) to room: ${roomName}`);
      console.log(`📡 Number of clients in room: ${numClientsInRoom}`);
      
      global.io.to(roomName).emit('reaction_updated', {
        conversationId,
        messageId,
        userId,
        reactionType: null,
        reactions: updatedMessage.reactions || {},
        reactionCounts: updatedMessage.reactionCounts || {}
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
    
    // Reset unreadCounts for this user to 0
    const updateData = { 
      seenBy,
      [`unreadCounts.${userId}`]: 0
    };
    
    await conversationRef.update(updateData);
    console.log(`📊 [SEEN] Reset unreadCounts for user ${userId} in conversation ${conversationId}`);
    
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

