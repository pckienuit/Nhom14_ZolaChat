const express = require('express');
const router = express.Router();
const { authenticateUser, db } = require('../middleware/auth');

// DEBUG: Get all conversations (no filter) to check structure
router.get('/debug/all', authenticateUser, async (req, res) => {
  try {
    const snapshot = await db.collection('conversations').limit(10).get();
    const conversations = [];
    snapshot.forEach(doc => {
      const data = doc.data();
      conversations.push({ 
        id: doc.id, 
        participants: data.participants,
        participantIds: data.participantIds,
        type: data.type,
        isGroup: data.isGroup,
        fields: Object.keys(data)
      });
    });
    res.json({ total: snapshot.size, conversations });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

router.get('/', authenticateUser, async (req, res) => {
  try {
    console.log('📋 Fetching conversations for user:', req.user.uid);
    
    // Try with orderBy first (requires Firestore composite index)
    let snapshot;
    try {
      // IMPORTANT: Query memberIds (old structure) instead of participants
      snapshot = await db.collection('conversations')
        .where('memberIds', 'array-contains', req.user.uid)
        .orderBy('timestamp', 'desc')
        .limit(50)
        .get();
    } catch (indexError) {
      // Fallback: Query without orderBy if index doesn't exist
      console.warn('Firestore index missing, using unordered query:', indexError.message);
      snapshot = await db.collection('conversations')
        .where('memberIds', 'array-contains', req.user.uid)
        .limit(50)
        .get();
    }
    
    console.log('📊 Found', snapshot.size, 'conversations');
    
    const conversations = [];
    snapshot.forEach(doc => {
      const data = doc.data();
      console.log('  - Conversation', doc.id, '- memberIds:', data.memberIds);
      
      // Convert old structure to new API format
      const conversation = {
        id: doc.id,
        participants: data.memberIds || [],  // Map memberIds → participants
        participantNames: data.memberNames || {},
        lastMessage: data.lastMessage || '',
        lastMessageTime: data.timestamp || 0,
        isGroup: data.memberIds && data.memberIds.length > 2,
        type: data.type || 'FRIEND',
        ...data
      };
      
      conversations.push(conversation);
    });
    
    // Sort in memory if we didn't use orderBy
    conversations.sort((a, b) => (b.lastMessageTime || 0) - (a.lastMessageTime || 0));
    
    console.log('✅ Returning', conversations.length, 'conversations');
    res.json({ conversations });
  } catch (error) {
    console.error('❌ Error fetching conversations:', error);
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

// Update conversation (rename group, change avatar, etc.)
router.put('/:conversationId', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const updates = req.body; // Can include: name, groupAvatar, etc.
    
    console.log('📝 Updating conversation', conversationId, '- updates:', Object.keys(updates));
    
    // Remove fields that shouldn't be updated directly
    delete updates.id;
    delete updates.memberIds;
    delete updates.participants;
    delete updates.createdAt;
    
    await db.collection('conversations').doc(conversationId).update(updates);
    
    console.log('✅ Updated conversation', conversationId);
    res.json({ success: true });
  } catch (error) {
    console.error('❌ Error updating conversation:', error);
    res.status(500).json({ error: error.message });
  }
});

// Add member to group
router.post('/:conversationId/members', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { userId, userName } = req.body;
    
    console.log('➕ Adding member', userId, 'to conversation', conversationId);
    
    const convRef = db.collection('conversations').doc(conversationId);
    const doc = await convRef.get();
    
    if (!doc.exists) {
      return res.status(404).json({ error: 'Conversation not found' });
    }
    
    const data = doc.data();
    
    // Add to memberIds array
    const memberIds = data.memberIds || [];
    if (!memberIds.includes(userId)) {
      memberIds.push(userId);
    }
    
    // Add to memberNames map
    const memberNames = data.memberNames || {};
    memberNames[userId] = userName || 'Unknown';
    
    await convRef.update({ memberIds, memberNames });
    
    console.log('✅ Added member', userId);
    res.json({ success: true });
  } catch (error) {
    console.error('❌ Error adding member:', error);
    res.status(500).json({ error: error.message });
  }
});

// Remove member from group
router.delete('/:conversationId/members/:userId', authenticateUser, async (req, res) => {
  try {
    const { conversationId, userId } = req.params;
    
    console.log('➖ Removing member', userId, 'from conversation', conversationId);
    
    const convRef = db.collection('conversations').doc(conversationId);
    const doc = await convRef.get();
    
    if (!doc.exists) {
      return res.status(404).json({ error: 'Conversation not found' });
    }
    
    const data = doc.data();
    
    // Remove from memberIds array
    const memberIds = (data.memberIds || []).filter(id => id !== userId);
    
    // Remove from memberNames map
    const memberNames = data.memberNames || {};
    delete memberNames[userId];
    
    await convRef.update({ memberIds, memberNames });
    
    console.log('✅ Removed member', userId);
    res.json({ success: true });
  } catch (error) {
    console.error('❌ Error removing member:', error);
    res.status(500).json({ error: error.message });
  }
});

// Leave group
router.post('/:conversationId/leave', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const userId = req.user.uid;
    
    console.log('🚪 User', userId, 'leaving conversation', conversationId);
    
    const convRef = db.collection('conversations').doc(conversationId);
    const doc = await convRef.get();
    
    if (!doc.exists) {
      return res.status(404).json({ error: 'Conversation not found' });
    }
    
    const data = doc.data();
    
    // Remove self from memberIds
    const memberIds = (data.memberIds || []).filter(id => id !== userId);
    
    // Remove self from memberNames
    const memberNames = data.memberNames || {};
    delete memberNames[userId];
    
    await convRef.update({ memberIds, memberNames });
    
    console.log('✅ User left conversation');
    res.json({ success: true });
  } catch (error) {
    console.error('❌ Error leaving conversation:', error);
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
