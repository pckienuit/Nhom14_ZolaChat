#!/bin/bash

################################################################################
# Script: Find and Update API Server on VPS
# Purpose: Automatically locate API server path and update index.js
# Date: 2025-12-25
################################################################################

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Print colored message
print_msg() {
    local color=$1
    shift
    echo -e "${color}$@${NC}"
}

print_header() {
    echo ""
    print_msg "$BLUE" "========================================="
    print_msg "$BLUE" "$1"
    print_msg "$BLUE" "========================================="
}

print_success() { print_msg "$GREEN" "✓ $1"; }
print_error() { print_msg "$RED" "✗ $1"; }
print_warning() { print_msg "$YELLOW" "⚠ $1"; }
print_info() { print_msg "$BLUE" "ℹ $1"; }

################################################################################
# STEP 1: Environment Setup
################################################################################

print_header "STEP 1: Environment Setup"

# Load NVM if exists
if [ -f ~/.nvm/nvm.sh ]; then
    print_info "Loading NVM..."
    source ~/.nvm/nvm.sh
    print_success "NVM loaded"
fi

# Load bashrc
if [ -f ~/.bashrc ]; then
    source ~/.bashrc
fi

# Check node
if command -v node &> /dev/null; then
    print_success "Node.js found: $(node -v)"
else
    print_error "Node.js not found!"
    exit 1
fi

# Check pm2
if command -v pm2 &> /dev/null; then
    print_success "PM2 found: $(pm2 -v)"
else
    print_warning "PM2 not found"
fi

################################################################################
# STEP 2: Find API Server Location
################################################################################

print_header "STEP 2: Find API Server Location"

API_PATHS=(
    "/opt/zaloclone-api/server"
    "/opt/zalo-api/server"
    "/home/zalo/server"
    "/var/www/api"
)

API_SERVER_PATH=""

# Try known paths first
for path in "${API_PATHS[@]}"; do
    if [ -d "$path" ] && [ -f "$path/src/index.js" ]; then
        API_SERVER_PATH="$path"
        print_success "Found API server at: $API_SERVER_PATH"
        break
    fi
done

# If not found, search filesystem
if [ -z "$API_SERVER_PATH" ]; then
    print_info "Searching filesystem for API server..."
    
    # Find all index.js in server directories
    FOUND_PATHS=$(find /opt /home /var/www -name "index.js" -path "*/server/src/*" 2>/dev/null || true)
    
    if [ -n "$FOUND_PATHS" ]; then
        echo "$FOUND_PATHS" | while read -r path; do
            server_path=$(dirname $(dirname "$path"))
            print_info "Found potential server: $server_path"
        done
        
        # Take the first one
        FIRST_PATH=$(echo "$FOUND_PATHS" | head -1)
        API_SERVER_PATH=$(dirname $(dirname "$FIRST_PATH"))
        print_success "Using: $API_SERVER_PATH"
    else
        print_error "Could not find API server!"
        print_info "Please manually specify the path."
        exit 1
    fi
fi

INDEX_JS_PATH="$API_SERVER_PATH/src/index.js"
print_info "Target file: $INDEX_JS_PATH"

################################################################################
# STEP 3: Check PM2 Process
################################################################################

print_header "STEP 3: Check PM2 Process"

PM2_PROCESS_NAME=""

if command -v pm2 &> /dev/null; then
    # List all PM2 processes
    print_info "PM2 processes:"
    pm2 list
    
    # Find process name
    PROCESS_NAMES=$(pm2 jlist 2>/dev/null | grep -o '"name":"[^"]*"' | cut -d'"' -f4 || true)
    
    if [ -n "$PROCESS_NAMES" ]; then
        echo ""
        print_info "Found PM2 processes:"
        echo "$PROCESS_NAMES"
        
        # Try to find zaloclone-api or similar
        if echo "$PROCESS_NAMES" | grep -qi "zaloclone"; then
            PM2_PROCESS_NAME=$(echo "$PROCESS_NAMES" | grep -i "zaloclone" | head -1)
        elif echo "$PROCESS_NAMES" | grep -qi "api"; then
            PM2_PROCESS_NAME=$(echo "$PROCESS_NAMES" | grep -i "api" | head -1)
        else
            PM2_PROCESS_NAME=$(echo "$PROCESS_NAMES" | head -1)
        fi
        
        print_success "Using PM2 process: $PM2_PROCESS_NAME"
    else
        print_warning "No PM2 processes found"
    fi
else
    print_warning "PM2 not available"
fi

################################################################################
# STEP 4: Check Current Server Status
################################################################################

print_header "STEP 4: Check Current Server Status"

# Check if server is running
print_info "Checking if API server is running..."

# Try localhost:3000
if curl -s http://localhost:3000/health > /dev/null 2>&1; then
    print_success "Server is running on port 3000"
    curl -s http://localhost:3000/health | python3 -m json.tool 2>/dev/null || cat
else
    print_warning "Server not responding on port 3000"
fi

# Check what's listening on port 3000
print_info "Checking port 3000..."
PORT_INFO=$(netstat -tulpn 2>/dev/null | grep ":3000" || ss -tulpn 2>/dev/null | grep ":3000" || echo "No process on port 3000")
echo "$PORT_INFO"

################################################################################
# STEP 5: Backup Current File
################################################################################

print_header "STEP 5: Backup Current File"

