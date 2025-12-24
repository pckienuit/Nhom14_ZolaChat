const express = require('express');
const router = express.Router();
const { authenticateUser, db, admin } = require('../middleware/auth');

router.get('/', authenticateUser, async (req, res) => {
  try {
    const userDoc = await db.collection('users').doc(req.user.uid).get();
    const friends = userDoc.exists ? (userDoc.data().friends || []) : [];
    res.json({ friends });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.get('/requests', authenticateUser, async (req, res) => {
  try {
    const snapshot = await db.collection('friendRequests')
      .where('receiverId', '==', req.user.uid)
      .where('status', '==', 'pending').get();
    const requests = [];
    snapshot.forEach(doc => requests.push({ id: doc.id, ...doc.data() }));
    res.json({ requests });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.post('/requests', authenticateUser, async (req, res) => {
  try {
    const { receiverId } = req.body;
    const request = {
      senderId: req.user.uid,
      receiverId,
      status: 'pending',
      createdAt: Date.now()
    };
    const requestRef = await db.collection('friendRequests').add(request);
    res.json({ success: true, requestId: requestRef.id });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.put('/requests/:requestId', authenticateUser, async (req, res) => {
  try {
    const { requestId } = req.params;
    const { action } = req.body;
    const requestDoc = await db.collection('friendRequests').doc(requestId).get();
    if (!requestDoc.exists) return res.status(404).json({ error: 'Not found' });
    
    const requestData = requestDoc.data();
    if (action === 'accept') {
      const batch = db.batch();
      batch.update(db.collection('users').doc(requestData.senderId), {
        friends: admin.firestore.FieldValue.arrayUnion(requestData.receiverId)
      });
      batch.update(db.collection('users').doc(requestData.receiverId), {
        friends: admin.firestore.FieldValue.arrayUnion(requestData.senderId)
      });
      batch.update(db.collection('friendRequests').doc(requestId), { status: 'accepted' });
      await batch.commit();
    } else {
      await db.collection('friendRequests').doc(requestId).update({ status: 'rejected' });
    }
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
