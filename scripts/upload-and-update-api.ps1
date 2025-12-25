# PowerShell Script: Upload and Update API Server
# Purpose: Upload new index.js to VPS and trigger update
# Date: 2025-12-25

param(
    [string]$VpsIp = "163.61.182.20",
    [string]$VpsUser = "root",
    [switch]$SkipUpload,
    [switch]$Help
)

# Helper functions
function Write-ColorOutput {
    param(
        [string]$Message,
        [string]$Color = "White",
        [string]$Prefix = ""
    )
    if ($Prefix) {
        Write-Host "$Prefix " -NoNewline -ForegroundColor $Color
    }
    Write-Host $Message -ForegroundColor $Color
}

function Write-Success { 
    param([string]$Message)
    Write-ColorOutput -Message $Message -Color Green -Prefix "[OK]"
}

function Write-ErrorMsg { 
    param([string]$Message)
    Write-ColorOutput -Message $Message -Color Red -Prefix "[ERROR]"
}

function Write-WarningMsg { 
    param([string]$Message)
    Write-ColorOutput -Message $Message -Color Yellow -Prefix "[WARN]"
}

function Write-InfoMsg { 
    param([string]$Message)
    Write-ColorOutput -Message $Message -Color Cyan -Prefix "[INFO]"
}

function Write-Header {
    param([string]$Message)
    Write-Host ""
    Write-Host "=========================================" -ForegroundColor Blue
    Write-Host $Message -ForegroundColor Blue
    Write-Host "=========================================" -ForegroundColor Blue
}

if ($Help) {
    Write-Host @"
Usage: .\upload-and-update-api.ps1 [options]

Options:
  -VpsIp <ip>       VPS IP address (default: 163.61.182.20)
  -VpsUser <user>   VPS username (default: root)
  -SkipUpload       Skip file upload (for testing)
  -Help             Show this help message

Examples:
  .\upload-and-update-api.ps1
  .\upload-and-update-api.ps1 -VpsIp 163.61.182.20 -VpsUser root
  .\upload-and-update-api.ps1 -SkipUpload
"@
    exit 0
}

Write-Header "API Server Update Script"

# Check if running from correct directory
$expectedPath = "DoAn_ZaloClone"
$currentPath = Get-Location
if ($currentPath -notlike "*$expectedPath*") {
    Write-ErrorMsg "Please run this script from the DoAn_ZaloClone directory"
    Write-InfoMsg "Current directory: $currentPath"
    exit 1
}

# Find project root
$projectRoot = $currentPath
while ($projectRoot -and -not (Test-Path "$projectRoot\server\src\index.js")) {
    $projectRoot = Split-Path $projectRoot -Parent
}

if (-not $projectRoot) {
    Write-ErrorMsg "Could not find project root (looking for server/src/index.js)"
    exit 1
}

Write-Success "Project root: $projectRoot"

# Check if index.js exists
$indexJsPath = Join-Path $projectRoot "server\src\index.js"
if (-not (Test-Path $indexJsPath)) {
    Write-ErrorMsg "File not found: $indexJsPath"
    exit 1
}

Write-Success "Found index.js: $indexJsPath"

# Show file info
$fileInfo = Get-Item $indexJsPath
Write-InfoMsg "File size: $($fileInfo.Length) bytes"
Write-InfoMsg "Last modified: $($fileInfo.LastWriteTime)"

# Calculate MD5 hash
$md5 = New-Object System.Security.Cryptography.MD5CryptoServiceProvider
$hash = [System.BitConverter]::ToString($md5.ComputeHash([System.IO.File]::ReadAllBytes($indexJsPath)))
Write-InfoMsg "MD5: $($hash.Replace('-', '').ToLower())"

################################################################################
# STEP 1: Upload find-and-update-api.sh script to VPS
################################################################################

Write-Header "STEP 1: Upload Update Script"

$updateScriptPath = Join-Path $projectRoot "scripts\find-and-update-api.sh"

