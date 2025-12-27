const socketIO = require('socket.io');
const { auth, db } = require('../middleware/auth');

function initializeWebSocket(server) {
  const io = socketIO(server, {
    cors: {
      origin: process.env.ALLOWED_ORIGINS.split(','),
      methods: ['GET', 'POST'],
      credentials: true
    }
  });
  
  global.io = io;
  
  io.use(async (socket, next) => {
    try {
      const token = socket.handshake.auth.token;
      if (!token) return next(new Error('No token'));
      const decodedToken = await auth.verifyIdToken(token);
      socket.userId = decodedToken.uid;
      next();
    } catch (error) {
      next(new Error('Authentication error'));
    }
  });
  
  io.on('connection', async (socket) => {
    console.log(`🔌 Connected: ${socket.userId}`);
    socket.join(`user:${socket.userId}`);
    
    // Update online status and notify friends
    await updateOnlineStatus(socket.userId, true);
    await notifyFriendsOfStatusChange(io, socket.userId, true);
    
    socket.on('join_conversation', (id) => socket.join(`conversation:${id}`));
    socket.on('leave_conversation', (id) => socket.leave(`conversation:${id}`));
    socket.on('typing', (data) => {
      socket.to(`conversation:${data.conversationId}`).emit('user_typing', {
        userId: socket.userId,
        isTyping: data.isTyping
      });
    });
    
    socket.on('disconnect', async () => {
      console.log(`🔌 Disconnected: ${socket.userId}`);
      // Update offline status and notify friends
      await updateOnlineStatus(socket.userId, false);
      await notifyFriendsOfStatusChange(io, socket.userId, false);
    });
  });
  
  console.log('✅ WebSocket server initialized');
  return io;
}

/**
 * Update user's online status in Firestore
 */
async function updateOnlineStatus(userId, isOnline) {
  try {
    await db.collection('users').doc(userId).update({
      isOnline: isOnline,
      lastSeen: Date.now()
    });
    console.log(`📡 User ${userId} is now ${isOnline ? 'online' : 'offline'}`);
  } catch (error) {
    console.error('Error updating online status:', error);
  }
}

/**
 * Notify all friends about user's status change
 */
async function notifyFriendsOfStatusChange(io, userId, isOnline) {
  try {
    // Get user's friends list
    const userDoc = await db.collection('users').doc(userId).get();
    if (!userDoc.exists) return;
    
    const userData = userDoc.data();
    const friends = userData.friends || [];
    
    // Notify each friend
    for (const friendId of friends) {
      io.to(`user:${friendId}`).emit('friend_status_changed', {
        friendId: userId,
        isOnline: isOnline,
        lastSeen: Date.now()
      });
    }
    
    console.log(`📡 Notified ${friends.length} friends about ${userId}'s status change`);
  } catch (error) {
    console.error('Error notifying friends of status change:', error);
  }
}

function broadcastMessage(io, conversationId, message) {
  io.to(`conversation:${conversationId}`).emit('new_message', message);
}

module.exports = { initializeWebSocket, broadcastMessage };
