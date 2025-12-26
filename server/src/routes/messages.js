const express = require('express');
const router = express.Router();
const { authenticateUser, db, admin } = require('../middleware/auth');

/**
 * POST /api/messages - Send a new message
 */
router.post('/', authenticateUser, async (req, res) => {
  try {
    const { conversationId, type, content, fileName, fileSize, fileMimeType } = req.body;
    
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
    
    // Add file metadata if present (for IMAGE, VIDEO, AUDIO, FILE types)
    if (fileName) {
      message.fileName = fileName;
      message.fileSize = fileSize || 0;
      if (fileMimeType) {
        message.fileMimeType = fileMimeType;
      }
    }
    
    console.log(`📤 Sending ${type} message in conversation ${conversationId} from ${req.user.uid}`);
    
    // Save to Firestore
    const messageRef = await db.collection('conversations')
      .doc(conversationId)
      .collection('messages')
      .add(message);
    
    console.log(`✅ Message created with ID: ${messageRef.id}`);
    
    // Update conversation lastMessage
    await db.collection('conversations').doc(conversationId).update({
      lastMessage: content || `[${type}]`,
      lastMessageTime: message.timestamp
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
            unreadUpdates[`unreadCounts.${memberId}`] = admin.firestore.FieldValue.increment(1);
          }
        });
        
        if (Object.keys(unreadUpdates).length > 0) {
          await db.collection('conversations').doc(conversationId).update(unreadUpdates);
          console.log(`📊 Incremented unreadCounts for ${Object.keys(unreadUpdates).length} members`);
        }
      }
    } catch (unreadErr) {
      console.error('Failed to update unreadCounts:', unreadErr);
      // Don't fail the request if unread update fails
    }
    
    // Emit WebSocket event
    const io = req.app.get('io');
    if (io) {
      const messageWithId = { ...message, id: messageRef.id };
      console.log(`📡 Emitting new_message to conversation:${conversationId}`);
      io.to(`conversation:${conversationId}`).emit('new_message', messageWithId);

      // Notification for Home Screen Preview (emit to each user's room)
      try {
        const conversationDoc = await db.collection('conversations').doc(conversationId).get();
        if (conversationDoc.exists) {
          const data = conversationDoc.data();
          // Support multiple field names for legacy compatibility
          const members = data.memberIds || data.participantIds || data.participants || [];
          
          console.log(`DEBUG_V3: Conversation ${conversationId} has members: ${JSON.stringify(members)}`);
          
          members.forEach(memberId => {
            // Avoid sending double to sender (optional, but sender already has it via API response)
            // But sender might need it for Home screen update if they go back quickly?
            // Let's send to everyone. Client handles duplicates.
            io.to(`user:${memberId}`).emit('new_message', messageWithId);
            console.log(`DEBUG_V3: Emitted to user:${memberId}`);
          });
        } else {
            console.warn(`DEBUG_V3: Conversation ${conversationId} not found in Firestore`);
        }
      } catch (err) {
        console.error('Failed to send notifications to members', err);
      }
    } else {
      console.warn('⚠️ io is null, cannot emit new_message event');
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
 * DELETE /api/messages/:messageId/reactions - Clear ALL reactions on a message
 */
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

module.exports = router;
