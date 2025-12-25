const express = require('express');
const router = express.Router();
const { auth } = require('../middleware/auth');

router.get('/verify', async (req, res) => {
  try {
    const authHeader = req.headers.authorization;
    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ valid: false });
    }
    const token = authHeader.split('Bearer ')[1];
    const decodedToken = await auth.verifyIdToken(token);
    res.json({ valid: true, uid: decodedToken.uid });
  } catch (error) {
    res.status(401).json({ valid: false, message: error.message });
  }
});

router.post('/set-admin', async (req, res) => {
  try {
    const { uid } = req.body;
    if (!uid) return res.status(400).json({ error: 'uid required' });
    await auth.setCustomUserClaims(uid, { role: 'admin' });
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
