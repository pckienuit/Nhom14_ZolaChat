#!/bin/bash

# Configuration
DOMAIN="zolachat.site"
EMAIL="admin@zolachat.site"

echo "======================================================="
echo "   SETUP SSL FOR $DOMAIN (Ubuntu/Debian)"
echo "======================================================="

# 1. Update system and install Certbot
echo "[1/4] Installing Apache & Certbot..."
apt-get update
# Install Apache if missing, and Certbot
apt-get install -y apache2 certbot python3-certbot-apache

# Enable necessary Apache modules for API Proxy
a2enmod proxy
a2enmod proxy_http
a2enmod ssl
a2enmod rewrite
a2enmod headers

# 2. Configure Apache VirtualHost
echo "[2/4] Configuring Apache VirtualHost..."
cat > /etc/apache2/sites-available/${DOMAIN}.conf <<EOF
<VirtualHost *:80>
    ServerName ${DOMAIN}
    ServerAlias www.${DOMAIN}
    DocumentRoot /var/www/admin
    
    # Proxy API requests to backend
    ProxyPreserveHost On
    ProxyPass /api http://localhost:3000/api
    ProxyPassReverse /api http://localhost:3000/api

    <Directory "/var/www/admin">
        Options Indexes FollowSymLinks
        AllowOverride All
        Require all granted
    </Directory>

    ErrorLog \${APACHE_LOG_DIR}/${DOMAIN}_error.log
    CustomLog \${APACHE_LOG_DIR}/${DOMAIN}_access.log combined
</VirtualHost>
EOF

# Enable the new site configuration
a2ensite ${DOMAIN}.conf
systemctl reload apache2

# 3. Request SSL Certificate
echo "[3/4] Requesting SSL Certificate from Let's Encrypt..."
# Try to obtain certificate
certbot --apache \
    -d ${DOMAIN} -d www.${DOMAIN} \
    --non-interactive --agree-tos -m ${EMAIL} \
    --redirect

# 4. Final check
echo "[4/4] Verifying setup..."
if [ -f "/etc/letsencrypt/live/${DOMAIN}/fullchain.pem" ]; then
    echo "SUCCESS! SSL Certificate installed successfully."
    echo "Your website is now secure at: https://${DOMAIN}"
    echo "Admin Panel: https://${DOMAIN}/admin/dashboard.html"
else
    echo "ERROR: SSL Certificate installation failed."
    echo "Please ensure your DNS A records for $DOMAIN and www.$DOMAIN point to $(curl -s ifconfig.me)"
fi
