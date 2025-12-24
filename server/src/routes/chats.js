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
    
    if (global.io) broadcastMessage(global.io, conversationId, fullMessage);
    
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

module.exports = router;
