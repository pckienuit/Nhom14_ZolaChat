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
    if (!query || query.length < 1) {  // Cho phép tìm từ 1 ký tự
      return res.json({ users: [] });
    }
    
    const queryLower = query.toLowerCase().trim();
    console.log(`🔍 Search query: "${query}" (normalized: "${queryLower}")`);
    
    // Vì Firestore không hỗ trợ substring search, ta phải lấy nhiều users và filter
    // Lấy tất cả users (hoặc limit cao) để có thể filter
    const allUsersSnapshot = await db.collection('users')
      .limit(500)  // Lấy tối đa 500 users
      .get();
    
    const matchedUsers = [];
    
    allUsersSnapshot.forEach(doc => {
      const userData = doc.data();
      const name = (userData.name || '').toLowerCase();
      const email = (userData.email || '').toLowerCase();
      const nameLowerCase = (userData.nameLowerCase || '').toLowerCase();
      
      // Kiểm tra nếu query có trong name hoặc email (substring matching)
      if (name.includes(queryLower) || 
          email.includes(queryLower) || 
          nameLowerCase.includes(queryLower)) {
        matchedUsers.push({ id: doc.id, ...userData });
      }
    });
    
    // Sắp xếp kết quả: ưu tiên những kết quả match ở đầu
    matchedUsers.sort((a, b) => {
      const aName = (a.name || '').toLowerCase();
      const bName = (b.name || '').toLowerCase();
      const aEmail = (a.email || '').toLowerCase();
      const bEmail = (b.email || '').toLowerCase();
      
      const aNameStartsWith = aName.startsWith(queryLower);
      const bNameStartsWith = bName.startsWith(queryLower);
      const aEmailStartsWith = aEmail.startsWith(queryLower);
      const bEmailStartsWith = bEmail.startsWith(queryLower);
      
      // Ưu tiên name starts with > email starts with > name contains > email contains
      if (aNameStartsWith && !bNameStartsWith) return -1;
      if (!aNameStartsWith && bNameStartsWith) return 1;
      if (aEmailStartsWith && !bEmailStartsWith) return -1;
      if (!aEmailStartsWith && bEmailStartsWith) return 1;
      
      return 0;
    });
    
    // Giới hạn kết quả trả về (top 20)
    const limitedUsers = matchedUsers.slice(0, 20);
    
    console.log(`✅ Search found ${matchedUsers.length} users (returning ${limitedUsers.length})`);
    res.json({ users: limitedUsers });
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
