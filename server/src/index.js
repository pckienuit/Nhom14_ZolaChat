const express = require('express');
const http = require('http');
const cors = require('cors');
const helmet = require('helmet');
const compression = require('compression');
const rateLimit = require('express-rate-limit');
const path = require('path');
require('dotenv').config();

const { initializeWebSocket } = require('./websocket');
const authRoutes = require('./routes/auth');
const userRoutes = require('./routes/users');
const chatRoutes = require('./routes/chats');
const conversationRoutes = require('./routes/conversations');
const callRoutes = require('./routes/calls');
const friendRoutes = require('./routes/friends');
const stickerRoutes = require('./routes/stickers');
const messageRoutes = require('./routes/messages');

const app = express();
const server = http.createServer(app);

// Initialize WebSocket
const io = initializeWebSocket(server);

// Make io available in routes for emitting events
app.set('io', io);

// Middleware
app.use(helmet({ contentSecurityPolicy: false }));
app.use(cors({
  origin: (origin, callback) => {
    const allowedOrigins = process.env.ALLOWED_ORIGINS.split(',');
    if (!origin || allowedOrigins.includes(origin)) {
      callback(null, true);
    } else {
      callback(new Error('Not allowed by CORS'));
    }
  },
  credentials: true
}));

app.use(compression());
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Serve static uploads (stickers, etc.)
// index.js is in src/, uploads is in root/uploads, so we go up one level
app.use('/uploads', express.static(path.join(__dirname, '../uploads')));

// Rate limiting
// Rate limiting
const limiter = rateLimit({
  windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS) || 1 * 60 * 1000, // 1 minute
  max: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS) || 500 // 500 requests per window
});
app.use('/api/', limiter);

// Request logging
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
  next();
});

// Health check
app.get('/health', (req, res) => {
  const memUsage = process.memoryUsage();
  
  res.json({ 
    status: 'ok', 
    timestamp: Date.now(),
    uptime: process.uptime(),
    memory: {
      rss: Math.round(memUsage.rss / 1024 / 1024) + ' MB',
      heapUsed: Math.round(memUsage.heapUsed / 1024 / 1024) + ' MB',
      heapTotal: Math.round(memUsage.heapTotal / 1024 / 1024) + ' MB'
    },
    websocket: {
      connected: io ? io.engine.clientsCount : 0
    },
    routes: {
      auth: '/api/auth',
      users: '/api/users',
      chats: '/api/chats',
      conversations: '/api/conversations',
      calls: '/api/calls',
      friends: '/api/friends',
      stickers: '/api/stickers',
      messages: '/api/messages'
    },
    version: process.env.npm_package_version || '1.0.0'
  });
});

// API Routes
app.use('/api/auth', authRoutes);
app.use('/api/users', userRoutes);
app.use('/api/chats', chatRoutes);
app.use('/api/conversations', conversationRoutes);
app.use('/api/calls', callRoutes);
app.use('/api/friends', friendRoutes);
app.use('/api/stickers', stickerRoutes);
app.use('/api/messages', messageRoutes);


// 404 handler
app.use((req, res) => {
  res.status(404).json({ error: 'Not Found' });
});

// Error handler
app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(err.status || 500).json({
    error: { message: err.message || 'Internal Server Error' }
  });
});

// Start server
const PORT = process.env.PORT || 3000;
server.listen(PORT, () => {
  console.log('==========================================');
  console.log(`ZaloClone API Server`);
  console.log(`HTTP Server: http://localhost:${PORT}`);
  console.log('==========================================');
});

process.on('SIGTERM', () => {
  server.close(() => process.exit(0));
});
