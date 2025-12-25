#!/bin/bash
# =============================================================================
# VPS Sticker Server Setup Script
# Target: Ubuntu/Debian VPS (2GB RAM, 16GB SSD)
# Server IP: 163.61.182.20
# =============================================================================

set -e

echo "==========================================="
echo "Sticker Server Setup for Zalo Clone"
echo "VPS Specs: 2GB RAM, 16GB SSD"
echo "==========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
STICKER_ROOT="/var/www/stickers"
APACHE_CONF="/etc/apache2/sites-available/stickers.conf"
UPLOAD_SERVICE_DIR="/opt/sticker-upload-service"
SERVER_IP="163.61.182.20"  # Change this to your VPS IP

# =============================================================================
# 1. Update System
# =============================================================================
echo -e "${GREEN}[1/7] Updating system packages...${NC}"
apt-get update -y
apt-get upgrade -y

# =============================================================================
# 2. Install Apache2
# =============================================================================
echo -e "${GREEN}[2/7] Installing Apache2...${NC}"
apt-get install -y apache2

# Enable required Apache modules
a2enmod headers
a2enmod rewrite
a2enmod expires
a2enmod proxy
a2enmod proxy_http

# =============================================================================
# 3. Create Sticker Directory Structure
# =============================================================================
echo -e "${GREEN}[3/7] Creating sticker directory structure...${NC}"

mkdir -p ${STICKER_ROOT}/official
mkdir -p ${STICKER_ROOT}/user
mkdir -p ${STICKER_ROOT}/thumbnails
mkdir -p ${STICKER_ROOT}/temp

# Set proper permissions
chown -R www-data:www-data ${STICKER_ROOT}
chmod -R 755 ${STICKER_ROOT}

# Create sample directory structure
mkdir -p ${STICKER_ROOT}/official/sample_pack
echo "Sample sticker pack directory created" > ${STICKER_ROOT}/official/sample_pack/README.txt

# =============================================================================
# 4. Configure Apache Virtual Host
# =============================================================================
echo -e "${GREEN}[4/7] Configuring Apache for sticker serving...${NC}"

cat > ${APACHE_CONF} << 'EOF'
<VirtualHost *:80>
    ServerName ${SERVER_IP}
    DocumentRoot /var/www/stickers

    # ==================================
    # Sticker Files Directory
    # ==================================
    <Directory /var/www/stickers>
        Options Indexes FollowSymLinks
        AllowOverride All
        Require all granted
        
        # Enable CORS for Android app access
        Header always set Access-Control-Allow-Origin "*"
        Header always set Access-Control-Allow-Methods "GET, HEAD, OPTIONS"
        Header always set Access-Control-Allow-Headers "Range, Content-Type"
    </Directory>

    # ==================================
    # Static Sticker Files with Caching
    # ==================================
    <LocationMatch "^/stickers/">
        # Cache stickers for 30 days (immutable content)
        ExpiresActive On
        ExpiresDefault "access plus 30 days"
        Header set Cache-Control "public, max-age=2592000, immutable"
    </LocationMatch>

    # ==================================
    # MIME Types for Sticker Formats
    # ==================================
    AddType image/webp .webp
    AddType image/gif .gif
    AddType image/png .png
    AddType image/jpeg .jpg .jpeg
    AddType application/json .json

    # Lottie animation files
    AddType application/json .lottie
    
    # APNG (Animated PNG)
    AddType image/apng .apng

    # ==================================
    # Compression for faster loading
    # ==================================
    <IfModule mod_deflate.c>
        AddOutputFilterByType DEFLATE application/json
        AddOutputFilterByType DEFLATE text/plain
    </IfModule>

    # ==================================
    # Upload API Proxy (to Node.js service)
    # ==================================
    <Location "/api/stickers">
        ProxyPass http://127.0.0.1:3001
        ProxyPassReverse http://127.0.0.1:3001
    </Location>

    # ==================================
    # Security Headers
    # ==================================
    Header always set X-Content-Type-Options "nosniff"
    Header always set X-Frame-Options "DENY"

    # ==================================
    # Logging
    # ==================================
    ErrorLog ${APACHE_LOG_DIR}/sticker_error.log
    CustomLog ${APACHE_LOG_DIR}/sticker_access.log combined

    # Log only errors to save disk space on 16GB SSD
    LogLevel warn
</VirtualHost>
EOF

# Replace SERVER_IP placeholder
sed -i "s/\${SERVER_IP}/${SERVER_IP}/g" ${APACHE_CONF}

# Enable the site
a2ensite stickers.conf

# Disable default site (optional)
# a2dissite 000-default.conf

# =============================================================================
# 5. Install Node.js for Upload Service
# =============================================================================
echo -e "${GREEN}[5/7] Installing Node.js for upload service...${NC}"

