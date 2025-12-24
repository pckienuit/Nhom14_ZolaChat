const express = require('express');
const router = express.Router();
const { authenticateUser, requireAdmin, db } = require('../middleware/auth');

router.get('/:userId', authenticateUser, async (req, res) => {
  try {
    const { userId } = req.params;
    const userDoc = await db.collection('users').doc(userId).get();
    if (!userDoc.exists) return res.status(404).json({ error: 'User not found' });
    res.json({ id: userDoc.id, ...userDoc.data() });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.put('/:userId', authenticateUser, async (req, res) => {
  try {
    const { userId } = req.params;
    if (req.user.uid !== userId && req.user.role !== 'admin') {
      return res.status(403).json({ error: 'Forbidden' });
    }
    const { name, bio, avatarUrl, phone } = req.body;
    const updates = {};
    if (name) updates.name = name;
    if (bio !== undefined) updates.bio = bio;
    if (avatarUrl) updates.avatarUrl = avatarUrl;
    if (phone) updates.phone = phone;
    await db.collection('users').doc(userId).update(updates);
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.post('/:userId/status', authenticateUser, async (req, res) => {
  try {
    const { userId } = req.params;
    if (req.user.uid !== userId) return res.status(403).json({ error: 'Forbidden' });
    const { isOnline } = req.body;
    await db.collection('users').doc(userId).update({
      isOnline: !!isOnline,
      lastSeen: Date.now()
    });
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.get('/', authenticateUser, requireAdmin, async (req, res) => {
  try {
    const snapshot = await db.collection('users').orderBy('createdAt', 'desc').limit(100).get();
    const users = [];
    snapshot.forEach(doc => users.push({ id: doc.id, ...doc.data() }));
    res.json({ users });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.post('/:userId/ban', authenticateUser, requireAdmin, async (req, res) => {
  try {
    const { userId } = req.params;
    const { banned } = req.body;
    const updateData = {
      isBanned: !!banned,
      bannedAt: banned ? Date.now() : null
    };
    if (banned) {
      updateData.forceLogoutAt = Date.now();
      updateData.isOnline = false;
    }
    await db.collection('users').doc(userId).update(updateData);
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
