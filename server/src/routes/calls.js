const express = require('express');
const router = express.Router();
const { authenticateUser, db } = require('../middleware/auth');

router.get('/', authenticateUser, async (req, res) => {
  try {
    const snapshot = await db.collection('calls')
      .where('participants', 'array-contains', req.user.uid)
      .orderBy('createdAt', 'desc').limit(50).get();
    const calls = [];
    snapshot.forEach(doc => calls.push({ id: doc.id, ...doc.data() }));
    res.json({ calls });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.post('/', authenticateUser, async (req, res) => {
  try {
    const { receiverId, callType, duration, status } = req.body;
    const call = {
      callerId: req.user.uid,
      receiverId, callType, duration: duration || 0, status: status || 'missed',
      participants: [req.user.uid, receiverId],
      createdAt: Date.now()
    };
    const callRef = await db.collection('calls').add(call);
    res.json({ success: true, callId: callRef.id });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