# Install Node.js 18.x LTS
curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
apt-get install -y nodejs

# =============================================================================
# 6. Create Sticker Upload Service
# =============================================================================
echo -e "${GREEN}[6/7] Creating sticker upload service...${NC}"

mkdir -p ${UPLOAD_SERVICE_DIR}
cd ${UPLOAD_SERVICE_DIR}

# Create package.json
cat > package.json << 'EOF'
{
  "name": "sticker-upload-service",
  "version": "1.0.0",
  "description": "Upload service for Zalo Clone stickers",
  "main": "server.js",
  "scripts": {
    "start": "node server.js"
  },
  "dependencies": {
    "express": "^4.18.2",
    "multer": "^1.4.5-lts.1",
    "sharp": "^0.32.6",
    "uuid": "^9.0.0",
    "cors": "^2.8.5"
  }
}
EOF

# Create server.js
cat > server.js << 'EOF'
const express = require('express');
const multer = require('multer');
const sharp = require('sharp');
const { v4: uuidv4 } = require('uuid');
const cors = require('cors');
const path = require('path');
const fs = require('fs');

const app = express();
app.use(cors());
app.use(express.json());

const STICKER_ROOT = '/var/www/stickers';
const SERVER_URL = process.env.SERVER_URL || 'http://163.61.182.20';

// Configure multer for file upload
const storage = multer.diskStorage({
    destination: (req, file, cb) => {
        cb(null, path.join(STICKER_ROOT, 'temp'));
    },
    filename: (req, file, cb) => {
        const ext = path.extname(file.originalname);
        cb(null, `${uuidv4()}${ext}`);
    }
});

const upload = multer({
    storage: storage,
    limits: {
        fileSize: 2 * 1024 * 1024  // 2MB max (optimized for 16GB SSD)
    },
    fileFilter: (req, file, cb) => {
        // Supported formats: WebP, GIF, PNG, JPEG, Lottie JSON, APNG
        const allowedMimes = [
            'image/webp',
            'image/gif', 
            'image/png',
            'image/jpeg',
            'image/apng',
            'application/json'  // Lottie
        ];
        if (allowedMimes.includes(file.mimetype)) {
            cb(null, true);
        } else {
            cb(new Error('Unsupported file format'));
        }
    }
});

// Health check
app.get('/api/stickers/health', (req, res) => {
    res.json({ status: 'ok', timestamp: new Date().toISOString() });
});

// Upload custom sticker
app.post('/api/stickers/upload', upload.single('sticker'), async (req, res) => {
    try {
        const { userId, packId } = req.body;
        
        if (!userId) {
            return res.status(400).json({ error: 'userId is required' });
        }

        const file = req.file;
        if (!file) {
            return res.status(400).json({ error: 'No file uploaded' });
        }

        const stickerId = uuidv4();
        const userDir = path.join(STICKER_ROOT, 'user', userId);
        const targetDir = packId 
            ? path.join(userDir, `pack_${packId}`)
            : path.join(userDir, 'custom_stickers');

        // Create directories if not exist
        fs.mkdirSync(targetDir, { recursive: true });
        fs.mkdirSync(path.join(STICKER_ROOT, 'thumbnails', userId), { recursive: true });

        const ext = path.extname(file.originalname);
        const originalFileName = `${stickerId}${ext}`;
        const thumbnailFileName = `${stickerId}_thumb.webp`;

        // Move original file
        const originalPath = path.join(targetDir, originalFileName);
        fs.renameSync(file.path, originalPath);

        // Generate thumbnail (128x128 WebP)
        const thumbnailPath = path.join(STICKER_ROOT, 'thumbnails', userId, thumbnailFileName);
        
        // Handle different formats
        if (ext === '.json') {
            // Lottie JSON - no thumbnail processing, use first frame placeholder
            fs.copyFileSync(originalPath, thumbnailPath.replace('.webp', '.json'));
        } else {
            await sharp(originalPath)
                .resize(128, 128, { fit: 'contain', background: { r: 0, g: 0, b: 0, alpha: 0 } })
                .webp({ quality: 80 })
                .toFile(thumbnailPath);
        }

        // Get image info
        let width = 512, height = 512, isAnimated = false;
        
        if (ext !== '.json') {
            const metadata = await sharp(originalPath).metadata();
            width = metadata.width || 512;
            height = metadata.height || 512;
            isAnimated = ext === '.gif' || ext === '.webp' && metadata.pages > 1;
        } else {
            // Lottie is always animated
            isAnimated = true;
        }

        res.json({
            success: true,
            sticker: {
                id: stickerId,
                url: `${SERVER_URL}/user/${userId}/${packId ? `pack_${packId}` : 'custom_stickers'}/${originalFileName}`,
                thumbnailUrl: `${SERVER_URL}/thumbnails/${userId}/${thumbnailFileName}`,
                width,
                height,
                isAnimated,
                format: ext.substring(1)
            }
        });

    } catch (error) {
        console.error('Upload error:', error);
        res.status(500).json({ error: error.message });
    }
});

