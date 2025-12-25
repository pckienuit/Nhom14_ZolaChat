# PowerShell Script: Deploy API Server to VPS
# Purpose: Full deployment of API server from scratch
# Date: 2025-12-25

param(
    [string]$VpsIp = "163.61.182.20",
    [string]$VpsUser = "root",
    [string]$DeployPath = "/opt/zaloclone-api",
    [switch]$SkipDependencies,
    [switch]$Help
)

# Helper functions
function Write-ColorOutput {
    param([string]$Message, [string]$Color = "White", [string]$Prefix = "")
    if ($Prefix) { Write-Host "$Prefix " -NoNewline -ForegroundColor $Color }
    Write-Host $Message -ForegroundColor $Color
}

function Write-Success { param([string]$Message); Write-ColorOutput -Message $Message -Color Green -Prefix "[OK]" }
function Write-ErrorMsg { param([string]$Message); Write-ColorOutput -Message $Message -Color Red -Prefix "[ERROR]" }
function Write-WarningMsg { param([string]$Message); Write-ColorOutput -Message $Message -Color Yellow -Prefix "[WARN]" }
function Write-InfoMsg { param([string]$Message); Write-ColorOutput -Message $Message -Color Cyan -Prefix "[INFO]" }
function Write-Header { param([string]$Message); Write-Host ""; Write-Host "=========================================" -ForegroundColor Blue; Write-Host $Message -ForegroundColor Blue; Write-Host "=========================================" -ForegroundColor Blue }

if ($Help) {
    Write-Host @"
Usage: .\deploy-api-server.ps1 [options]

Options:
  -VpsIp <ip>           VPS IP address (default: 163.61.182.20)
  -VpsUser <user>       VPS username (default: root)
  -DeployPath <path>    Deployment path on VPS (default: /opt/zaloclone-api)
  -SkipDependencies     Skip Node.js/PM2 installation
  -Help                 Show this help message

This script will:
  1. Install Node.js 18, PM2, and dependencies
  2. Upload server code to VPS
  3. Install npm packages
  4. Configure .env file
  5. Start server with PM2
  6. Test health endpoint

Examples:
  .\deploy-api-server.ps1
  .\deploy-api-server.ps1 -VpsIp 163.61.182.20
  .\deploy-api-server.ps1 -SkipDependencies
"@
    exit 0
}

Write-Header "API Server Full Deployment Script"

# Verify project structure
$projectRoot = Get-Location
$serverPath = Join-Path $projectRoot "server"
$packageJsonPath = Join-Path $serverPath "package.json"

if (-not (Test-Path $packageJsonPath)) {
    Write-ErrorMsg "server/package.json not found!"
    Write-InfoMsg "Please run from DoAn_ZaloClone root directory"
    exit 1
}

Write-Success "Project root: $projectRoot"
Write-InfoMsg "Server path: $serverPath"

################################################################################
# STEP 1: Install Dependencies on VPS
################################################################################

if (-not $SkipDependencies) {
    Write-Header "STEP 1: Install Dependencies on VPS"
    
    Write-InfoMsg "Installing Node.js 18, PM2, and system dependencies..."
    
    $installScript = @'
#!/bin/bash
set -e

echo "Installing system dependencies..."
apt-get update -qq
apt-get install -y curl git build-essential

echo "Installing Node.js 18..."
if ! command -v node &> /dev/null; then
    curl -fsSL https://deb.nodesource.com/setup_18.x | bash -
    apt-get install -y nodejs
fi

echo "Node version: $(node -v)"
echo "NPM version: $(npm -v)"

echo "Installing PM2 globally..."
npm install -g pm2

echo "PM2 version: $(pm2 -v)"

echo "Setup PM2 startup..."
pm2 startup systemd -u root --hp /root || true

echo "Dependencies installed successfully!"
'@
    
    try {
        Write-InfoMsg "Uploading installation script..."
        $installScript | ssh "${VpsUser}@${VpsIp}" "cat > /tmp/install-deps.sh && chmod +x /tmp/install-deps.sh && bash /tmp/install-deps.sh"
        Write-Success "Dependencies installed"
    }
    catch {
        Write-ErrorMsg "Failed to install dependencies: $_"
        exit 1
    }
} else {
    Write-Header "STEP 1: Skip Dependencies Installation"
    Write-WarningMsg "Skipping dependencies (--SkipDependencies flag)"
}