if (Test-Path $updateScriptPath) {
    Write-InfoMsg "Uploading find-and-update-api.sh to VPS..."
    
    try {
        scp $updateScriptPath "${VpsUser}@${VpsIp}:/tmp/find-and-update-api.sh"
        Write-Success "Update script uploaded"
        
        # Make it executable
        ssh "${VpsUser}@${VpsIp}" "chmod +x /tmp/find-and-update-api.sh"
        Write-Success "Script made executable"
    }
    catch {
        Write-ErrorMsg "Failed to upload update script: $_"
        exit 1
    }
} else {
    Write-WarningMsg "Update script not found at: $updateScriptPath"
    Write-InfoMsg "Continuing without it..."
}

################################################################################
# STEP 2: Run diagnostics on VPS
################################################################################

Write-Header "STEP 2: Run Diagnostics"

Write-InfoMsg "Running diagnostic script on VPS..."

try {
    ssh "${VpsUser}@${VpsIp}" "/tmp/find-and-update-api.sh"
    Write-Success "Diagnostics completed"
}
catch {
    Write-WarningMsg "Diagnostics failed, but continuing..."
}

################################################################################
# STEP 3: Upload new index.js
################################################################################

if (-not $SkipUpload) {
    Write-Header "STEP 3: Upload New index.js"
    
    Write-InfoMsg "Uploading $indexJsPath to VPS..."
    
    try {
        scp $indexJsPath "${VpsUser}@${VpsIp}:/tmp/index.js.new"
        Write-Success "File uploaded to /tmp/index.js.new"
    }
    catch {
        Write-ErrorMsg "Failed to upload file: $_"
        exit 1
    }
} else {
    Write-Header "STEP 3: Skip Upload"
    Write-WarningMsg "Skipping file upload (--SkipUpload flag)"
}

################################################################################
# STEP 4: Apply update on VPS
################################################################################

Write-Header "STEP 4: Apply Update"

Write-InfoMsg "Running update script with --apply flag..."

try {
    ssh "${VpsUser}@${VpsIp}" "/tmp/find-and-update-api.sh --apply"
    Write-Success "Update applied!"
}
catch {
    Write-ErrorMsg "Update failed: $_"
    
    Write-InfoMsg "Checking server status..."
    ssh "${VpsUser}@${VpsIp}" "pm2 status || true; ps aux | grep node | grep -v grep || true"
    
    exit 1
}

################################################################################
# STEP 5: Test API
################################################################################

Write-Header "STEP 5: Test API"

Write-InfoMsg "Testing API endpoints..."

# Test via public domain
$testUrls = @(
    "https://api.zolachat.site/health",
    "https://zolachat.site/api/health"
)

foreach ($url in $testUrls) {
    Write-InfoMsg "Testing: $url"
    
    try {
        $response = Invoke-RestMethod -Uri $url -TimeoutSec 10
        Write-Success "Response received!"
        $response | ConvertTo-Json -Depth 3
    }
    catch {
        Write-WarningMsg "Failed to reach $url"
    }
}

# Test SSH to localhost:3000
Write-InfoMsg "Testing via SSH tunnel..."
try {
    $result = ssh "${VpsUser}@${VpsIp}" "curl -s http://localhost:3000/health"
    Write-Success "Server responding on localhost:3000"
    $result
}
catch {
    Write-ErrorMsg "Server not responding on localhost:3000"
}

################################################################################
# STEP 6: Summary
################################################################################

Write-Header "Update Complete!"

Write-Success "API server has been updated with the new index.js file"
Write-InfoMsg "Check the improved /health endpoint:"
Write-InfoMsg "  https://api.zolachat.site/health"
Write-InfoMsg ""
Write-InfoMsg "To view logs:"
Write-InfoMsg "  ssh ${VpsUser}@${VpsIp}"
Write-InfoMsg "  pm2 logs zaloclone-api"

Write-Host ""
Write-Success "All done!"
