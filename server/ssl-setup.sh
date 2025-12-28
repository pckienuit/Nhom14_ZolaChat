#!/bin/bash

# SSL Setup Script for Janus Gateway
# Domain: zolachat.site
# Requires: Domain DNS pointing to 163.61.182.20

set -e

DOMAIN="zolachat.site"
JANUS_DOMAIN="janus.${DOMAIN}"

echo "========================================="
echo " SSL Setup for Janus Gateway"
echo "========================================="

# Install certbot if not already installed
if ! command -v certbot &> /dev/null; then
    echo "Installing certbot..."
    apt install -y certbot python3-certbot-nginx
fi

# Get SSL certificate
echo "Obtaining SSL certificate for ${JANUS_DOMAIN}..."
certbot certonly --standalone \
    -d ${JANUS_DOMAIN} \
    --non-interactive \
    --agree-tos \
    --email admin@${DOMAIN} \
    --preferred-challenges http

# Copy certificates to Janus directory
echo "Copying certificates to Janus..."
mkdir -p /opt/janus/share/janus/certs
cp /etc/letsencrypt/live/${JANUS_DOMAIN}/fullchain.pem /opt/janus/share/janus/certs/
cp /etc/letsencrypt/live/${JANUS_DOMAIN}/privkey.pem /opt/janus/share/janus/certs/

# Configure Janus to use SSL
echo "Configuring Janus for HTTPS..."

# Update janus.transport.http.jcfg
cat > /opt/janus/etc/janus/janus.transport.http.jcfg <<EOF
general: {
    https = true
    secure_port = 8089
    
    # SSL certificates
    cert_pem = "/opt/janus/share/janus/certs/fullchain.pem"
    cert_key = "/opt/janus/share/janus/certs/privkey.pem"
}
EOF

# Update janus.transport.websockets.jcfg
cat > /opt/janus/etc/janus/janus.transport.websockets.jcfg <<EOF
general: {
    ws = true
    ws_port = 8188
    
    wss = true
    wss_port = 8989
    
    # SSL certificates
    cert_pem = "/opt/janus/share/janus/certs/fullchain.pem"
    cert_key = "/opt/janus/share/janus/certs/privkey.pem"
}
EOF

# Setup auto-renewal
echo "Setting up auto-renewal..."
(crontab -l 2>/dev/null; echo "0 3 * * * certbot renew --quiet --deploy-hook 'systemctl restart janus'") | crontab -

# Restart Janus
echo "Restarting Janus..."
systemctl restart janus

echo ""
echo "========================================="
echo " SSL Setup Complete!"
echo "========================================="
echo ""
echo "Janus is now available at:"
echo "  - HTTPS:      https://${JANUS_DOMAIN}:8089"
echo "  - WSS:        wss://${JANUS_DOMAIN}:8989"
echo ""
echo "Certificate will auto-renew every 3 months"
echo ""
