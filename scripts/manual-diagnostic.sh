#!/bin/bash
################################################################################
# Manual Diagnostic Script - Find API Server
# Run this on VPS to manually locate the API server
################################################################################

echo "========================================="
echo "Manual API Server Diagnostic"
echo "========================================="
echo ""

# 1. Check all node processes
echo "[1] All Node.js processes:"
ps aux | grep node | grep -v grep

echo ""
echo "========================================="
echo ""

# 2. Check what's on port 3000
echo "[2] Port 3000 status:"
netstat -tulpn 2>/dev/null | grep ":3000" || echo "Nothing on port 3000"
ss -tulpn 2>/dev/null | grep ":3000" || echo "Nothing on port 3000 (ss)"

echo ""
echo "========================================="
echo ""

# 3. Search for index.js files
echo "[3] Searching for index.js in server directories:"
find /opt /home /var/www -name "index.js" -path "*/server/*" 2>/dev/null | head -20

echo ""
echo "========================================="
echo ""

# 4. Check PM2
echo "[4] PM2 status:"
if command -v pm2 &> /dev/null; then
    # Try different ways to activate PM2
    export NVM_DIR="$HOME/.nvm"
    [ -s "$NVM_DIR/nvm.sh" ] && \. "$NVM_DIR/nvm.sh"
    
    pm2 list 2>/dev/null || echo "PM2 not working in current shell"
else
    echo "PM2 command not found"
fi

echo ""
echo "========================================="
echo ""

# 5. Check Nginx config
echo "[5] Nginx proxy configuration:"
if [ -d /etc/nginx ]; then
    echo "Looking for proxy_pass directives:"
    grep -r "proxy_pass" /etc/nginx/ 2>/dev/null | grep -v "#"
else
    echo "Nginx config not found"
fi

echo ""
echo "========================================="
echo ""

# 6. Check systemd services
echo "[6] Systemd services related to API:"
systemctl list-units --type=service --all | grep -iE "node|api|zalo" || echo "No matching services"

echo ""
echo "========================================="
echo ""

# 7. Check common directories
echo "[7] Checking common directories:"
for dir in /opt/zaloclone-api /opt/zalo-api /home/zalo /var/www/api /opt/api; do
    if [ -d "$dir" ]; then
        echo "FOUND: $dir"
        ls -la "$dir" | head -10
    fi
done

echo ""
echo "========================================="
echo "Diagnostic Complete"
echo "========================================="
