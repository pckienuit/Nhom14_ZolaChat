#!/bin/bash
# ============================================
# Deploy Admin Web to VPS (163.61.182.20)
# ============================================
#
# Script này deploy Admin Web lên VPS đã được setup trước đó.
# Chạy script này từ máy local trong thư mục DoAn_ZaloClone.
#
# Yêu cầu:
#   - SSH access tới VPS (ssh root@163.61.182.20)
#   - VPS đã cài Apache2 (từ setup_vps_sticker_server.sh)
#
# Usage: bash scripts/deploy_admin_web.sh

set -e

VPS_HOST="root@163.61.182.20"
VPS_ADMIN_DIR="/var/www/admin"
LOCAL_ADMIN_DIR="admin-web"

echo "========================================"
echo "🚀 Deploying Admin Web to VPS"
echo "========================================"
echo ""

# Check if admin-web directory exists
if [ ! -d "$LOCAL_ADMIN_DIR" ]; then
    echo "❌ Error: $LOCAL_ADMIN_DIR directory not found!"
    echo "Make sure you're running this from the DoAn_ZaloClone root directory."
    exit 1
fi

# Create directory on VPS
echo "📁 Creating directory on VPS..."
ssh $VPS_HOST "mkdir -p $VPS_ADMIN_DIR"

# Upload files to VPS
echo "📤 Uploading files..."
scp -r $LOCAL_ADMIN_DIR/* $VPS_HOST:$VPS_ADMIN_DIR/

# Set permissions
echo "🔐 Setting permissions..."
ssh $VPS_HOST "chown -R www-data:www-data $VPS_ADMIN_DIR && chmod -R 755 $VPS_ADMIN_DIR"

# Configure Apache
echo "⚙️  Configuring Apache..."
ssh $VPS_HOST "cat > /etc/apache2/sites-available/admin.conf << 'EOF'
<VirtualHost *:80>
    ServerName admin.zaloclone.local
    DocumentRoot /var/www/admin
    
    <Directory /var/www/admin>
        Options -Indexes +FollowSymLinks
        AllowOverride All
        Require all granted
        
        # SPA fallback (optional)
        FallbackResource /index.html
    </Directory>
    
    # Security headers
    Header always set X-Content-Type-Options \"nosniff\"
    Header always set X-Frame-Options \"SAMEORIGIN\"
    Header always set X-XSS-Protection \"1; mode=block\"
    
    # Cache static assets
    <FilesMatch \"\.(css|js|png|jpg|jpeg|gif|ico|svg|woff|woff2)$\">
        Header set Cache-Control \"max-age=86400, public\"
    </FilesMatch>
    
    ErrorLog \${APACHE_LOG_DIR}/admin_error.log
    CustomLog \${APACHE_LOG_DIR}/admin_access.log combined
</VirtualHost>
EOF"

# Enable site and reload Apache
echo "🔄 Enabling site and reloading Apache..."
ssh $VPS_HOST "a2ensite admin.conf 2>/dev/null || true"
ssh $VPS_HOST "a2enmod headers rewrite 2>/dev/null || true"
ssh $VPS_HOST "systemctl reload apache2"

echo ""
echo "========================================"
echo "✅ Deployment Complete!"
echo "========================================"
echo ""
echo "Admin Web is available at:"
echo "  📍 http://163.61.182.20/admin/"
echo ""
echo "Or if you set up the virtual host correctly:"
echo "  📍 http://admin.zaloclone.local/"
echo ""
echo "Default login credentials:"
echo "  Email: (use your Firebase admin account)"
echo ""
