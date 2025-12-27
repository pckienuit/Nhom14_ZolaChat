const express = require('express');
const router = express.Router();
const { authenticateUser, requireAdmin, db, admin } = require('../middleware/auth');

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
    const { name, bio, avatarUrl, coverUrl, phone } = req.body;
    const updates = {};
    if (name) updates.name = name;
    if (bio !== undefined) updates.bio = bio;
    if (avatarUrl !== undefined) updates.avatarUrl = avatarUrl; // Allow empty string to remove avatar
    if (coverUrl !== undefined) updates.coverUrl = coverUrl; // Allow empty string to remove cover
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

// Search users by name or email
router.post('/search', authenticateUser, async (req, res) => {
  try {
    const { query } = req.body;
    if (!query || query.length < 2) {
      return res.json({ users: [] });
    }
    
    const queryLower = query.toLowerCase();
    
    // Search by name (case-insensitive)
    const nameSnapshot = await db.collection('users')
      .orderBy('nameLowerCase')
      .startAt(queryLower)
      .endAt(queryLower + '\uf8ff')
      .limit(20)
      .get();
    
    // Search by email
    const emailSnapshot = await db.collection('users')
      .where('email', '>=', queryLower)
      .where('email', '<=', queryLower + '\uf8ff')
      .limit(20)
      .get();
    
    // Merge results and remove duplicates
    const usersMap = new Map();
    nameSnapshot.forEach(doc => {
      usersMap.set(doc.id, { id: doc.id, ...doc.data() });
    });
    emailSnapshot.forEach(doc => {
      if (!usersMap.has(doc.id)) {
        usersMap.set(doc.id, { id: doc.id, ...doc.data() });
      }
    });
    
    const users = Array.from(usersMap.values());
    
    console.log(`🔍 Search query: "${query}" - Found ${users.length} users`);
    res.json({ users });
  } catch (error) {
    console.error('Search error:', error);
    res.status(500).json({ error: error.message });
  }
});

// Get multiple users by IDs (batch)
router.post('/batch', authenticateUser, async (req, res) => {
  try {
    const { userIds } = req.body;
    
    if (!userIds || !Array.isArray(userIds) || userIds.length === 0) {
      return res.json({ users: [] });
    }
    
    // Firestore 'in' query limit is 10, so we need to batch
    const batchSize = 10;
    const batches = [];
    
    for (let i = 0; i < userIds.length; i += batchSize) {
      const batch = userIds.slice(i, i + batchSize);
      batches.push(
        db.collection('users')
          .where(admin.firestore.FieldPath.documentId(), 'in', batch)
          .get()
      );
    }
    
    const snapshots = await Promise.all(batches);
    const users = [];
    
    snapshots.forEach(snapshot => {
      snapshot.forEach(doc => {
        users.push({ id: doc.id, ...doc.data() });
      });
    });
    
    res.json({ users });
  } catch (error) {
    console.error('Batch get users error:', error);
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