// Create sticker pack
app.post('/api/stickers/packs', upload.single('icon'), async (req, res) => {
    try {
        const { userId, packName } = req.body;

        if (!userId || !packName) {
            return res.status(400).json({ error: 'userId and packName are required' });
        }

        const packId = uuidv4();
        const packDir = path.join(STICKER_ROOT, 'user', userId, `pack_${packId}`);
        
        fs.mkdirSync(packDir, { recursive: true });

        // Process pack icon if provided
        let iconUrl = null;
        if (req.file) {
            const iconPath = path.join(packDir, 'icon.webp');
            await sharp(req.file.path)
                .resize(96, 96, { fit: 'cover' })
                .webp({ quality: 80 })
                .toFile(iconPath);
            
            fs.unlinkSync(req.file.path);
            iconUrl = `${SERVER_URL}/user/${userId}/pack_${packId}/icon.webp`;
        }

        res.json({
            success: true,
            pack: {
                id: packId,
                name: packName,
                iconUrl,
                path: `/user/${userId}/pack_${packId}`
            }
        });

    } catch (error) {
        console.error('Create pack error:', error);
        res.status(500).json({ error: error.message });
    }
});

// Disk usage check (important for 16GB SSD)
app.get('/api/stickers/disk-usage', (req, res) => {
    const { execSync } = require('child_process');
    try {
        const result = execSync(`du -sh ${STICKER_ROOT}`).toString();
        const [size] = result.split('\t');
        res.json({ 
            stickerDirSize: size,
            path: STICKER_ROOT
        });
    } catch (error) {
        res.status(500).json({ error: error.message });
    }
});

const PORT = process.env.PORT || 3001;
app.listen(PORT, '127.0.0.1', () => {
    console.log(`Sticker upload service running on port ${PORT}`);
});
EOF

# Install dependencies
npm install

# =============================================================================
# 7. Create Systemd Service for Upload Service
# =============================================================================
echo -e "${GREEN}[7/7] Creating systemd service...${NC}"

cat > /etc/systemd/system/sticker-upload.service << EOF
[Unit]
Description=Sticker Upload Service
After=network.target

[Service]
Type=simple
User=www-data
WorkingDirectory=${UPLOAD_SERVICE_DIR}
ExecStart=/usr/bin/node server.js
Restart=on-failure
RestartSec=10
Environment=NODE_ENV=production
Environment=SERVER_URL=http://${SERVER_IP}

# Memory limit (conserve RAM on 2GB VPS)
MemoryLimit=256M

[Install]
WantedBy=multi-user.target
EOF

# Set permissions
chown -R www-data:www-data ${UPLOAD_SERVICE_DIR}

# Reload systemd and start services
systemctl daemon-reload
systemctl enable sticker-upload
systemctl start sticker-upload

# =============================================================================
# 8. Install Python rembg Service (AI Background Removal)
# =============================================================================
echo -e "${GREEN}[8/8] Installing rembg AI background removal service...${NC}"

# Install Python 3 and pip
apt-get install -y python3 python3-pip python3-venv

# Create rembg service directory
REMBG_SERVICE_DIR="/opt/rembg-service"
mkdir -p ${REMBG_SERVICE_DIR}
cd ${REMBG_SERVICE_DIR}

# Create Python virtual environment
python3 -m venv venv
source venv/bin/activate

# Install dependencies (use lightweight model for 2GB RAM VPS)
pip install fastapi uvicorn python-multipart pillow
pip install rembg[cpu]  # CPU-only version, lighter than GPU

# Create FastAPI server
cat > main.py << 'EOF'
from fastapi import FastAPI, File, UploadFile, HTTPException
from fastapi.responses import Response
from fastapi.middleware.cors import CORSMiddleware
from rembg import remove, new_session
from PIL import Image
import io
import os
import gc

app = FastAPI(title="Rembg Background Removal API")

# Enable CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

# Use lightweight model for 2GB RAM VPS
# Options: u2net, u2netp (lightweight), u2net_human_seg, silueta
MODEL_NAME = os.getenv("REMBG_MODEL", "u2netp")  # u2netp is lighter
session = None

def get_session():
    global session
    if session is None:
        session = new_session(MODEL_NAME)
    return session

