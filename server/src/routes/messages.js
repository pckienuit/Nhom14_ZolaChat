const express = require('express');
const router = express.Router();
const { authenticateUser, db, admin } = require('../middleware/auth');

/**
 * POST /api/messages - Send a new message
 */
/**
 * POST /api/messages - Send a new message
 */
router.post('/', authenticateUser, async (req, res) => {
  try {
    const { 
      conversationId, type, content, 
      fileName, fileSize, fileMimeType,
      senderName,
      replyToId, replyToContent, replyToSenderId, replyToSenderName,
      isForwarded, originalSenderId, originalSenderName,
      contactUserId,
      latitude, longitude, locationName, locationAddress,
      liveLocationSessionId,
      stickerId, stickerPackId, stickerUrl, isStickerAnimated,
      voiceUrl, voiceDuration,
      pollData
    } = req.body;
    
    // Validate required fields
    if (!conversationId || !type) {
      return res.status(400).json({ error: 'Missing required fields: conversationId, type' });
    }
    
    // Create message object
    const message = {
      conversationId,
      senderId: req.user.uid,
      type,
      content: content || '',
      timestamp: Date.now(),
      isRecalled: false,
      reactions: {},
      reactionCounts: {}
    };

    // If senderName is not provided, fetch it
    if (senderName) {
      message.senderName = senderName;
    } else {
      try {
        const userDoc = await db.collection('users').doc(req.user.uid).get();
        if (userDoc.exists) {
          const userData = userDoc.data();
          message.senderName = userData.name || userData.displayName || 'Unknown';
        } else {
          message.senderName = 'Unknown';
        }
      } catch (e) {
        console.error('Error fetching sender name:', e);
        message.senderName = 'Unknown';
      }
    }
    
    // Add optional fields if present
    const addIfPresent = (key, value) => {
      if (value !== undefined && value !== null) {
        message[key] = value;
      }
    };

    addIfPresent('fileName', fileName);
    addIfPresent('fileSize', fileSize);
    addIfPresent('fileMimeType', fileMimeType);
    
    // Reply fields
    addIfPresent('replyToId', replyToId);
    addIfPresent('replyToContent', replyToContent);
    addIfPresent('replyToSenderId', replyToSenderId);
    addIfPresent('replyToSenderName', replyToSenderName);
    
    // Forward fields
    if (isForwarded) {
      message.isForwarded = true;
      addIfPresent('originalSenderId', originalSenderId);
      addIfPresent('originalSenderName', originalSenderName);
    }
    
    // Contact
    addIfPresent('contactUserId', contactUserId);
    
    // Location
    addIfPresent('latitude', latitude);
    addIfPresent('longitude', longitude);
    addIfPresent('locationName', locationName);
    addIfPresent('locationAddress', locationAddress);
    
    // Live Location
    addIfPresent('liveLocationSessionId', liveLocationSessionId);
    
    // Sticker
    addIfPresent('stickerId', stickerId);
    addIfPresent('stickerPackId', stickerPackId);
    addIfPresent('stickerUrl', stickerUrl);
    addIfPresent('isStickerAnimated', isStickerAnimated);
    
    // Voice
    addIfPresent('voiceUrl', voiceUrl);
    addIfPresent('voiceDuration', voiceDuration);

    // Poll
    addIfPresent('pollData', pollData);
    
    console.log(`📤 Sending ${type} message in conversation ${conversationId} from ${req.user.uid}`);
    if (type === 'POLL') console.log('📊 Poll question:', pollData?.question);
    
    // Save to Firestore
    const messageRef = await db.collection('conversations')
      .doc(conversationId)
      .collection('messages')
      .add(message);
    
    console.log(`✅ Message created with ID: ${messageRef.id}`);
    
    // Update conversation lastMessage and timestamp
    await db.collection('conversations').doc(conversationId).update({
      lastMessage: content || `[${type}]`,
      lastMessageTime: message.timestamp,
      timestamp: message.timestamp
    });
    
    // Increment unreadCounts for all members except sender
    try {
      const convDoc = await db.collection('conversations').doc(conversationId).get();
      if (convDoc.exists) {
        const convData = convDoc.data();
        const members = convData.memberIds || convData.participantIds || [];
        const unreadUpdates = {};
        
        members.forEach(memberId => {
          if (memberId !== req.user.uid) {
            unreadUpdates['unreadCounts.' + memberId] = admin.firestore.FieldValue.increment(1);
          }
        });
        
        if (Object.keys(unreadUpdates).length > 0) {
          await db.collection('conversations').doc(conversationId).update(unreadUpdates);
        }
      }
    } catch (unreadErr) {
      console.error('❌ Failed to update unreadCounts:', unreadErr);
    }
    
    // Emit WebSocket event
    const io = req.app.get('io');
    if (io) {
      // Ensure senderName is populated (client should send it, but we can double check or just use what client sent)
      // Since we already saved senderName (if provided), we use it.
      
      const messageWithId = { ...message, id: messageRef.id };
      console.log(`📡 Emitting new_message to conversation:${conversationId}`);
      io.to(`conversation:${conversationId}`).emit('new_message', messageWithId);

      // Notification for Home Screen Preview
      try {
        const conversationDoc = await db.collection('conversations').doc(conversationId).get();
        if (conversationDoc.exists) {
            const data = conversationDoc.data();
            const members = data.memberIds || data.participantIds || data.participants || [];
            members.forEach(memberId => {
                io.to(`user:${memberId}`).emit('new_message', messageWithId);
            });
        }
      } catch (err) {
        console.error('Failed to send notifications to members', err);
      }
    }
    
    res.json({ 
      success: true, 
      messageId: messageRef.id,
      message: { ...message, id: messageRef.id }
    });
  } catch (error) {
    console.error('❌ Error sending message:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * DELETE /api/messages/:messageId - Delete a message
 */
router.delete('/:messageId', authenticateUser, async (req, res) => {
  try {
    const { messageId } = req.params;
    const { conversationId } = req.query;
    
    if (!conversationId) {
      return res.status(400).json({ error: 'conversationId query parameter required' });
    }
    
    console.log(`🗑️ Deleting message ${messageId} in conversation ${conversationId}`);
    
    // Get message to verify ownership
    const messageDoc = await db.collection('conversations')
      .doc(conversationId)
      .collection('messages')
      .doc(messageId)
      .get();
    
    if (!messageDoc.exists) {
      return res.status(404).json({ error: 'Message not found' });
    }
    
    const messageData = messageDoc.data();
    
    // Verify ownership
    if (messageData.senderId !== req.user.uid) {
      console.warn(`⚠️ User ${req.user.uid} attempted to delete message owned by ${messageData.senderId}`);
      return res.status(403).json({ error: 'Not authorized to delete this message' });
    }
    
    // Delete message
    await messageDoc.ref.delete();
    console.log(`✅ Message ${messageId} deleted`);
    
    // Emit WebSocket event
    const io = req.app.get('io');
    if (io) {
      console.log(`📡 Emitting message_deleted to conversation:${conversationId}`);
      io.to(`conversation:${conversationId}`).emit('message_deleted', {
        messageId,
        conversationId
      });
    }
    
    res.json({ success: true });
  } catch (error) {
    console.error('❌ Error deleting message:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * PUT /api/messages/:messageId - Update message (edit or recall)
 */
router.put('/:messageId', authenticateUser, async (req, res) => {
  try {
    const { messageId } = req.params;
    const { conversationId, action, content } = req.body;
    
    if (!conversationId || !action) {
      return res.status(400).json({ error: 'Missing required fields: conversationId, action' });
    }
    
    console.log(`✏️ Updating message ${messageId} with action: ${action}`);
    
    const messageRef = db.collection('conversations')
      .doc(conversationId)
      .collection('messages')
      .doc(messageId);
    
    const messageDoc = await messageRef.get();
    if (!messageDoc.exists) {
      return res.status(404).json({ error: 'Message not found' });
    }
    
    const messageData = messageDoc.data();
    
    // Verify ownership
    if (messageData.senderId !== req.user.uid) {
      console.warn(`⚠️ User ${req.user.uid} attempted to update message owned by ${messageData.senderId}`);
      return res.status(403).json({ error: 'Not authorized to update this message' });
    }
    
    let updates = {};
    
    if (action === 'recall') {
      updates = {
        isRecalled: true,
        recalledAt: Date.now()
      };
      console.log(`📞 Recalling message ${messageId}`);
    } else if (action === 'edit') {
      if (!content) {
        return res.status(400).json({ error: 'Content required for edit action' });
      }
      updates = {
        content,
        editedAt: Date.now()
      };
      console.log(`✏️ Editing message ${messageId}`);
    } else {
      return res.status(400).json({ error: 'Invalid action. Must be "recall" or "edit"' });
    }
    
    await messageRef.update(updates);
    console.log(`✅ Message ${messageId} updated`);
    
    // Emit WebSocket event
    const io = req.app.get('io');
    if (io) {
      console.log(`📡 Emitting message_updated to conversation:${conversationId}`);
      io.to(`conversation:${conversationId}`).emit('message_updated', {
        messageId,
        conversationId,
        ...updates
      });
    }
    
    res.json({ 
      success: true, 
      message: { ...messageData, ...updates, id: messageId }
    });
  } catch (error) {
    console.error('❌ Error updating message:', error);
    res.status(500).json({ error: error.message });
  }
});

/**
 * POST /api/messages/:messageId/reactions - Add, change, or remove a reaction
 */
router.post('/:messageId/reactions', authenticateUser, async (req, res) => {
  try {
    const { messageId } = req.params;
    const { conversationId, reactionType } = req.body;
    
    if (!conversationId) {
      return res.status(400).json({ error: 'conversationId required' });
    }
    
    console.log(`❤️ ${reactionType ? 'Adding' : 'Removing'} reaction on message ${messageId} by ${req.user.uid}`);
    
    const messageRef = db.collection('conversations')
      .doc(conversationId)
      .collection('messages')
      .doc(messageId);
    
    // Use transaction to safely update reactions
    const result = await db.runTransaction(async (transaction) => {
      const messageDoc = await transaction.get(messageRef);
      
      if (!messageDoc.exists) {
        throw new Error('Message not found');
      }
      
      const messageData = messageDoc.data();
      let reactionsDetailed = messageData.reactionsDetailed || {};
      let reactionCounts = messageData.reactionCounts || {};
      
      // MIGRATION: If reactionsDetailed is empty but reactions exists (old format),
      // migrate old data while preserving click counts from reactionCounts
      if (Object.keys(reactionsDetailed).length === 0 && messageData.reactions) {
        console.log(`📦 Migrating old reactions format to reactionsDetailed`);
        
        // Count how many users have each reaction type
        const usersByType = {};
        for (const userId in messageData.reactions) {
          const reactionType = messageData.reactions[userId];
          if (reactionType && typeof reactionType === 'string') {
            if (!usersByType[reactionType]) {
              usersByType[reactionType] = [];
            }
            usersByType[reactionType].push(userId);
          }
        }
        
        // Distribute counts evenly among users (best guess for old data)
        for (const type in usersByType) {
          const users = usersByType[type];
          const totalCount = reactionCounts[type] || users.length;
          const countPerUser = Math.max(1, Math.floor(totalCount / users.length));
          
          users.forEach(userId => {
            reactionsDetailed[userId] = { [type]: countPerUser };
          });
        }
        
        console.log(`📦 Migrated to: ${JSON.stringify(reactionsDetailed)}`);
      }
      
      // Get user's current reactions (object with counts per type)
      const userReactions = reactionsDetailed[req.user.uid] || {};
      
      if (reactionType) {
        // Add/increment reaction
        const currentCount = userReactions[reactionType] || 0;
        userReactions[reactionType] = currentCount + 1;
        
        // Update user's reactions
        reactionsDetailed[req.user.uid] = userReactions;
        
        // Update global count
        reactionCounts[reactionType] = (reactionCounts[reactionType] || 0) + 1;
      } else {
        // Remove ALL user's reactions
        for (const type in userReactions) {
          const count = userReactions[type] || 0;
          // Ensure we don't go negative
          reactionCounts[type] = Math.max(0, (reactionCounts[type] || 0) - count);
          if (reactionCounts[type] === 0) {
            delete reactionCounts[type];
          }
        }
        
        // Remove user from reactions
        delete reactionsDetailed[req.user.uid];
      }
      
      // IMPORTANT: Recalculate reactionCounts from reactionsDetailed to fix inconsistencies
      // This ensures counts are always accurate
      reactionCounts = {};
      for (const userId in reactionsDetailed) {
        const userReacts = reactionsDetailed[userId];
        for (const type in userReacts) {
          reactionCounts[type] = (reactionCounts[type] || 0) + (userReacts[type] || 0);
        }
      }
      
      // Create flattened reactions for backward compatibility
      // Format: { "userId": "heart" } - shows primary reaction type (highest count)
      const reactions = {};
      for (const userId in reactionsDetailed) {
        const userReacts = reactionsDetailed[userId];
        let maxType = null;
        let maxCount = 0;
        for (const type in userReacts) {
          if (userReacts[type] > maxCount) {
            maxCount = userReacts[type];
            maxType = type;
          }
        }
        if (maxType) {
          reactions[userId] = maxType;
        }
      }
      
      // Update message with both formats
      transaction.update(messageRef, { 
        reactions,           // Flattened for backward compatibility
        reactionsDetailed,   // Detailed for Option 2
        reactionCounts 
      });
      
      return { reactions, reactionsDetailed, reactionCounts };
    });
    
    console.log(`✅ Reaction updated - counts: ${JSON.stringify(result.reactionCounts)}`);
    console.log(`📊 Flattened reactions: ${JSON.stringify(result.reactions)}`);
    console.log(`� Detailed reactions: ${JSON.stringify(result.reactionsDetailed)}`);
    
    // Emit WebSocket event
    const io = req.app.get('io');
    if (io) {
      const eventData = {
        messageId,
        conversationId,
        userId: req.user.uid,
        reactionType,
        reactions: result.reactions,
        reactionCounts: result.reactionCounts
      };
      console.log(`📡 Emitting reaction_updated to conversation:${conversationId}`);
      console.log(`📡 Event data: ${JSON.stringify(eventData)}`);
      io.to(`conversation:${conversationId}`).emit('reaction_updated', eventData);
    }
    
    res.json({ 
      success: true,
      reactions: result.reactions,
      reactionCounts: result.reactionCounts
    });
  } catch (error) {
    console.error('❌ Error updating reaction:', error);
    res.status(500).json({ error: error.message });
  }
});


/**
 * POST /api/messages/:messageId/poll/vote - Vote on a poll
 */
router.post('/:messageId/poll/vote', authenticateUser, async (req, res) => {
  try {
    const { messageId } = req.params;
    const { conversationId, optionId } = req.body;
    
    if (!conversationId || !optionId) {
      return res.status(400).json({ error: 'Missing required fields: conversationId, optionId' });
    }
    
    const userId = req.user.uid;
    console.log(`🗳️ Voting on poll message ${messageId}: user=${userId}, option=${optionId}`);
    
    const messageRef = db.collection('conversations')
      .doc(conversationId)
      .collection('messages')
      .doc(messageId);
    
    // Get user name for display
    let userName = 'User';
    try {
      const userDoc = await db.collection('users').doc(userId).get();
      if (userDoc.exists) {
        const userData = userDoc.data();
        userName = userData.name || userData.displayName || 'User';
      }
    } catch (e) {
      console.warn('Failed to fetch user name for poll vote', e);
    }

    // Run transaction
    const result = await db.runTransaction(async (transaction) => {
      const messageDoc = await transaction.get(messageRef);
      if (!messageDoc.exists) {
        throw new Error('Message not found');
      }
      
      const data = messageDoc.data();
      if (data.type !== 'POLL') {
        throw new Error('Message is not a poll');
      }
      
      const pollData = data.pollData || {};
      const options = pollData.options || [];
      
      // Find the option
      const optionIndex = options.findIndex(opt => opt.id === optionId);
      if (optionIndex === -1) {
        throw new Error('Poll option not found');
      }
      
      // Check if user already voted for ANY option (if single choice)
      // For now, let's assume multiple choice is allowed or handled by client?
      // Re-reading requirements: usually poll allows removing vote or changing vote.
      // Let's implement toggle logic similar to reactions:
      // If user checks the same option -> remove vote
      // If user checks different option -> allow (multiple choice) OR switch (single choice)
      // The current client implementation seems to just send "vote".
      
      // Update the specific option
      const currentOption = options[optionIndex];
      const voters = currentOption.voters || [];
      const voterIds = currentOption.voterIds || [];
      
      // Check if user already voted for THIS option
      const existingVoteIndex = voters.findIndex(v => v.userId === userId);
      
      if (existingVoteIndex !== -1) {
        // Remove vote (toggle off)
        voters.splice(existingVoteIndex, 1);
        const idIndex = voterIds.indexOf(userId);
        if (idIndex !== -1) voterIds.splice(idIndex, 1);
        currentOption.voteCount = Math.max(0, (currentOption.voteCount || 0) - 1);
      } else {
        // Add vote (toggle on)
        voters.push({
          userId,
          userName,
          votedAt: Date.now()
        });
        voterIds.push(userId);
        currentOption.voteCount = (currentOption.voteCount || 0) + 1;
      }
      
      currentOption.voters = voters;
      currentOption.voterIds = voterIds;
      options[optionIndex] = currentOption;
      
      // Update message
      transaction.update(messageRef, {
        'pollData.options': options
      });
      
      return { pollData: { ...pollData, options } };
    });
    
    console.log(`✅ Poll vote recorded`);
    
    // Emit WebSocket event
    const io = req.app.get('io');
    if (io) {
      // Use message_updated event so clients refresh the message
      const eventData = {
        conversationId,
        messageId,
        pollData: result.pollData,
        // Helper fields for easier client updates if needed
        updatedOptionId: optionId, 
        userId,
        userName
      };
      
      console.log(`📡 Emitting message_updated (poll) to conversation:${conversationId}`);
      io.to(`conversation:${conversationId}`).emit('message_updated', {
        ...eventData,
        // Include minimal fields to identify this is a poll update
         type: 'POLL'
      });
    }
    
    res.json({ success: true, pollData: result.pollData });
    
  } catch (error) {
    console.error('❌ Error voting on poll:', error);
    res.status(500).json({ error: error.message });
  }
});

router.delete('/:messageId/reactions', authenticateUser, async (req, res) => {
  try {
    const { messageId } = req.params;
    const { conversationId } = req.query;
    
    if (!conversationId) {
      return res.status(400).json({ error: 'conversationId required' });
    }
    
    console.log(`🗑️ Clearing ALL reactions on message ${messageId}`);
    
    const messageRef = db.collection('conversations')
      .doc(conversationId)
      .collection('messages')
      .doc(messageId);
    
    // Clear all reactions and counts
    await messageRef.update({
      reactions: {},
      reactionCounts: {}
    });
    
    console.log(`✅ All reactions cleared on message ${messageId}`);
    
    // Emit WebSocket event
    const io = req.app.get('io');
    if (io) {
      console.log(`📡 Emitting reaction_updated (cleared) to conversation:${conversationId}`);
      io.to(`conversation:${conversationId}`).emit('reaction_updated', {
        messageId,
        conversationId,
        userId: req.user.uid,
        reactionType: null,
        reactions: {},
        reactionCounts: {}
      });
    }
    
    res.json({ 
      success: true,
      reactions: {},
      reactionCounts: {}
    });
  } catch (error) {
    console.error('❌ Error clearing reactions:', error);
    res.status(500).json({ error: error.message });
  }
});

// Mark conversation as read (reset unread count)
router.post('/conversations/:conversationId/seen', async (req, res) => {
  try {
    const { conversationId } = req.params;
    const { userId } = req.body;

    if (!userId) {
      return res.status(400).json({ error: 'Missing userId' });
    }
    
    await db.collection('conversations').doc(conversationId).update({
      [`unreadCounts.${userId}`]: 0
    });
    
    console.log(`👀 Mark as read for user ${userId} in conversation ${conversationId}`);
    res.json({ success: true });
  } catch (error) {
    console.error('Error marking as read:', error);
    res.status(500).json({ error: error.message });
  }
});

module.exports = router;
