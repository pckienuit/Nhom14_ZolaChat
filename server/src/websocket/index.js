const socketIO = require('socket.io');
const { auth } = require('../middleware/auth');

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
  
  io.on('connection', (socket) => {
    console.log(`🔌 Connected: ${socket.userId}`);
    socket.join(`user:${socket.userId}`);
    
    socket.on('join_conversation', (id) => socket.join(`conversation:${id}`));
    socket.on('leave_conversation', (id) => socket.leave(`conversation:${id}`));
    socket.on('typing', (data) => {
      socket.to(`conversation:${data.conversationId}`).emit('user_typing', {
        userId: socket.userId,
        isTyping: data.isTyping
      });
    });
    socket.on('disconnect', () => console.log(`🔌 Disconnected: ${socket.userId}`));
  });
  
  console.log('✅ WebSocket server initialized');
  return io;
}

function broadcastMessage(io, conversationId, message) {
  io.to(`conversation:${conversationId}`).emit('new_message', message);
}

module.exports = { initializeWebSocket, broadcastMessage };
