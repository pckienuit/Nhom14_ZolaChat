// API Monitor JavaScript
const API_BASE_URL = 'https://zolachat.site';

const endpoints = [
    { name: 'Authentication', path: '/api/auth', method: 'POST', description: 'User login & registration' },
    { name: 'Users', path: '/api/users', method: 'GET', description: 'User management' },
    { name: 'Friends', path: '/api/friends', method: 'GET', description: 'Friend requests & contacts' },
    { name: 'Conversations', path: '/api/conversations', method: 'GET', description: 'Chat conversations' },
    { name: 'Messages', path: '/api/messages', method: 'POST', description: 'Send & receive messages' },
    { name: 'Chats', path: '/api/chats', method: 'GET', description: 'Chat history' },
    { name: 'Calls', path: '/api/calls', method: 'POST', description: 'WebRTC calls' },
    { name: 'Stickers', path: '/api/stickers', method: 'GET', description: 'Sticker packs' }
];

let healthData = null;

// Format uptime
function formatUptime(seconds) {
    const days = Math.floor(seconds / 86400);
    const hours = Math.floor((seconds % 86400) / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    
    if (days > 0) return `${days}d ${hours}h`;
    if (hours > 0) return `${hours}h ${minutes}m`;
    return `${minutes}m`;
}

// Fetch health status
async function fetchHealthStatus() {
    try {
        const response = await fetch(`${API_BASE_URL}/health`);
        
        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }
        
        healthData = await response.json();
        updateUI();
        
    } catch (error) {
        console.error('Health check failed:', error);
        showOfflineStatus(error.message);
    }
}

// Update UI with health data
function updateUI() {
    if (!healthData) return;
    
    // Status indicator
    const indicator = document.getElementById('statusIndicator');
    const statusText = document.getElementById('statusText');
    
    if (healthData.status === 'ok') {
        indicator.className = 'status-indicator online';
        statusText.textContent = 'API Server Online';
        statusText.style.color = '#10b981';
    }
    
    // Metrics
    document.getElementById('uptime').textContent = formatUptime(healthData.uptime);
    document.getElementById('wsClients').textContent = healthData.websocket?.connected || 0;
    document.getElementById('memory').textContent = healthData.memory?.heapUsed || '-';
    document.getElementById('version').textContent = healthData.version || '1.0.0';
    
    // Endpoints list
    renderEndpoints();
    
    // Raw response
    document.getElementById('rawResponse').textContent = JSON.stringify(healthData, null, 2);
}

// Show offline status
function showOfflineStatus(error) {
    const indicator = document.getElementById('statusIndicator');
    const statusText = document.getElementById('statusText');
    
    indicator.className = 'status-indicator offline';
    statusText.textContent = 'API Server Offline';
    statusText.style.color = '#ef4444';
    
    document.getElementById('uptime').textContent = '-';
    document.getElementById('wsClients').textContent = '-';
    document.getElementById('memory').textContent = '-';
    document.getElementById('version').textContent = '-';
    
    document.getElementById('rawResponse').textContent = `Error: ${error}`;
}

// Render endpoints
function renderEndpoints() {
    const container = document.getElementById('endpointList');
    
    container.innerHTML = endpoints.map(endpoint => `
        <div class="endpoint-item">
            <div class="endpoint-info">
                <h4>${endpoint.name}</h4>
                <p>${endpoint.path} - ${endpoint.description}</p>
            </div>
            <span class="endpoint-badge ${endpoint.method.toLowerCase()}">${endpoint.method}</span>
        </div>
    `).join('');
}

// Refresh status
function refreshStatus() {
    const btn = document.querySelector('.refresh-btn i');
    btn.style.animation = 'spin 1s linear';
    
    fetchHealthStatus();
    
    setTimeout(() => {
        btn.style.animation = '';
    }, 1000);
}

// Auto-refresh every 30 seconds
setInterval(fetchHealthStatus, 30000);

// Initial load
document.addEventListener('DOMContentLoaded', () => {
    fetchHealthStatus();
});

// Spin animation
const style = document.createElement('style');
style.textContent = `
    @keyframes spin {
        from { transform: rotate(0deg); }
        to { transform: rotate(360deg); }
    }
`;
document.head.appendChild(style);
