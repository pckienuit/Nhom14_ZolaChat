const express = require('express');
const router = express.Router();
const path = require('path');
const fs = require('fs');
const { optionalAuth, authenticateUser, requireAdmin, db } = require('../middleware/auth');

// Middleware to parse multipart form data without external dependencies
// Using built-in Node.js capabilities
const parseMultipart = async (req, res, next) => {
  if (!req.headers['content-type']?.includes('multipart/form-data')) {
    return next();
  }

  const contentType = req.headers['content-type'];
  const boundary = contentType.split('boundary=')[1];
  
  if (!boundary) {
    console.log('[Upload] No boundary found in content-type');
    return next(); // Let distinct middleware handle if needed, or fail later
  }

  try {
    const chunks = [];
    for await (const chunk of req) {
      chunks.push(chunk);
    }
    const buffer = Buffer.concat(chunks);
    
    // Parse multipart data
    const parts = parseMultipartBuffer(buffer, boundary);
    req.files = {};
    req.body = req.body || {};
    
    for (const part of parts) {
      if (part.filename) {
        req.files[part.name] = {
          originalname: part.filename,
          buffer: part.data,
          mimetype: part.contentType || 'application/octet-stream',
          size: part.data.length
        };
      } else if (part.name) {
        req.body[part.name] = part.data.toString('utf8');
      }
    }
    next();
  } catch (error) {
    console.error('[Upload] Error parsing multipart:', error);
    res.status(400).json({ error: 'Invalid multipart request' });
  }
};

function parseMultipartBuffer(buffer, boundary) {
  const parts = [];
  const boundaryBuffer = Buffer.from('--' + boundary);
  const str = buffer.toString('binary');
  
  let start = str.indexOf(boundaryBuffer.toString('binary'));
  while (start !== -1) {
    const end = str.indexOf(boundaryBuffer.toString('binary'), start + boundaryBuffer.length);
    if (end === -1) break;
    
    const partStr = str.substring(start + boundaryBuffer.length + 2, end - 2);
    const headerEnd = partStr.indexOf('\r\n\r\n');
    
    if (headerEnd !== -1) {
      const headerStr = partStr.substring(0, headerEnd);
      const dataStr = partStr.substring(headerEnd + 4);
      
      const part = parsePartHeaders(headerStr);
      part.data = Buffer.from(dataStr, 'binary');
      parts.push(part);
    }
    
    start = end;
  }
  
  return parts;
}

function parsePartHeaders(headerStr) {
  const part = {};
  const lines = headerStr.split('\r\n');
  
  for (const line of lines) {
    if (line.toLowerCase().startsWith('content-disposition:')) {
      const nameMatch = line.match(/name="([^"]+)"/);
      const filenameMatch = line.match(/filename="([^"]+)"/);
      if (nameMatch) part.name = nameMatch[1];
      if (filenameMatch) part.filename = filenameMatch[1];
    } else if (line.toLowerCase().startsWith('content-type:')) {
      part.contentType = line.split(':')[1].trim();
    }
  }
  
  return part;
}

// Get all sticker packs
router.get('/packs', optionalAuth, async (req, res) => {
  try {
    const snapshot = await db.collection('stickerPacks').orderBy('name').get();
    const packs = [];
    snapshot.forEach(doc => packs.push({ id: doc.id, ...doc.data() }));
    res.json({ packs });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Get pack detail with stickers
router.get('/packs/:packId', optionalAuth, async (req, res) => {
  try {
    const { packId } = req.params;
    const packDoc = await db.collection('stickerPacks').doc(packId).get();
    if (!packDoc.exists) return res.status(404).json({ error: 'Not found' });
    
    const stickersSnapshot = await db.collection('stickerPacks')
      .doc(packId).collection('stickers').get();
    const stickers = [];
    stickersSnapshot.forEach(doc => stickers.push({ id: doc.id, ...doc.data() }));
    
    res.json({ id: packDoc.id, ...packDoc.data(), stickers });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

// Create pack (admin only)
router.post('/packs', authenticateUser, requireAdmin, async (req, res) => {
  try {
    const { name, description, category, stickers } = req.body;
    const pack = {
      name, description: description || '', category: category || 'general',
      downloadCount: 0, createdAt: Date.now()
    };
    const packRef = await db.collection('stickerPacks').add(pack);
    res.json({ success: true, packId: packRef.id });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});

/**
 * Upload sticker file
 * POST /api/stickers/upload
 * Content-Type: multipart/form-data
 */
router.post('/upload', parseMultipart, async (req, res) => {
  try {
    console.log('[Sticker Upload] Request received');
    
    const file = req.files?.sticker;
    const userId = req.body?.userId;
    
    if (!file) {
      console.log('[Sticker Upload] No file provided');
      return res.status(400).json({ success: false, error: 'No sticker file provided' });
    }
    
    // Validate file type
    const allowedTypes = ['image/png', 'image/jpeg', 'image/gif', 'image/webp'];
    if (!allowedTypes.includes(file.mimetype)) {
      return res.status(400).json({ success: false, error: 'Invalid file type. Allowed: PNG, JPEG, GIF, WebP' });
    }
    
    // Ensure upload directory exists
    const uploadDir = path.join(__dirname, '../../uploads/stickers');
    if (!fs.existsSync(uploadDir)) {
      fs.mkdirSync(uploadDir, { recursive: true });
    }
    
    // Generate filename
    const ext = path.extname(file.originalname) || '.webp';
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(2, 10);
    const filename = `sticker_${timestamp}_${random}${ext}`;
    
    // Save file
    const filepath = path.join(uploadDir, filename);
    fs.writeFileSync(filepath, file.buffer);
    console.log('[Sticker Upload] Saved to:', filepath);
    
    // Generate URL - Use HTTPS by default
    // Check if hosted on zolachat.site
    let baseUrl = process.env.BASE_URL;
    if (!baseUrl) {
        // Fallback detection
        const host = req.get('host') || 'zolachat.site';
        const protocol = req.secure || host.includes('zolachat.site') ? 'https' : 'http';
        baseUrl = `${protocol}://${host}`;
    }
    
    const stickerUrl = `${baseUrl}/uploads/stickers/${filename}`;
    
    const sticker = {
      id: `sticker_${timestamp}_${random}`,
      url: stickerUrl,
      imageUrl: stickerUrl,
      thumbnailUrl: stickerUrl,
      width: 512,
      height: 512,
      isAnimated: file.mimetype === 'image/gif',
      format: ext.replace('.', ''),
      createdAt: timestamp,
      creatorId: userId || null
    };
    
    console.log('[Sticker Upload] Success:', sticker.url);
    res.json({ success: true, sticker });
    
  } catch (error) {
    console.error('[Sticker Upload] Error:', error);
    res.status(500).json({ success: false, error: error.message });
  }
});

module.exports = router;
