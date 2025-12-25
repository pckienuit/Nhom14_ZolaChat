#!/bin/bash
################################################################################
# Setup Nginx and SSL for API Server
# Run this on VPS after deploying the API server
################################################################################

set -e

echo "========================================="
echo "Nginx & SSL Setup for API Server"
echo "========================================="
echo ""

# Check if running as root
if [ "$EUID" -ne 0 ]; then 
    echo "Please run as root"
    exit 1
fi

# Variables
DOMAIN="api.zolachat.site"
API_PORT="3000"

################################################################################
# STEP 1: Install Nginx
################################################################################

echo "[STEP 1] Installing Nginx..."
if ! command -v nginx &> /dev/null; then
    apt-get update -qq
    apt-get install -y nginx
    echo "✓ Nginx installed"
else
    echo "✓ Nginx already installed"
fi

################################################################################
# STEP 2: Create Nginx Configuration
################################################################################

echo ""
echo "[STEP 2] Creating Nginx configuration..."

cat > /etc/nginx/sites-available/zaloclone-api << 'NGINX_CONFIG'
server {
    listen 80;
    server_name api.zolachat.site;

    # Security headers
    add_header X-Frame-Options "SAMEORIGIN" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;

    # Logging
    access_log /var/log/nginx/zaloclone-api-access.log;
    error_log /var/log/nginx/zaloclone-api-error.log;

    # API endpoints
    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        
        # WebSocket support
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        
        # Standard proxy headers
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # Buffering
        proxy_buffering off;
        proxy_cache_bypass $http_upgrade;
    }

    # Health check endpoint (no auth required)
    location /health {
        proxy_pass http://localhost:3000/health;
        access_log off;
    }
}
NGINX_CONFIG

echo "✓ Nginx config created"

################################################################################
# STEP 3: Enable Site
################################################################################

echo ""
echo "[STEP 3] Enabling site..."

# Remove existing symlink if any
rm -f /etc/nginx/sites-enabled/zaloclone-api

# Create symlink
ln -s /etc/nginx/sites-available/zaloclone-api /etc/nginx/sites-enabled/

echo "✓ Site enabled"

################################################################################
# STEP 4: Test Nginx Configuration
################################################################################

echo ""
echo "[STEP 4] Testing Nginx configuration..."

nginx -t

echo "✓ Nginx configuration valid"

################################################################################
# STEP 5: Reload Nginx
################################################################################

echo ""
echo "[STEP 5] Reloading Nginx..."

systemctl reload nginx
systemctl status nginx --no-pager -l

echo "✓ Nginx reloaded"

################################################################################
# STEP 6: Install Certbot
################################################################################

echo ""
echo "[STEP 6] Installing Certbot for SSL..."

if ! command -v certbot &> /dev/null; then
    apt-get install -y certbot python3-certbot-nginx
    echo "✓ Certbot installed"
else
    echo "✓ Certbot already installed"
fi

################################################################################
# STEP 7: Obtain SSL Certificate
################################################################################

echo ""
echo "[STEP 7] Obtaining SSL certificate..."
echo ""
echo "IMPORTANT: Make sure DNS for $DOMAIN points to this server!"
echo "Current server IP: $(curl -s ifconfig.me)"
echo ""
read -p "Press Enter to continue with SSL setup (or Ctrl+C to cancel)..."

certbot --nginx -d $DOMAIN --non-interactive --agree-tos --email admin@zolachat.site || {
    echo ""
    echo "⚠ SSL certificate setup failed"
    echo "You can run it manually later:"
    echo "  certbot --nginx -d $DOMAIN"
    echo ""
    echo "Or test HTTP first:"
    echo "  curl http://$DOMAIN/health"
}

################################################################################
# STEP 8: Setup Auto-Renewal
################################################################################

echo ""
echo "[STEP 8] Setting up SSL auto-renewal..."

# Test renewal
certbot renew --dry-run || echo "⚠ Renewal test failed, but continuing..."

echo "✓ Auto-renewal configured"

################################################################################
# STEP 9: Verification
################################################################################

echo ""
echo "========================================="
echo "Setup Complete!"
echo "========================================="
echo ""

echo "Testing endpoints..."
echo ""

# Test HTTP
echo "1. Testing HTTP (localhost):"
curl -s http://localhost:3000/health | python3 -m json.tool || echo "Failed"

echo ""
echo "2. Testing via Nginx (HTTP):"
curl -s http://$DOMAIN/health | python3 -m json.tool || echo "Failed"

echo ""
echo "3. Testing HTTPS:"
curl -s https://$DOMAIN/health | python3 -m json.tool || echo "Failed"

echo ""
echo "========================================="
echo "Configuration Summary"
echo "========================================="
echo ""
echo "Domain:       $DOMAIN"
echo "API Port:     $API_PORT"
echo "Nginx Config: /etc/nginx/sites-available/zaloclone-api"
echo "SSL Cert:     Managed by Certbot"
echo ""
echo "Logs:"
echo "  Access: /var/log/nginx/zaloclone-api-access.log"
echo "  Error:  /var/log/nginx/zaloclone-api-error.log"
echo ""
echo "Useful commands:"
echo "  nginx -t                    # Test config"
echo "  systemctl reload nginx      # Reload nginx"
echo "  certbot certificates        # Check SSL status"
echo "  certbot renew              # Renew SSL manually"
echo "  tail -f /var/log/nginx/zaloclone-api-error.log"
echo ""
echo "✓ All done!"