@app.post("/api/rembg")
async def remove_background(image: UploadFile = File(...)):
    """
    Remove background from uploaded image
    Returns: PNG with transparent background
    """
    # Validate file type
    if not image.content_type.startswith("image/"):
        raise HTTPException(400, "File must be an image")
    
    # Check file size (max 5MB)
    content = await image.read()
    if len(content) > 5 * 1024 * 1024:
        raise HTTPException(400, "Image too large. Max 5MB")
    
    try:
        # Remove background using rembg
        output_bytes = remove(
            content,
            session=get_session(),
            post_process_mask=True
        )
        
        # Force garbage collection to free memory on low-RAM VPS
        gc.collect()
        
        return Response(
            content=output_bytes,
            media_type="image/png",
            headers={
                "Content-Disposition": f'attachment; filename="sticker_{image.filename}.png"',
                "X-Model-Used": MODEL_NAME
            }
        )
    except Exception as e:
        raise HTTPException(500, f"Error processing image: {str(e)}")

@app.get("/api/rembg/health")
async def health_check():
    """Health check endpoint"""
    import psutil
    memory = psutil.virtual_memory()
    return {
        "status": "ok",
        "model": MODEL_NAME,
        "memory_used_percent": memory.percent,
        "memory_available_mb": memory.available // (1024 * 1024)
    }

@app.get("/api/rembg/models")
async def list_models():
    """List available models"""
    return {
        "available_models": [
            {"name": "u2net", "description": "Full model, best quality, ~170MB RAM"},
            {"name": "u2netp", "description": "Lightweight, good quality, ~70MB RAM"},
            {"name": "u2net_human_seg", "description": "Optimized for humans"},
            {"name": "silueta", "description": "Fast, good for objects"}
        ],
        "current_model": MODEL_NAME
    }
EOF

# Install psutil for health check
pip install psutil

deactivate

# Create systemd service for rembg
cat > /etc/systemd/system/rembg.service << EOF
[Unit]
Description=Rembg AI Background Removal Service
After=network.target

[Service]
Type=simple
User=www-data
WorkingDirectory=${REMBG_SERVICE_DIR}
ExecStart=${REMBG_SERVICE_DIR}/venv/bin/uvicorn main:app --host 127.0.0.1 --port 5000
Restart=on-failure
RestartSec=10
Environment=REMBG_MODEL=u2netp

# Memory management for 2GB VPS
MemoryLimit=800M
MemoryHigh=600M

[Install]
WantedBy=multi-user.target
EOF

# Set permissions
chown -R www-data:www-data ${REMBG_SERVICE_DIR}

# Add Apache proxy for rembg
cat >> ${APACHE_CONF} << 'EOF'

    # Rembg AI Background Removal Proxy
    <Location "/api/rembg">
        ProxyPass http://127.0.0.1:5000/api/rembg
        ProxyPassReverse http://127.0.0.1:5000/api/rembg
    </Location>
EOF

# Reload and start rembg service
systemctl daemon-reload
systemctl enable rembg
systemctl start rembg
systemctl restart apache2

# =============================================================================
# Final Status
# =============================================================================
echo ""
echo -e "${GREEN}==========================================="
echo "Setup Complete!"
echo "==========================================${NC}"
echo ""
echo "Sticker files directory: ${STICKER_ROOT}"
echo "Apache config: ${APACHE_CONF}"
echo "Upload service: ${UPLOAD_SERVICE_DIR}"
echo ""
echo -e "${YELLOW}Supported Sticker Formats:${NC}"
echo "  - WebP (static & animated)"
echo "  - GIF (animated)"
echo "  - PNG (static)"
echo "  - APNG (animated PNG)"
echo "  - JPEG (static)"
echo "  - Lottie JSON (animated)"
echo ""
echo -e "${YELLOW}API Endpoints:${NC}"
echo "  GET  http://${SERVER_IP}/stickers/... - Serve sticker files"
echo "  POST http://${SERVER_IP}/api/stickers/upload - Upload custom sticker"
echo "  POST http://${SERVER_IP}/api/stickers/packs - Create sticker pack"
echo "  GET  http://${SERVER_IP}/api/stickers/health - Health check"
echo "  GET  http://${SERVER_IP}/api/stickers/disk-usage - Check disk usage"
echo ""
echo -e "${YELLOW}Disk Space Optimization (16GB SSD):${NC}"
echo "  - Max sticker size: 2MB"
echo "  - Thumbnails: 128x128 WebP"
echo "  - Log level: warn (minimal logging)"
echo "  - Memory limit: 256MB for upload service"
echo ""
echo -e "${YELLOW}Test commands:${NC}"
echo "  curl http://${SERVER_IP}/api/stickers/health"
echo "  curl http://${SERVER_IP}/api/stickers/disk-usage"
echo ""
echo -e "${GREEN}Services status:${NC}"
systemctl status apache2 --no-pager | head -5
systemctl status sticker-upload --no-pager | head -5