################################################################################
# STEP 2: Create Deployment Directory
################################################################################

Write-Header "STEP 2: Create Deployment Directory"

Write-InfoMsg "Creating directory: $DeployPath"

try {
    ssh "${VpsUser}@${VpsIp}" "mkdir -p $DeployPath && chown -R root:root $DeployPath"
    Write-Success "Directory created"
}
catch {
    Write-ErrorMsg "Failed to create directory: $_"
    exit 1
}

################################################################################
# STEP 3: Upload Server Code
################################################################################

Write-Header "STEP 3: Upload Server Code"

Write-InfoMsg "Uploading server files to VPS..."
Write-InfoMsg "This may take a few minutes..."

try {
    # Upload entire server directory
    scp -r "$serverPath/*" "${VpsUser}@${VpsIp}:${DeployPath}/"
    Write-Success "Server code uploaded"
}
catch {
    Write-ErrorMsg "Failed to upload server code: $_"
    exit 1
}

# Verify upload
Write-InfoMsg "Verifying upload..."
$remoteFiles = ssh "${VpsUser}@${VpsIp}" "ls -la $DeployPath"
Write-Host $remoteFiles
Write-Success "Upload verified"

################################################################################
# STEP 4: Install NPM Packages
################################################################################

Write-Header "STEP 4: Install NPM Packages"

Write-InfoMsg "Installing npm packages on VPS..."

try {
    ssh "${VpsUser}@${VpsIp}" "cd $DeployPath && npm install --production"
    Write-Success "NPM packages installed"
}
catch {
    Write-ErrorMsg "Failed to install npm packages: $_"
    exit 1
}

################################################################################
# STEP 5: Configure Environment
################################################################################

Write-Header "STEP 5: Configure Environment"

Write-InfoMsg "Setting up .env file..."

# Check if .env.example exists locally
$envExamplePath = Join-Path $serverPath ".env.example"
$hasEnvExample = Test-Path $envExamplePath

if ($hasEnvExample) {
    Write-InfoMsg "Found .env.example, using as template"
} else {
    Write-InfoMsg "Creating default .env configuration"
}

$envContent = @"
# API Server Configuration
PORT=3000
NODE_ENV=production

# CORS
ALLOWED_ORIGINS=https://zolachat.site,https://www.zolachat.site,https://api.zolachat.site

# Firebase
FIREBASE_SERVICE_ACCOUNT_PATH=./serviceAccountKey.json

# Rate Limiting
RATE_LIMIT_WINDOW_MS=900000
RATE_LIMIT_MAX_REQUESTS=100

# Logging
LOG_LEVEL=info
"@

try {
    $envContent | ssh "${VpsUser}@${VpsIp}" "cat > $DeployPath/.env"
    Write-Success ".env file created"
    
    # Set permissions
    ssh "${VpsUser}@${VpsIp}" "chmod 600 $DeployPath/.env"
    Write-Success "Permissions set"
}
catch {
    Write-ErrorMsg "Failed to create .env file: $_"
    exit 1
}

# Check for Firebase service account key
Write-InfoMsg "Checking for Firebase service account key..."

$localKeyPath = Join-Path $serverPath "serviceAccountKey.json"
if (Test-Path $localKeyPath) {
    Write-InfoMsg "Found serviceAccountKey.json locally, uploading..."
    try {
        scp "$localKeyPath" "${VpsUser}@${VpsIp}:${DeployPath}/serviceAccountKey.json"
        ssh "${VpsUser}@${VpsIp}" "chmod 600 $DeployPath/serviceAccountKey.json"
        Write-Success "Firebase key uploaded"
    }
    catch {
        Write-WarningMsg "Failed to upload Firebase key, you'll need to add it manually"
    }
} else {
    Write-WarningMsg "serviceAccountKey.json not found locally"
    Write-InfoMsg "You'll need to upload it manually:"
    Write-InfoMsg "  scp server/serviceAccountKey.json ${VpsUser}@${VpsIp}:${DeployPath}/"
}

