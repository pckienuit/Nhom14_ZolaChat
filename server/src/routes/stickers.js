const express = require('express');
const router = express.Router();
const { optionalAuth, authenticateUser, requireAdmin, db } = require('../middleware/auth');

router.get('/packs', optionalAuth, async (req, res) => {
  try {
    const snapshot = await db.collection('stickerPacks').orderBy('name').get();
    const packs = [];
    snapshot.forEach(doc => packs.push({ id: doc.id, ...doc.data() }));
    res.json({ packs });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.get('/packs/:packId', optionalAuth, async (req, res) => {
  try {
    const { packId } = req.params;
    const packDoc = await db.collection('stickerPacks').doc(packId).get();
    if (!packDoc.exists) return res.status(404).json({ error: 'Not found' });
    
    const stickersSnapshot = await db.collection('stickerPacks')
      .doc(packId).collection('stickers').get();
    const stickers = [];
    stickersSnapshot.forEach(doc => stickers.push({ id: doc.id, ...doc.data() }));
    
    res.json({ id: packDoc.id, ...packDoc.data(), stickers });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.post('/packs', authenticateUser, requireAdmin, async (req, res) => {
  try {
    const { name, description, category, stickers } = req.body;
    const pack = {
      name, description: description || '', category: category || 'general',
      downloadCount: 0, createdAt: Date.now()
    };
    const packRef = await db.collection('stickerPacks').add(pack);
    res.json({ success: true, packId: packRef.id });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
