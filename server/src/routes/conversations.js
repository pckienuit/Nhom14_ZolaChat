const express = require('express');
const router = express.Router();
const { authenticateUser, db } = require('../middleware/auth');

router.get('/', authenticateUser, async (req, res) => {
  try {
    const snapshot = await db.collection('conversations')
      .where('participants', 'array-contains', req.user.uid)
      .orderBy('lastMessageTime', 'desc').limit(50).get();
    const conversations = [];
    snapshot.forEach(doc => conversations.push({ id: doc.id, ...doc.data() }));
    res.json({ conversations });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.post('/', authenticateUser, async (req, res) => {
  try {
    const { participants, isGroup, groupName } = req.body;
    const conversation = {
      participants, isGroup, groupName,
      createdAt: Date.now(),
      lastMessage: '',
      lastMessageTime: Date.now()
    };
    const convRef = await db.collection('conversations').add(conversation);
    res.json({ success: true, conversationId: convRef.id });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