if [ -f "$INDEX_JS_PATH" ]; then
    BACKUP_DIR="$API_SERVER_PATH/backups"
    mkdir -p "$BACKUP_DIR"
    
    BACKUP_FILE="$BACKUP_DIR/index.js.backup.$(date +%Y%m%d_%H%M%S)"
    cp "$INDEX_JS_PATH" "$BACKUP_FILE"
    
    print_success "Backed up to: $BACKUP_FILE"
    
    # Show file info
    print_info "Current file info:"
    ls -lh "$INDEX_JS_PATH"
    print_info "MD5: $(md5sum "$INDEX_JS_PATH" | cut -d' ' -f1)"
else
    print_error "File not found: $INDEX_JS_PATH"
    exit 1
fi

################################################################################
# STEP 6: Show Update Instructions
################################################################################

print_header "STEP 6: Update Instructions"

cat << 'EOF'

The new index.js file needs to be uploaded from your local machine.

From your LOCAL machine (PowerShell), run:

  scp server/src/index.js root@163.61.182.20:/tmp/index.js.new

Then on the VPS, run this script again with the --apply flag:

  ./find-and-update-api.sh --apply

OR manually move the file:
EOF

echo ""
print_info "mv /tmp/index.js.new $INDEX_JS_PATH"

################################################################################
# STEP 7: Apply Update (if --apply flag provided)
################################################################################

if [ "$1" == "--apply" ]; then
    print_header "STEP 7: Apply Update"
    
    if [ -f "/tmp/index.js.new" ]; then
        print_info "Moving new file to $INDEX_JS_PATH"
        mv /tmp/index.js.new "$INDEX_JS_PATH"
        chmod 644 "$INDEX_JS_PATH"
        print_success "File updated!"
        
        print_info "New file MD5: $(md5sum "$INDEX_JS_PATH" | cut -d' ' -f1)"
    else
        print_error "File /tmp/index.js.new not found!"
        print_info "Please upload it first using scp from your local machine."
        exit 1
    fi
    
    ############################################################################
    # STEP 8: Restart Server
    ############################################################################
    
    print_header "STEP 8: Restart Server"
    
    if [ -n "$PM2_PROCESS_NAME" ]; then
        print_info "Restarting PM2 process: $PM2_PROCESS_NAME"
        pm2 restart "$PM2_PROCESS_NAME"
        
        sleep 3
        
        print_info "Process status:"
        pm2 status "$PM2_PROCESS_NAME"
        
        print_success "Server restarted!"
    else
        print_warning "No PM2 process found. You may need to restart manually:"
        print_info "cd $API_SERVER_PATH && pm2 restart zaloclone-api"
        print_info "OR: systemctl restart your-api-service"
    fi
    
    ############################################################################
    # STEP 9: Verify Update
    ############################################################################
    
    print_header "STEP 9: Verify Update"
    
    print_info "Waiting 5 seconds for server to start..."
    sleep 5
    
    print_info "Testing /health endpoint..."
    
    if curl -s http://localhost:3000/health > /dev/null 2>&1; then
        print_success "Server is responding!"
        echo ""
        curl -s http://localhost:3000/health | python3 -m json.tool 2>/dev/null || curl -s http://localhost:3000/health
    else
        print_error "Server not responding!"
        print_info "Check logs:"
        
        if [ -n "$PM2_PROCESS_NAME" ]; then
            echo "  pm2 logs $PM2_PROCESS_NAME --lines 50"
        fi
        
        echo "  tail -f $API_SERVER_PATH/logs/*.log"
    fi
    
    print_header "Update Complete!"
    
else
    print_header "Summary"
    
    cat << EOF

API Server Path: $API_SERVER_PATH
Index.js Path:   $INDEX_JS_PATH
PM2 Process:     ${PM2_PROCESS_NAME:-"Not found"}
Backup Created:  $BACKUP_FILE

Next steps:
1. Upload new index.js from local machine:
   scp server/src/index.js root@163.61.182.20:/tmp/index.js.new

2. Run this script with --apply flag:
   ./find-and-update-api.sh --apply

EOF
fi

################################################################################
# STEP 10: Additional Diagnostics (if no --apply)
################################################################################

if [ "$1" != "--apply" ]; then
    print_header "Additional Diagnostics"
    
    # Check Nginx config
    print_info "Checking Nginx configuration..."
    if [ -f /etc/nginx/sites-enabled/zaloclone-api ]; then
        print_success "Found Nginx config: /etc/nginx/sites-enabled/zaloclone-api"
        grep -E "proxy_pass|server_name|listen" /etc/nginx/sites-enabled/zaloclone-api | head -10
    else
        print_warning "Nginx config not found at expected location"
        print_info "Searching for proxy configs..."
        grep -r "proxy_pass.*3000" /etc/nginx/ 2>/dev/null | head -5 || true
    fi
    
    # Check all Node processes
    echo ""
    print_info "All Node.js processes:"
    ps aux | grep node | grep -v grep || echo "No node processes found"
    
    # Check environment file
    echo ""
    if [ -f "$API_SERVER_PATH/.env" ]; then
        print_success "Found .env file"
        print_info "PORT setting:"
        grep "^PORT=" "$API_SERVER_PATH/.env" || echo "PORT not set"
    else
        print_warning ".env file not found"
    fi
fi

print_header "Script Completed"
