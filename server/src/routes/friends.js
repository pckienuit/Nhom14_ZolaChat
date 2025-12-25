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
    const { receiverId, senderName, senderEmail } = req.body;
    
    // Check for existing pending request
    const existingPending = await db.collection('friendRequests')
      .where('senderId', '==', req.user.uid)
      .where('receiverId', '==', receiverId)
      .where('status', '==', 'pending')
      .get();
    
    if (!existingPending.empty) {
      return res.status(400).json({ error: 'Friend request already pending' });
    }
    
    // Delete any old rejected requests between these users
    const existingRejected = await db.collection('friendRequests')
      .where('senderId', '==', req.user.uid)
      .where('receiverId', '==', receiverId)
      .where('status', '==', 'rejected')
      .get();
    
    const batch = db.batch();
    existingRejected.forEach(doc => {
      batch.delete(doc.ref);
    });
    
    // Fetch sender info if not provided
    let fromUserName = senderName;
    let fromUserEmail = senderEmail;
    
    if (!fromUserName || !fromUserEmail) {
      const senderDoc = await db.collection('users').doc(req.user.uid).get();
      if (senderDoc.exists) {
        const senderData = senderDoc.data();
        fromUserName = fromUserName || senderData.name;
        fromUserEmail = fromUserEmail || senderData.email;
      }
    }
    
    const request = {
      senderId: req.user.uid,
      receiverId,
      fromUserName: fromUserName || 'Unknown User',
      fromUserEmail: fromUserEmail || '',
      status: 'pending',
      createdAt: Date.now()
    };
    
    const requestRef = db.collection('friendRequests').doc();
    batch.set(requestRef, request);
    
    await batch.commit();
    
    // Notify receiver via WebSocket that they have a new friend request
    const io = req.app.get('io');
    if (io) {
      console.log(`📤 Emitting friend_request_received to user:${receiverId}`);
      io.to(`user:${receiverId}`).emit('friend_request_received', {
        requestId: requestRef.id,
        senderId: req.user.uid,
        senderName: fromUserName,
        senderEmail: fromUserEmail
      });
    }
    
    console.log(`📤 Friend request sent from ${fromUserName} to ${receiverId}`);
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
    const io = req.app.get('io');
    
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
      
      // Notify both users via WebSocket
      if (io) {
        io.to(`user:${requestData.senderId}`).emit('friend_request_accepted', {
          userId: requestData.receiverId,
          requestId
        });
        io.to(`user:${requestData.receiverId}`).emit('friend_added', {
          userId: requestData.senderId,
          requestId
        });
        console.log('✅ Notified users of friend request acceptance');
      }
    } else {
      await db.collection('friendRequests').doc(requestId).update({ status: 'rejected' });
      
      // Notify sender of rejection
      if (io) {
        console.log(`📤 Emitting friend_request_rejected to user:${requestData.senderId}`);
        io.to(`user:${requestData.senderId}`).emit('friend_request_rejected', {
          userId: requestData.receiverId,
          requestId
        });
        console.log('✅ Notified sender of friend request rejection');
      } else {
        console.log('❌ io is null, cannot emit friend_request_rejected');
      }
    }
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Remove/unfriend a user
router.delete('/:friendId', authenticateUser, async (req, res) => {
  try {
    const { friendId } = req.params;
    const userId = req.user.uid;
    
    console.log('👋 Removing friendship between', userId, 'and', friendId);
    
    const batch = db.batch();
    
    // Remove from both users' friends arrays
    batch.update(db.collection('users').doc(userId), {
      friends: admin.firestore.FieldValue.arrayRemove(friendId)
    });
    batch.update(db.collection('users').doc(friendId), {
      friends: admin.firestore.FieldValue.arrayRemove(userId)
    });
    
    await batch.commit();
    
    // Notify both users via WebSocket
    const io = req.app.get('io');
    if (io) {
      io.to(`user:${userId}`).emit('friend_removed', { userId: friendId });
      io.to(`user:${friendId}`).emit('friend_removed', { userId });
      console.log('✅ Notified users of friendship removal');
    }
    
    console.log('✅ Friendship removed');
    res.json({ success: true });
  } catch (error) {
    console.error('❌ Error removing friend:', error);
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
