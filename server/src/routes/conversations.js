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

/**
 * POST /api/conversations - Create new conversation (group or private)
 * Phase 3D: Group Chat Management
 */
router.post('/', authenticateUser, async (req, res) => {
  try {
    const { type, name, memberIds, adminId } = req.body;
    
    // Validation
    if (!type || !memberIds || !Array.isArray(memberIds)) {
      return res.status(400).json({ error: 'Invalid request data' });
    }
    
    if (type === 'GROUP') {
      if (!name || name.trim() === '') {
        return res.status(400).json({ error: 'Group name is required' });
      }
      if (!adminId) {
        return res.status(400).json({ error: 'Admin ID is required for groups' });
      }
      if (memberIds.length < 2) {
        return res.status(400).json({ error: 'Group must have at least 2 members' });
      }
      if (!memberIds.includes(adminId)) {
        return res.status(400).json({ error: 'Admin must be a member' });
      }
    } else if (type === 'PRIVATE') {
      if (memberIds.length !== 2) {
        return res.status(400).json({ error: 'Private conversation must have exactly 2 members' });
      }
    }
    
    console.log(`📤 Creating ${type} conversation with ${memberIds.length} members`);
    
    // Create conversation document
    const conversationData = {
      type,
      memberIds,
      createdAt: Date.now(),
      timestamp: Date.now(),
      lastMessageAt: Date.now(),
      lastMessage: null,
      isActive: true
    };
    
    if (type === 'GROUP') {
      conversationData.name = name.trim();
      conversationData.adminIds = [adminId];
      conversationData.avatar = null;
    }
    
    const conversationRef = await db.collection('conversations').add(conversationData);
    const conversationId = conversationRef.id;
    
    console.log(`✅ Conversation created: ${conversationId}`);
    
    // Emit WebSocket event to all members
    const io = req.app.get('io');
    if (io) {
      const eventData = {
        conversationId,
        type,
        name: type === 'GROUP' ? name : null,
        memberIds,
        adminIds: type === 'GROUP' ? [adminId] : null,
        createdBy: req.user.uid,
        timestamp: Date.now()
      };
      
      console.log(`📡 Emitting conversation_created to members`);
      
      // Emit to each member's room
      memberIds.forEach(memberId => {
        io.to(`user:${memberId}`).emit('conversation_created', eventData);
      });
    }
    
    res.json({
      success: true,
      conversationId,
      conversation: {
        id: conversationId,
        ...conversationData
      }
    });
    
  } catch (error) {
    console.error('❌ Error creating conversation:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * PUT /api/conversations/:conversationId - Update conversation info
 * Phase 3D: Update group name/avatar
 */
router.put('/:conversationId', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { name, avatar } = req.body;
    
    console.log(`📝 Updating conversation ${conversationId}`);
    
    const conversationRef = db.collection('conversations').doc(conversationId);
    const conversationDoc = await conversationRef.get();
    
    if (!conversationDoc.exists) {
      return res.status(404).json({ error: 'Conversation not found' });
    }
    
    const conversationData = conversationDoc.data();
    
    // Only GROUP conversations can be updated
    if (conversationData.type !== 'GROUP') {
      return res.status(400).json({ error: 'Only group conversations can be updated' });
    }
    
    // Check if user is admin
    if (!conversationData.adminIds || !conversationData.adminIds.includes(req.user.uid)) {
      return res.status(403).json({ error: 'Only admins can update group info' });
    }
    
    const updates = {};
    if (name !== undefined) updates.name = name.trim();
    if (avatar !== undefined) updates.avatar = avatar;
    
    await conversationRef.update(updates);
    
    console.log(`✅ Conversation updated`);
    
    // Emit WebSocket event
    const io = req.app.get('io');
    if (io) {
      console.log(`📡 Emitting conversation_updated to conversation:${conversationId}`);
      io.to(`conversation:${conversationId}`).emit('conversation_updated', {
        conversationId,
        updates,
        updatedBy: req.user.uid,
        timestamp: Date.now()
      });
    }
    
    res.json({ success: true });
    
  } catch (error) {
    console.error('❌ Error updating conversation:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * POST /api/conversations/:conversationId/members - Add member to group
 * Phase 3D: Member management
 */
router.post('/:conversationId/members', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { userId } = req.body;
    
    if (!userId) {
      return res.status(400).json({ error: 'User ID is required' });
    }
    
    console.log(`➕ Adding member ${userId} to conversation ${conversationId}`);
    
    const conversationRef = db.collection('conversations').doc(conversationId);
    const conversationDoc = await conversationRef.get();
    
    if (!conversationDoc.exists) {
      return res.status(404).json({ error: 'Conversation not found' });
    }
    
    const conversationData = conversationDoc.data();
    
    // Only GROUP conversations can add members
    if (conversationData.type !== 'GROUP') {
      return res.status(400).json({ error: 'Can only add members to groups' });
    }
    
    // Check if user is admin
    if (!conversationData.adminIds || !conversationData.adminIds.includes(req.user.uid)) {
      return res.status(403).json({ error: 'Only admins can add members' });
    }
    
    // Check if already a member
    if (conversationData.memberIds && conversationData.memberIds.includes(userId)) {
      return res.status(400).json({ error: 'User is already a member' });
    }
    
    // Add member
    await conversationRef.update({
      memberIds: [...(conversationData.memberIds || []), userId]
    });
    
    console.log(`✅ Member added successfully`);
    
    // Emit WebSocket event
    const io = req.app.get('io');
    if (io) {
      const eventData = {
        conversationId,
        userId,
        addedBy: req.user.uid,
        timestamp: Date.now()
      };
      
      console.log(`📡 Emitting member_added to conversation:${conversationId}`);
      io.to(`conversation:${conversationId}`).emit('member_added', eventData);
      
      // Also emit to new member
      io.to(`user:${userId}`).emit('conversation_created', {
        conversationId,
        ...conversationData,
        memberIds: [...(conversationData.memberIds || []), userId]
      });
    }
    
    res.json({ success: true });
    
  } catch (error) {
    console.error('❌ Error adding member:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * DELETE /api/conversations/:conversationId/members/:userId - Remove member from group
 * Phase 3D: Member management
 */
router.delete('/:conversationId/members/:userId', authenticateUser, async (req, res) => {
  try {
    const { conversationId, userId } = req.params;
    
    console.log(`➖ Removing member ${userId} from conversation ${conversationId}`);
    
    const conversationRef = db.collection('conversations').doc(conversationId);
    const conversationDoc = await conversationRef.get();
    
    if (!conversationDoc.exists) {
      return res.status(404).json({ error: 'Conversation not found' });
    }
    
    const conversationData = conversationDoc.data();
    
    // Only GROUP conversations can remove members
    if (conversationData.type !== 'GROUP') {
      return res.status(400).json({ error: 'Can only remove members from groups' });
    }
    
    // Check if user is admin
    if (!conversationData.adminIds || !conversationData.adminIds.includes(req.user.uid)) {
      return res.status(403).json({ error: 'Only admins can remove members' });
    }
    
    // Cannot remove yourself (use leave endpoint)
    if (userId === req.user.uid) {
      return res.status(400).json({ error: 'Use leave endpoint to leave group' });
    }
    
    // Remove member
    const newMemberIds = (conversationData.memberIds || []).filter(id => id !== userId);
    const newAdminIds = (conversationData.adminIds || []).filter(id => id !== userId);
    
    await conversationRef.update({
      memberIds: newMemberIds,
      adminIds: newAdminIds
    });
    
    console.log(`✅ Member removed successfully`);
    
    // Emit WebSocket event
    const io = req.app.get('io');
    if (io) {
      console.log(`📡 Emitting member_removed to conversation:${conversationId}`);
      io.to(`conversation:${conversationId}`).emit('member_removed', {
        conversationId,
        userId,
        removedBy: req.user.uid,
        timestamp: Date.now()
      });
    }
    
    res.json({ success: true });
    
  } catch (error) {
    console.error('❌ Error removing member:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * POST /api/conversations/:conversationId/leave - Leave group
 * Phase 3D: Member management
 */
router.post('/:conversationId/leave', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    
    console.log(`🚪 User ${req.user.uid} leaving conversation ${conversationId}`);
    
    const conversationRef = db.collection('conversations').doc(conversationId);
    const conversationDoc = await conversationRef.get();
    
    if (!conversationDoc.exists) {
      return res.status(404).json({ error: 'Conversation not found' });
    }
    
    const conversationData = conversationDoc.data();
    
    // Only GROUP conversations can be left
    if (conversationData.type !== 'GROUP') {
      return res.status(400).json({ error: 'Cannot leave private conversations' });
    }
    
    // Check if user is a member
    if (!conversationData.memberIds || !conversationData.memberIds.includes(req.user.uid)) {
      return res.status(400).json({ error: 'You are not a member of this group' });
    }
    
    // Check if last admin
    const isAdmin = conversationData.adminIds && conversationData.adminIds.includes(req.user.uid);
    if (isAdmin && conversationData.adminIds.length === 1) {
      return res.status(400).json({ error: 'Cannot leave as last admin. Promote someone else first.' });
    }
    
    // Remove user from members and admins
    const newMemberIds = conversationData.memberIds.filter(id => id !== req.user.uid);
    const newAdminIds = (conversationData.adminIds || []).filter(id => id !== req.user.uid);
    
    await conversationRef.update({
      memberIds: newMemberIds,
      adminIds: newAdminIds
    });
    
    console.log(`✅ User left successfully`);
    
    // Emit WebSocket event
    const io = req.app.get('io');
    if (io) {
      console.log(`📡 Emitting member_left to conversation:${conversationId}`);
      io.to(`conversation:${conversationId}`).emit('member_left', {
        conversationId,
        userId: req.user.uid,
        timestamp: Date.now()
      });
    }
    
    res.json({ success: true });
    
  } catch (error) {
    console.error('❌ Error leaving conversation:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * PUT /api/conversations/:conversationId/admins - Promote/demote admin
 * Phase 3D: Admin management
 */
router.put('/:conversationId/admins', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { userId, action } = req.body; // action: 'add' | 'remove'
    
    if (!userId || !action || !['add', 'remove'].includes(action)) {
      return res.status(400).json({ error: 'Invalid request' });
    }
    
    console.log(`👑 ${action === 'add' ? 'Promoting' : 'Demoting'} admin ${userId} in conversation ${conversationId}`);
    
    const conversationRef = db.collection('conversations').doc(conversationId);
    const conversationDoc = await conversationRef.get();
    
    if (!conversationDoc.exists) {
      return res.status(404).json({ error: 'Conversation not found' });
    }
    
    const conversationData = conversationDoc.data();
    
    // Only GROUP conversations have admins
    if (conversationData.type !== 'GROUP') {
      return res.status(400).json({ error: 'Only groups have admins' });
    }
    
    // Check if requester is admin
    if (!conversationData.adminIds || !conversationData.adminIds.includes(req.user.uid)) {
      return res.status(403).json({ error: 'Only admins can manage admins' });
    }
    
    // Check if user is a member
    if (!conversationData.memberIds || !conversationData.memberIds.includes(userId)) {
      return res.status(400).json({ error: 'User is not a member' });
    }
    
    let newAdminIds = conversationData.adminIds || [];
    
    if (action === 'add') {
      if (newAdminIds.includes(userId)) {
        return res.status(400).json({ error: 'User is already an admin' });
      }
      newAdminIds.push(userId);
    } else {
      // Cannot demote last admin
      if (newAdminIds.length === 1 && newAdminIds.includes(userId)) {
        return res.status(400).json({ error: 'Cannot remove last admin' });
      }
      newAdminIds = newAdminIds.filter(id => id !== userId);
    }
    
    await conversationRef.update({ adminIds: newAdminIds });
    
    console.log(`✅ Admin ${action === 'add' ? 'promoted' : 'demoted'} successfully`);
    
    // Emit WebSocket event
    const io = req.app.get('io');
    if (io) {
      console.log(`📡 Emitting admin_updated to conversation:${conversationId}`);
      io.to(`conversation:${conversationId}`).emit('admin_updated', {
        conversationId,
        userId,
        action,
        updatedBy: req.user.uid,
        timestamp: Date.now()
      });
    }
    
    res.json({ success: true });
    
  } catch (error) {
    console.error('❌ Error updating admin:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * PUT /api/conversations/:conversationId/archive - Archive/unarchive conversation
 * Phase 3E: Conversation operations
 */
router.put('/:conversationId/archive', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { archived } = req.body; // boolean
    
    console.log(`📁 ${archived ? 'Archiving' : 'Unarchiving'} conversation ${conversationId} for user ${req.user.uid}`);
    
    // Store archive status per user in subcollection
    const userSettingsRef = db.collection('conversations')
      .doc(conversationId)
      .collection('userSettings')
      .doc(req.user.uid);
    
    await userSettingsRef.set({
      archived: archived === true,
      updatedAt: Date.now()
    }, { merge: true });
    
    console.log(`✅ Conversation ${archived ? 'archived' : 'unarchived'}`);
    
    res.json({ success: true });
    
  } catch (error) {
    console.error('❌ Error archiving conversation:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * PUT /api/conversations/:conversationId/mute - Mute/unmute conversation
 * Phase 3E: Conversation operations
 */
router.put('/:conversationId/mute', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { muted, muteUntil } = req.body; // muted: boolean, muteUntil: timestamp (optional)
    
    console.log(`🔕 ${muted ? 'Muting' : 'Unmuting'} conversation ${conversationId} for user ${req.user.uid}`);
    
    // Store mute status per user in subcollection
    const userSettingsRef = db.collection('conversations')
      .doc(conversationId)
      .collection('userSettings')
      .doc(req.user.uid);
    
    const settings = {
      muted: muted === true,
      updatedAt: Date.now()
    };
    
    if (muted && muteUntil) {
      settings.muteUntil = muteUntil;
    }
    
    await userSettingsRef.set(settings, { merge: true });
    
    console.log(`✅ Conversation ${muted ? 'muted' : 'unmuted'}`);
    
    res.json({ success: true });
    
  } catch (error) {
    console.error('❌ Error muting conversation:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * PUT /api/conversations/:conversationId/pin - Pin/unpin conversation
 * Phase 3E: Conversation operations
 */
router.put('/:conversationId/pin', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { pinned } = req.body; // boolean
    
    console.log(`📌 ${pinned ? 'Pinning' : 'Unpinning'} conversation ${conversationId} for user ${req.user.uid}`);
    
    // Store pin status per user in subcollection
    const userSettingsRef = db.collection('conversations')
      .doc(conversationId)
      .collection('userSettings')
      .doc(req.user.uid);
    
    await userSettingsRef.set({
      pinned: pinned === true,
      pinnedAt: pinned ? Date.now() : null,
      updatedAt: Date.now()
    }, { merge: true });
    
    console.log(`✅ Conversation ${pinned ? 'pinned' : 'unpinned'}`);
    
    res.json({ success: true });
    
  } catch (error) {
    console.error('❌ Error pinning conversation:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * DELETE /api/conversations/:conversationId - Delete conversation (soft delete for user)
 * Phase 3E: Conversation operations
 */
router.delete('/:conversationId/user', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    
    console.log(`🗑️ Deleting conversation ${conversationId} for user ${req.user.uid}`);
    
    // Soft delete: mark as deleted for this user only
    const userSettingsRef = db.collection('conversations')
      .doc(conversationId)
      .collection('userSettings')
      .doc(req.user.uid);
    
    await userSettingsRef.set({
      deleted: true,
      deletedAt: Date.now(),
      updatedAt: Date.now()
    }, { merge: true });
    
    console.log(`✅ Conversation deleted for user`);
    
    res.json({ success: true });
    
  } catch (error) {
    console.error('❌ Error deleting conversation:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * GET /api/conversations/:conversationId/settings - Get user's conversation settings
 * Phase 3E: Helper endpoint to fetch user settings
 */
router.get('/:conversationId/settings', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    
    const userSettingsDoc = await db.collection('conversations')
      .doc(conversationId)
      .collection('userSettings')
      .doc(req.user.uid)
      .get();
    
    const settings = userSettingsDoc.exists ? userSettingsDoc.data() : {
      archived: false,
      muted: false,
      pinned: false,
      deleted: false
    };
    
    res.json({ success: true, settings });
    
  } catch (error) {
    console.error('❌ Error fetching settings:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * GET /api/conversations/search - Search conversations by name
 * Phase 3F: Search & Filter
 */
router.get('/search', authenticateUser, async (req, res) => {
  try {
    const { query, limit = 20 } = req.query;
    
    if (!query || query.trim().length < 2) {
      return res.status(400).json({ error: 'Search query must be at least 2 characters' });
    }
    
    console.log(`🔍 Searching conversations for: "${query}"`);
    
    // Get user's conversations first
    const userConversations = await db.collection('conversations')
      .where('memberIds', 'array-contains', req.user.uid)
      .get();
    
    // Filter by name (case-insensitive)
    const searchLower = query.toLowerCase();
    const results = [];
    
    userConversations.forEach(doc => {
      const data = doc.data();
      
      // Search in group name or member names (for private chats)
      if (data.type === 'GROUP' && data.name) {
        if (data.name.toLowerCase().includes(searchLower)) {
          results.push({
            id: doc.id,
            ...data
          });
        }
      }
      // For private chats, we could search by other member's name
      // (would need to fetch user data - skipping for now)
    });
    
    // Sort by lastMessageAt and limit
    results.sort((a, b) => (b.lastMessageAt || 0) - (a.lastMessageAt || 0));
    const limited = results.slice(0, parseInt(limit));
    
    console.log(`✅ Found ${limited.length} conversations`);
    
    res.json({
      success: true,
      results: limited,
      total: results.length
    });
    
  } catch (error) {
    console.error('❌ Error searching conversations:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * GET /api/conversations/:conversationId/messages/search - Search messages in conversation
 * Phase 3F: Search & Filter
 */
router.get('/:conversationId/messages/search', authenticateUser, async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { query, limit = 50, offset = 0 } = req.query;
    
    if (!query || query.trim().length < 2) {
      return res.status(400).json({ error: 'Search query must be at least 2 characters' });
    }
    
    console.log(`🔍 Searching messages in ${conversationId} for: "${query}"`);
    
    // Verify user is member
    const conversationDoc = await db.collection('conversations').doc(conversationId).get();
    if (!conversationDoc.exists) {
      return res.status(404).json({ error: 'Conversation not found' });
    }
    
    const conversationData = conversationDoc.data();
    if (!conversationData.memberIds || !conversationData.memberIds.includes(req.user.uid)) {
      return res.status(403).json({ error: 'Not a member of this conversation' });
    }
    
    // Get all messages (Firestore doesn't support full-text search natively)
    const messagesSnapshot = await db.collection('conversations')
      .doc(conversationId)
      .collection('messages')
      .orderBy('timestamp', 'desc')
      .limit(1000) // Limit to prevent performance issues
      .get();
    
    // Filter messages by content (case-insensitive)
    const searchLower = query.toLowerCase();
    const results = [];
    
    messagesSnapshot.forEach(doc => {
      const data = doc.data();
      
      // Search in message content
      if (data.content && data.content.toLowerCase().includes(searchLower)) {
        results.push({
          id: doc.id,
          ...data
        });
      }
    });
    
    // Apply offset and limit
    const paginatedResults = results.slice(parseInt(offset), parseInt(offset) + parseInt(limit));
    
    console.log(`✅ Found ${results.length} messages, returning ${paginatedResults.length}`);
    
    res.json({
      success: true,
      results: paginatedResults,
      total: results.length,
      hasMore: results.length > parseInt(offset) + parseInt(limit)
    });
    
  } catch (error) {
    console.error('❌ Error searching messages:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * GET /api/conversations/filter - Filter conversations by type/status
 * Phase 3F: Search & Filter
 */
router.get('/filter', authenticateUser, async (req, res) => {
  try {
    const { type, archived, muted, pinned, limit = 50 } = req.query;
    
    console.log(`🔍 Filtering conversations - type: ${type}, archived: ${archived}, muted: ${muted}, pinned: ${pinned}`);
    
    // Get user's conversations
    let query = db.collection('conversations')
      .where('memberIds', 'array-contains', req.user.uid);
    
    // Filter by type if specified
    if (type && ['GROUP', 'PRIVATE'].includes(type)) {
      query = query.where('type', '==', type);
    }
    
    const conversationsSnapshot = await query.limit(parseInt(limit) * 2).get(); // Get more to filter
    
    // Get user settings for all conversations (for archive/mute/pin filtering)
    const conversations = [];
    const settingsPromises = [];
    
    conversationsSnapshot.forEach(doc => {
      conversations.push({ id: doc.id, ...doc.data() });
      
      // Only fetch settings if we're filtering by them
      if (archived !== undefined || muted !== undefined || pinned !== undefined) {
        settingsPromises.push(
          db.collection('conversations')
            .doc(doc.id)
            .collection('userSettings')
            .doc(req.user.uid)
            .get()
        );
      }
    });
    
    // Apply user-specific filters
    let filtered = conversations;
    
    if (settingsPromises.length > 0) {
      const settingsDocs = await Promise.all(settingsPromises);
      
      filtered = conversations.filter((conv, index) => {
        const settings = settingsDocs[index].exists ? settingsDocs[index].data() : {};
        
        // Filter by archived status
        if (archived !== undefined) {
          const isArchived = settings.archived === true;
          if (archived === 'true' && !isArchived) return false;
          if (archived === 'false' && isArchived) return false;
        }
        
        // Filter by muted status
        if (muted !== undefined) {
          const isMuted = settings.muted === true;
          if (muted === 'true' && !isMuted) return false;
          if (muted === 'false' && isMuted) return false;
        }
        
        // Filter by pinned status
        if (pinned !== undefined) {
          const isPinned = settings.pinned === true;
          if (pinned === 'true' && !isPinned) return false;
          if (pinned === 'false' && isPinned) return false;
        }
        
        return true;
      });
    }
    
    // Sort and limit
    filtered.sort((a, b) => (b.lastMessageAt || 0) - (a.lastMessageAt || 0));
    const limited = filtered.slice(0, parseInt(limit));
    
    console.log(`✅ Found ${limited.length} conversations after filtering`);
    
    res.json({
      success: true,
      conversations: limited,
      total: filtered.length
    });
    
  } catch (error) {
    console.error('❌ Error filtering conversations:', error);
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
