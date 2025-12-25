const express = require('express');
const router = express.Router();
const { authenticateUser, db, admin } = require('../middleware/auth');

/**
 * POST /api/messages - Send a new message
 */
router.post('/', authenticateUser, async (req, res) => {
  try {
    const { conversationId, type, content } = req.body;
    
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
    
    console.log(`📤 Sending message in conversation ${conversationId} from ${req.user.uid}`);
    
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
    
    // Emit WebSocket event
    const io = req.app.get('io');
    if (io) {
      const messageWithId = { ...message, id: messageRef.id };
      console.log(`📡 Emitting new_message to conversation:${conversationId}`);
      io.to(`conversation:${conversationId}`).emit('new_message', messageWithId);
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

module.exports = router;
