/**
 * Dashboard Logic for ZaloClone Admin
 * ====================================
 * 
 * Handles dashboard statistics and recent activity.
 */

// Wait for DOM to be ready
document.addEventListener('DOMContentLoaded', () => {
    // Load dashboard data when auth is ready
    auth.onAuthStateChanged((user) => {
        if (user) {
            loadDashboardStats();
            loadTopStickerPacks();
            loadRecentUsers();
        }
    });
});

/**
 * Load dashboard statistics
 */
async function loadDashboardStats() {
    try {
        // Get total users count
        const usersSnapshot = await db.collection('users').get();
        document.getElementById('totalUsers').textContent = formatNumber(usersSnapshot.size);
        
        // Get total sticker packs count
        const stickerPacksSnapshot = await db.collection('stickerPacks').get();
        document.getElementById('totalStickerPacks').textContent = formatNumber(stickerPacksSnapshot.size);
        
        // Calculate total downloads
        let totalDownloads = 0;
        stickerPacksSnapshot.forEach(doc => {
            totalDownloads += doc.data().downloadCount || 0;
        });
        document.getElementById('totalDownloads').textContent = formatNumber(totalDownloads);
        
        // Get total conversations count
        const conversationsSnapshot = await db.collection('conversations').get();
        document.getElementById('totalConversations').textContent = formatNumber(conversationsSnapshot.size);
        
    } catch (error) {
        console.error('Error loading dashboard stats:', error);
    }
}

/**
 * Load top sticker packs by download count
 */
async function loadTopStickerPacks() {
    const listElement = document.getElementById('topStickersList');
    
    try {
        const snapshot = await db.collection('stickerPacks')
            .orderBy('downloadCount', 'desc')
            .limit(5)
            .get();
        
        if (snapshot.empty) {
            listElement.innerHTML = '<li class="loading">Chưa có sticker pack nào</li>';
            return;
        }
        
        listElement.innerHTML = '';
        
        snapshot.forEach(doc => {
            const pack = doc.data();
            const li = document.createElement('li');
            li.innerHTML = `
                <div class="item-info">
                    <img class="item-icon" src="${fixUrl(pack.iconUrl) || 'assets/images/default-sticker.png'}" 
                         alt="${pack.name}" onerror="this.src='assets/images/default-sticker.png'">
                    <div>
                        <div class="item-name">${escapeHtml(pack.name)}</div>
                        <div class="item-stat">${pack.stickerCount || 0} stickers</div>
                    </div>
                </div>
                <div class="item-value">${formatNumber(pack.downloadCount || 0)} <i class="fas fa-download"></i></div>
            `;
            listElement.appendChild(li);
        });
        
    } catch (error) {
        console.error('Error loading top sticker packs:', error);
        listElement.innerHTML = '<li class="loading text-danger">Lỗi tải dữ liệu</li>';
    }
}

/**
 * Load recent users
 */
async function loadRecentUsers() {
    const listElement = document.getElementById('recentUsersList');
    
    try {
        const snapshot = await db.collection('users')
            .orderBy('createdAt', 'desc')
            .limit(5)
            .get();
        
        if (snapshot.empty) {
            listElement.innerHTML = '<li class="loading">Chưa có người dùng nào</li>';
            return;
        }
        
        listElement.innerHTML = '';
        
        snapshot.forEach(doc => {
            const user = doc.data();
            const li = document.createElement('li');
            const joinDate = user.createdAt ? new Date(user.createdAt).toLocaleDateString('vi-VN') : 'N/A';
            
            li.innerHTML = `
                <div class="item-info">
                    <div class="item-icon" style="display: flex; align-items: center; justify-content: center; background: var(--primary);">
                        <i class="fas fa-user" style="color: white;"></i>
                    </div>
                    <div>
                        <div class="item-name">${escapeHtml(user.name || 'Unnamed')}</div>
                        <div class="item-stat">${escapeHtml(user.email || '')}</div>
                    </div>
                </div>
                <div class="item-value" style="font-size: 0.75rem; color: var(--text-muted);">${joinDate}</div>
            `;
            listElement.appendChild(li);
        });
        
    } catch (error) {
        console.error('Error loading recent users:', error);
        listElement.innerHTML = '<li class="loading text-danger">Lỗi tải dữ liệu</li>';
    }
}

/**
 * Format number with thousand separators
 */
function formatNumber(num) {
    if (num >= 1000000) {
        return (num / 1000000).toFixed(1) + 'M';
    } else if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K';
    }
    return num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
}

/**
 * Escape HTML to prevent XSS
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
