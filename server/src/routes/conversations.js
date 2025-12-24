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

module.exports = router;