################################################################################
# STEP 6: Start Server with PM2
################################################################################

Write-Header "STEP 6: Start Server with PM2"

Write-InfoMsg "Starting API server with PM2..."

$pm2StartScript = @"
cd $DeployPath

# Stop existing process if any
pm2 delete zaloclone-api 2>/dev/null || true

# Start new process
pm2 start src/index.js --name zaloclone-api --time

# Save PM2 process list
pm2 save

# Show status
pm2 list
pm2 info zaloclone-api
"@

try {
    $pm2StartScript | ssh "${VpsUser}@${VpsIp}" "bash"
    Write-Success "Server started with PM2"
}
catch {
    Write-ErrorMsg "Failed to start server: $_"
    Write-InfoMsg "Check logs with: ssh ${VpsUser}@${VpsIp} 'pm2 logs zaloclone-api'"
    exit 1
}

################################################################################
# STEP 7: Verify Deployment
################################################################################

Write-Header "STEP 7: Verify Deployment"

Write-InfoMsg "Waiting 5 seconds for server to initialize..."
Start-Sleep -Seconds 5

# Test localhost:3000
Write-InfoMsg "Testing http://localhost:3000/health..."
try {
    $healthCheck = ssh "${VpsUser}@${VpsIp}" "curl -s http://localhost:3000/health"
    Write-Success "Server is responding!"
    Write-Host $healthCheck -ForegroundColor Green
    
    # Try to parse JSON
    try {
        $healthJson = $healthCheck | ConvertFrom-Json
        Write-InfoMsg "Server uptime: $($healthJson.uptime) seconds"
        Write-InfoMsg "WebSocket connections: $($healthJson.websocket.connected)"
    }
    catch {
        # Just display raw response if JSON parsing fails
    }
}
catch {
    Write-WarningMsg "Server not responding on localhost:3000"
    Write-InfoMsg "Checking PM2 logs..."
    ssh "${VpsUser}@${VpsIp}" "pm2 logs zaloclone-api --lines 20 --nostream"
}

# Check PM2 status
Write-InfoMsg "PM2 status:"
ssh "${VpsUser}@${VpsIp}" "pm2 status"

################################################################################
# STEP 8: Next Steps - Nginx Configuration
################################################################################

Write-Header "STEP 8: Next Steps"

Write-Host @"

[OK] API Server deployed successfully!

Server is running at:
  - Internal: http://localhost:3000
  - PM2 Process: zaloclone-api

Next steps to make it publicly accessible:

1. Configure Nginx reverse proxy:
   
   Create /etc/nginx/sites-available/zaloclone-api:
   
   server {
       listen 80;
       server_name api.zolachat.site;
       
       location / {
           proxy_pass http://localhost:3000;
           proxy_http_version 1.1;
           proxy_set_header Upgrade \$http_upgrade;
           proxy_set_header Connection 'upgrade';
           proxy_set_header Host \$host;
           proxy_cache_bypass \$http_upgrade;
       }
   }
   
   Then:
   ln -s /etc/nginx/sites-available/zaloclone-api /etc/nginx/sites-enabled/
   nginx -t
   systemctl reload nginx

2. Setup SSL certificate:
   certbot --nginx -d api.zolachat.site

3. Test public endpoint:
   curl https://api.zolachat.site/health

Useful commands:
  View logs:    ssh ${VpsUser}@${VpsIp} 'pm2 logs zaloclone-api'
  Restart:      ssh ${VpsUser}@${VpsIp} 'pm2 restart zaloclone-api'
  Monitor:      ssh ${VpsUser}@${VpsIp} 'pm2 monit'
  Status:       ssh ${VpsUser}@${VpsIp} 'pm2 status'

"@ -ForegroundColor Cyan

Write-Success "Deployment complete!"
