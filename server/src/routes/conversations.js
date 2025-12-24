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
    const { participants, memberIds, isGroup, groupName, name, adminId, type } = req.body;
    const userId = req.user.uid;
    
    // Support both old format (participants) and new format (memberIds)
    const members = memberIds || participants || [];
    const conversationType = type || (isGroup ? 'GROUP' : 'FRIEND');
    const conversationName = name || groupName || '';
    
    console.log('📝 Creating conversation:', { type: conversationType, name: conversationName, members });
    
    const conversation = {
      memberIds: members,
      type: conversationType,
      name: conversationName,
      adminIds: adminId ? [adminId] : [userId],
      createdAt: Date.now(),
      timestamp: Date.now(),
      lastMessage: '',
      lastMessageTime: Date.now(),
      // Legacy fields for compatibility
      participants: members,
      isGroup: conversationType === 'GROUP'
    };
    
    const convRef = await db.collection('conversations').add(conversation);
    
    console.log('✅ Created conversation:', convRef.id);
    
    // Emit WebSocket event to all members
    const io = req.app.get('io');
    if (io && members && members.length > 0) {
      console.log('📢 Emitting conversation_created to members:', members);
      members.forEach(memberId => {
        const room = `user:${memberId}`;
        console.log(`  → Emitting to room: ${room}`);
        io.to(room).emit('conversation_created', {
          conversationId: convRef.id,
          conversation: { id: convRef.id, ...conversation }
        });
      });
      console.log('✅ Notified all members of new conversation');
    }
    
    res.json({ 
      success: true, 
      conversationId: convRef.id,
      conversation: { id: convRef.id, ...conversation }
    });
  } catch (error) {
    console.error('❌ Error creating conversation:', error);
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
    
    const convRef = db.collection('conversations').doc(conversationId);
    const doc = await convRef.get();
    const data = doc.data();
    
    await convRef.update(updates);
    
    // Emit WebSocket event to all members
    const io = req.app.get('io');
    if (io && data && data.memberIds) {
      data.memberIds.forEach(memberId => {
        io.to(`user:${memberId}`).emit('conversation_updated', {
          conversationId,
          updates
        });
      });
      console.log('✅ Notified members of conversation update');
    }
    
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
    
    // Get user name before removing
    const userName = data.memberNames?.[userId] || 'Unknown';
    
    // Remove self from memberIds
    const memberIds = (data.memberIds || []).filter(id => id !== userId);
    
    // Remove self from memberNames
    const memberNames = data.memberNames || {};
    delete memberNames[userId];
    
    await convRef.update({ memberIds, memberNames });
    
    // Emit WebSocket event to notify all remaining members
    const io = req.app.get('io');
    if (io) {
      // Notify remaining members that someone left
      memberIds.forEach(memberId => {
        io.to(`user:${memberId}`).emit('member_left', {
          conversationId,
          userId,
          userName,
          remainingMembers: memberIds
        });
      });
      
      // Notify the user who left to remove conversation from their UI
      io.to(`user:${userId}`).emit('group_left', {
        conversationId
      });
    }
    
    console.log('✅ User left conversation, notified remaining members');
    res.json({ success: true });
  } catch (error) {
    console.error('❌ Error leaving conversation:', error);
    res.status(500).json({ error: error.message });
  }
});

// Delete/Dissolve group conversation (admin only)
router.delete('/:conversationId', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const userId = req.user.uid;
    
    console.log('🗑️ User', userId, 'deleting conversation', conversationId);
    
    const convRef = db.collection('conversations').doc(conversationId);
    const doc = await convRef.get();
    
    if (!doc.exists) {
      return res.status(404).json({ error: 'Conversation not found' });
    }
    
    const data = doc.data();
    
    // Check if user is admin (creator) of the group
    // For now, allow any member to delete (can add admin check later)
    const memberIds = data.memberIds || [];
    if (!memberIds.includes(userId)) {
      return res.status(403).json({ error: 'You are not a member of this conversation' });
    }
    
    // Notify all members before deletion
    const io = req.app.get('io');
    console.log('🔌 WebSocket io available:', !!io);
    if (io) {
      console.log('📢 Emitting conversation_deleted to members:', memberIds);
      memberIds.forEach(memberId => {
        const room = `user:${memberId}`;
        console.log(`  → Emitting to room: ${room}`);
        io.to(room).emit('conversation_deleted', {
          conversationId,
          deletedBy: userId
        });
      });
      console.log('✅ Notified all members of conversation deletion');
    } else {
      console.log('❌ WebSocket io not available!');
    }
    
    // Delete all messages in the conversation first
    const messagesSnapshot = await convRef.collection('messages').get();
    const batch = db.batch();
    messagesSnapshot.docs.forEach(doc => {
      batch.delete(doc.ref);
    });
    
    // Delete the conversation document
    batch.delete(convRef);
    
    await batch.commit();
    
    console.log('✅ Deleted conversation', conversationId);
    res.json({ success: true });
  } catch (error) {
    console.error('❌ Error deleting conversation:', error);
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
