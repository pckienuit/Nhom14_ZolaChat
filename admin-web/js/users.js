/**
 * User Management Logic for ZaloClone Admin
 * ==========================================
 */

// Global state
let users = [];
let currentUserId = null;

// DOM Elements
const usersTableBody = document.getElementById('usersTableBody');
const searchInput = document.getElementById('searchInput');
const filterStatus = document.getElementById('filterStatus');
const userModal = document.getElementById('userModal');

// Wait for DOM and Auth
document.addEventListener('DOMContentLoaded', () => {
    auth.onAuthStateChanged((user) => {
        if (user) {
            loadUsers();
            setupEventListeners();
        }
    });
});

/**
 * Setup event listeners
 */
function setupEventListeners() {
    // Search and filter
    searchInput.addEventListener('input', debounce(filterUsers, 300));
    filterStatus.addEventListener('change', filterUsers);
    
    // Modal
    document.getElementById('closeUserModal').addEventListener('click', closeModal);
    document.getElementById('closeUserBtn').addEventListener('click', closeModal);
    document.querySelector('#userModal .modal-overlay').addEventListener('click', closeModal);
    
    // Actions
    document.getElementById('toggleBanBtn').addEventListener('click', toggleBan);
    document.getElementById('toggleVerifyBtn').addEventListener('click', toggleVerify);
}

/**
 * Load users from Firestore
 */
async function loadUsers() {
    usersTableBody.innerHTML = `
        <tr>
            <td colspan="6" class="loading-cell">
                <i class="fas fa-spinner fa-spin"></i> Đang tải...
            </td>
        </tr>
    `;
    
    try {
        const snapshot = await db.collection('users')
            .orderBy('createdAt', 'desc')
            .limit(100)
            .get();
        
        users = [];
        snapshot.forEach(doc => {
            users.push({ id: doc.id, ...doc.data() });
        });
        
        renderUsers(users);
        
    } catch (error) {
        console.error('Error loading users:', error);
        usersTableBody.innerHTML = `
            <tr>
                <td colspan="6" class="loading-cell text-danger">
                    Lỗi: ${error.message}
                </td>
            </tr>
        `;
    }
}

/**
 * Render users table
 */
function renderUsers(usersList) {
    if (usersList.length === 0) {
        usersTableBody.innerHTML = `
            <tr>
                <td colspan="6" class="loading-cell">
                    Không tìm thấy người dùng nào
                </td>
            </tr>
        `;
        return;
    }
    
    usersTableBody.innerHTML = usersList.map(user => createUserRow(user)).join('');
    
    // Attach click handlers
    document.querySelectorAll('.view-user-btn').forEach(btn => {
        btn.addEventListener('click', () => openUserModal(btn.dataset.id));
    });
}

/**
 * Create user table row
 */
function createUserRow(user) {
    const status = getStatusBadge(user);
    const joinDate = user.createdAt ? new Date(user.createdAt).toLocaleDateString('vi-VN') : 'N/A';
    const initial = (user.name || user.email || 'U')[0].toUpperCase();
    
    return `
        <tr>
            <td>
                <div class="user-cell">
                    <div class="user-avatar">
                        ${user.avatarUrl 
                            ? `<img src="${user.avatarUrl}" alt="${escapeHtml(user.name)}" onerror="this.style.display='none'; this.parentNode.textContent='${initial}'">` 
                            : initial}
                    </div>
                    <div class="user-name">${escapeHtml(user.name || 'Chưa đặt tên')}</div>
                </div>
            </td>
            <td>${escapeHtml(user.email || 'N/A')}</td>
            <td>${escapeHtml(user.phone || 'N/A')}</td>
            <td>${status}</td>
            <td>${joinDate}</td>
            <td>
                <div class="action-btns">
                    <button class="btn btn-icon view-user-btn" data-id="${user.id}" title="Xem chi tiết">
                        <i class="fas fa-eye"></i>
                    </button>
                </div>
            </td>
        </tr>
    `;
}

/**
 * Get status badge HTML
 */
function getStatusBadge(user) {
    if (user.isBanned) {
        return '<span class="status-badge status-banned"><i class="fas fa-ban"></i> Bị khóa</span>';
    }
    if (user.isVerified) {
        return '<span class="status-badge status-verified"><i class="fas fa-check-circle"></i> Đã xác minh</span>';
    }
    if (user.isOnline) {
        return '<span class="status-badge status-online"><i class="fas fa-circle"></i> Online</span>';
    }
    return '<span class="status-badge status-offline"><i class="fas fa-circle"></i> Offline</span>';
}

/**
 * Filter users
 */
