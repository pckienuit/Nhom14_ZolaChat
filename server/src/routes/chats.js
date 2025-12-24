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
    res.json({ messages });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.post('/:conversationId/messages', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { content, type, mediaUrl, metadata } = req.body;
    const message = {
      content, type, mediaUrl, metadata,
      senderId: req.user.uid,
      timestamp: Date.now(),
      isRead: false
    };
    const messageRef = await db.collection('conversations').doc(conversationId)
      .collection('messages').add(message);
    await db.collection('conversations').doc(conversationId).update({
      lastMessage: content || `[${type}]`,
      lastMessageTime: message.timestamp
    });
    const fullMessage = { id: messageRef.id, ...message };
    if (global.io) broadcastMessage(global.io, conversationId, fullMessage);
    res.json({ success: true, messageId: messageRef.id, message: fullMessage });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