function filterUsers() {
    const searchTerm = searchInput.value.toLowerCase().trim();
    const statusFilter = filterStatus.value;
    
    let filtered = users;
    
    // Search filter
    if (searchTerm) {
        filtered = filtered.filter(user => 
            (user.name && user.name.toLowerCase().includes(searchTerm)) ||
            (user.email && user.email.toLowerCase().includes(searchTerm)) ||
            (user.phone && user.phone.includes(searchTerm))
        );
    }
    
    // Status filter
    if (statusFilter !== 'all') {
        filtered = filtered.filter(user => {
            switch (statusFilter) {
                case 'online': return user.isOnline === true;
                case 'offline': return user.isOnline !== true;
                case 'banned': return user.isBanned === true;
                case 'verified': return user.isVerified === true;
                default: return true;
            }
        });
    }
    
    renderUsers(filtered);
}

/**
 * Open user detail modal
 */
function openUserModal(userId) {
    currentUserId = userId;
    const user = users.find(u => u.id === userId);
    
    if (!user) return;
    
    const detail = document.getElementById('userDetail');
    detail.innerHTML = `
        <div class="user-detail-grid">
            <div class="detail-row">
                <span class="detail-label">ID</span>
                <span class="detail-value">${user.id}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">Tên</span>
                <span class="detail-value">${escapeHtml(user.name || 'Chưa đặt tên')}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">Email</span>
                <span class="detail-value">${escapeHtml(user.email || 'N/A')}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">Số điện thoại</span>
                <span class="detail-value">${escapeHtml(user.phone || 'N/A')}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">Trạng thái</span>
                <span class="detail-value">${getStatusBadge(user)}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">Ngày tham gia</span>
                <span class="detail-value">${user.createdAt ? new Date(user.createdAt).toLocaleString('vi-VN') : 'N/A'}</span>
            </div>
            <div class="detail-row">
                <span class="detail-label">Hoạt động cuối</span>
                <span class="detail-value">${user.lastSeen ? new Date(user.lastSeen).toLocaleString('vi-VN') : 'N/A'}</span>
            </div>
        </div>
    `;
    
    // Update button states
    updateActionButtons(user);
    
    userModal.classList.add('active');
}

/**
 * Update action button states
 */
function updateActionButtons(user) {
    const banBtn = document.getElementById('toggleBanBtn');
    const verifyBtn = document.getElementById('toggleVerifyBtn');
    
    if (user.isBanned) {
        banBtn.innerHTML = '<i class="fas fa-unlock"></i> <span>Mở khóa</span>';
        banBtn.classList.remove('btn-warning');
        banBtn.classList.add('btn-success');
    } else {
        banBtn.innerHTML = '<i class="fas fa-ban"></i> <span>Khóa User</span>';
        banBtn.classList.remove('btn-success');
        banBtn.classList.add('btn-warning');
    }
    
    if (user.isVerified) {
        verifyBtn.innerHTML = '<i class="fas fa-times-circle"></i> <span>Bỏ xác minh</span>';
        verifyBtn.classList.remove('btn-primary');
        verifyBtn.classList.add('btn-secondary');
    } else {
        verifyBtn.innerHTML = '<i class="fas fa-check-circle"></i> <span>Xác minh</span>';
        verifyBtn.classList.remove('btn-secondary');
        verifyBtn.classList.add('btn-primary');
    }
}

/**
 * Close modal
 */
function closeModal() {
    userModal.classList.remove('active');
    currentUserId = null;
}

/**
 * Toggle ban status
 */
async function toggleBan() {
    if (!currentUserId) return;
    
    const user = users.find(u => u.id === currentUserId);
    if (!user) return;
    
    try {
        await db.collection('users').doc(currentUserId).update({
            isBanned: !user.isBanned,
            bannedAt: user.isBanned ? null : Date.now()
        });
        
        // Update local state
        user.isBanned = !user.isBanned;
        updateActionButtons(user);
        renderUsers(users);
        
    } catch (error) {
        console.error('Error toggling ban:', error);
        alert('Lỗi: ' + error.message);
    }
}

/**
 * Toggle verify status
 */
async function toggleVerify() {
    if (!currentUserId) return;
    
    const user = users.find(u => u.id === currentUserId);
    if (!user) return;
    
    try {
        await db.collection('users').doc(currentUserId).update({
            isVerified: !user.isVerified,
            verifiedAt: user.isVerified ? null : Date.now()
        });
        
        // Update local state
        user.isVerified = !user.isVerified;
        updateActionButtons(user);
        renderUsers(users);
        
    } catch (error) {
        console.error('Error toggling verify:', error);
        alert('Lỗi: ' + error.message);
    }
}

/**
 * Utilities
 */
function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}
