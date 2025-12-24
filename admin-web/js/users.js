/**
 * User Management Logic for ZaloClone Admin
 * ==========================================
 */

// Global state
let users = [];
let currentUserId = null;
let unsubscribeUsers = null; // Store unsubscribe function for cleanup

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
    
    // Modal close
    document.getElementById('closeUserModal').addEventListener('click', closeModal);
    document.querySelector('#userModal .modal-overlay').addEventListener('click', closeModal);
    
    // Action buttons
    document.getElementById('resetPasswordBtn').addEventListener('click', resetUserPassword);
    document.getElementById('editUserBtn').addEventListener('click', toggleEditMode);
    document.getElementById('forceLogoutBtn').addEventListener('click', forceLogoutUser);
    document.getElementById('toggleBanBtn').addEventListener('click', toggleBan);
}

/**
 * Load and subscribe to users from Firestore (Real-time)
 */
async function loadUsers() {
    usersTableBody.innerHTML = `
        <tr>
            <td colspan="7" class="loading-cell">
                <i class="fas fa-spinner fa-spin"></i> Đang tải...
            </td>
        </tr>
    `;
    
    try {
        // Unsubscribe from previous listener if exists
        if (unsubscribeUsers) {
            unsubscribeUsers();
        }
        
        // Subscribe to real-time updates
        unsubscribeUsers = db.collection('users')
            .orderBy('createdAt', 'desc')
            .limit(100)
            .onSnapshot((snapshot) => {
                users = [];
                snapshot.forEach(doc => {
                    users.push({ id: doc.id, ...doc.data() });
                });
                
                renderUsers(users);
            }, (error) => {
                console.error('Error loading users:', error);
                usersTableBody.innerHTML = `
                    <tr>
                        <td colspan="6" class="loading-cell text-danger">
                            Lỗi: ${error.message}
                        </td>
                    </tr>
                `;
            });
        
    } catch (error) {
        console.error('Error setting up user listener:', error);
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
                <td colspan="7" class="loading-cell">
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
                            ? `<img src="${fixUrl(user.avatarUrl)}" alt="${escapeHtml(user.name)}" onerror="this.style.display='none'; this.parentNode.textContent='${initial}'">` 
                            : initial}
                    </div>
                    <div class="user-name">${escapeHtml(user.name || 'Chưa đặt tên')}</div>
                </div>
            </td>
            <td>${escapeHtml(user.email || 'N/A')}</td>
            <td>${escapeHtml(user.phone || 'N/A')}</td>
            <td>${status}</td>
            <td>${joinDate}</td>
            <td>${getLastSeenText(user)}</td>
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
    
    // Priority: Show online/offline status first
    // Check for STALE online status (e.g. app crashed and didn't update offline status)
    // If lastSeen is older than 5 minutes, consider offline even if isOnline=true
    const OFFLINE_THRESHOLD_MS = 5 * 60 * 1000; // 5 minutes
    const now = Date.now();
    const isStale = user.isOnline && user.lastSeen && (now - user.lastSeen > OFFLINE_THRESHOLD_MS);
    
    if (user.isOnline && !isStale) {
        return '<span class="status-badge status-online"><i class="fas fa-circle"></i> Online</span>';
    }
    
    // Offline - show time since last seen
    const lastSeenText = user.lastSeen ? ` - ${getRelativeTime(user.lastSeen)}` : '';
    const offlineBadge = `<span class="status-badge status-offline"><i class="fas fa-circle"></i> Offline${lastSeenText}</span>`;
    
    // Secondary badges for verified (only if offline)
    if (user.isVerified) {
        return offlineBadge + ' <span class="status-badge status-verified"><i class="fas fa-check-circle"></i> Verified</span>';
    }
    
    return offlineBadge;
}

/**
 * Get relative time string (e.g., "5 phút trước")
 */
function getRelativeTime(timestamp) {
    const now = Date.now();
    const diff = now - timestamp;
    
    const seconds = Math.floor(diff / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);
    
    if (seconds < 60) return 'vừa mới';
    if (minutes < 60) return `${minutes} phút trước`;
    if (hours < 24) return `${hours} giờ trước`;
    if (days < 7) return `${days} ngày trước`;
    
    return new Date(timestamp).toLocaleDateString('vi-VN');
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
            const OFFLINE_THRESHOLD_MS = 5 * 60 * 1000;
            const now = Date.now();
            const isStale = user.isOnline && user.lastSeen && (now - user.lastSeen > OFFLINE_THRESHOLD_MS);
            const effectiveOnline = user.isOnline && !isStale;

            switch (statusFilter) {
                case 'online': return effectiveOnline === true;
                case 'offline': return effectiveOnline !== true;
                case 'banned': return user.isBanned === true;
                default: return true;
            }
        });
    }
    
    renderUsers(filtered);
}

/**
 * Get last seen text
 */
function getLastSeenText(user) {
    const OFFLINE_THRESHOLD_MS = 5 * 60 * 1000; // 5 minutes
    const now = Date.now();
    const isStale = user.isOnline && user.lastSeen && (now - user.lastSeen > OFFLINE_THRESHOLD_MS);

    if (user.isOnline && !isStale) {
        return '<span class="text-success" style="font-weight: 500; font-size: 0.85rem;">Đang hoạt động</span>';
    }
    
    if (!user.lastSeen) {
        return '<span class="text-muted" style="font-size: 0.85rem;">Chưa hoạt động</span>';
    }
    
    // Format timestamp: HH:mm dd/MM/yyyy
    const date = new Date(user.lastSeen);
    const timeStr = date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
    const dateStr = date.toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric' });
    
    // Add relative time tooltip or sub-text
    const relative = getRelativeTime(user.lastSeen);
    
    return `
        <div style="display: flex; flex-direction: column;">
            <span style="font-weight: 500; font-size: 0.9rem;">${timeStr} ${dateStr}</span>
            <span class="text-muted" style="font-size: 0.75rem;">${relative}</span>
        </div>
    `;
}

/**
 * Open user detail modal
 */
function openUserModal(userId) {
    currentUserId = userId;
    const user = users.find(u => u.id === userId);
    
    if (!user) return;
    
    // Reset edit mode
    isEditMode = false;
    
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
    
    // Reset edit button
    const editBtn = document.getElementById('editUserBtn');
    if (editBtn) {
        editBtn.innerHTML = '<i class="fas fa-edit"></i> <span>Chỉnh sửa</span>';
        editBtn.title = '';
        editBtn.classList.remove('btn-success');
        editBtn.classList.add('btn-secondary');
    }
    
    // Remove cancel button if exists
    const cancelBtn = document.getElementById('cancelEditBtn');
    if (cancelBtn) {
        cancelBtn.remove();
    }
    
    userModal.classList.add('active');
}

/**
 * Update action button states
 */
function updateActionButtons(user) {
    const banBtn = document.getElementById('toggleBanBtn');
    
    if (user.isBanned) {
        banBtn.innerHTML = '<i class="fas fa-unlock"></i> <span>Mở khóa</span>';
        banBtn.title = '';
        banBtn.classList.remove('btn-warning');
        banBtn.classList.add('btn-success');
    } else {
        banBtn.innerHTML = '<i class="fas fa-ban"></i> <span>Khóa</span>';
        banBtn.title = '';
        banBtn.classList.remove('btn-success');
        banBtn.classList.add('btn-warning');
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
        const updateData = {
            isBanned: !user.isBanned,
            bannedAt: user.isBanned ? null : Date.now()
        };
        
        // Also force logout when banning
        if (!user.isBanned) {
            updateData.forceLogoutAt = Date.now();
        }
        
        await db.collection('users').doc(currentUserId).update(updateData);
        
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
 * Force logout user remotely
 */
async function forceLogoutUser() {
    if (!currentUserId) return;
    
    const user = users.find(u => u.id === currentUserId);
    if (!user) return;
    
    if (!confirm(`Bạn có chắc muốn đăng xuất ${user.name || user.email} khỏi tất cả thiết bị?`)) {
        return;
    }
    
    try {
        await db.collection('users').doc(currentUserId).update({
            forceLogoutAt: Date.now()
        });
        
        alert(`✅ Đã gửi lệnh đăng xuất đến ${user.name || user.email}`);
        
    } catch (error) {
        console.error('Error forcing logout:', error);
        alert('Lỗi: ' + error.message);
    }
}

/**
 * Reset user password via email
 */
async function resetUserPassword() {
    if (!currentUserId) return;
    
    const user = users.find(u => u.id === currentUserId);
    if (!user || !user.email) {
        alert('Không thể reset password: user không có email');
        return;
    }
    
    if (!confirm(`Gửi email reset password đến ${user.email}?`)) {
        return;
    }
    
    try {
        await auth.sendPasswordResetEmail(user.email);
        alert(`✅ Đã gửi email reset password đến ${user.email}`);
    } catch (error) {
        console.error('Error sending password reset email:', error);
        alert('Lỗi: ' + error.message);
    }
}

/**
 * Toggle edit mode for user details
 */
let isEditMode = false;

function toggleEditMode() {
    if (!currentUserId) return;
    
    const user = users.find(u => u.id === currentUserId);
    if (!user) return;
    
    const editBtn = document.getElementById('editUserBtn');
    const detailDiv = document.getElementById('userDetail');
    
    if (!isEditMode) {
        // Switch to edit mode
        isEditMode = true;
        editBtn.innerHTML = '<i class="fas fa-save"></i> <span>Lưu</span>';
        editBtn.title = '';
        editBtn.classList.remove('btn-secondary');
        editBtn.classList.add('btn-success');
        
        // Add cancel button
        const cancelBtn = document.createElement('button');
        cancelBtn.className = 'btn btn-secondary';
        cancelBtn.id = 'cancelEditBtn';
        cancelBtn.title = '';
        cancelBtn.innerHTML = '<i class="fas fa-times"></i> <span>Hủy</span>';
        cancelBtn.onclick = () => {
            isEditMode = false;
            openUserModal(currentUserId); // Reload modal
        };
        editBtn.parentNode.insertBefore(cancelBtn, editBtn);
        
        // Replace detail view with inputs
        detailDiv.innerHTML = `
            <div class="user-detail-grid">
                <div class="detail-row">
                    <span class="detail-label">ID</span>
                    <span class="detail-value">${user.id}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">Tên</span>
                    <input type="text" id="editName" class="edit-input" value="${escapeHtml(user.name || '')}" placeholder="Nhập tên">
                </div>
                <div class="detail-row">
                    <span class="detail-label">Email</span>
                    <span class="detail-value">${escapeHtml(user.email || 'N/A')}</span>
                </div>
                <div class="detail-row">
                    <span class="detail-label">Số điện thoại</span>
                    <input type="text" id="editPhone" class="edit-input" value="${escapeHtml(user.phone || '')}" placeholder="Nhập SĐT">
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
    } else {
        // Save changes
        saveUserChanges();
    }
}

/**
 * Save user changes to Firestore
 */
async function saveUserChanges() {
    if (!currentUserId) return;
    
    const user = users.find(u => u.id === currentUserId);
    if (!user) return;
    
    const newName = document.getElementById('editName').value.trim();
    const newPhone = document.getElementById('editPhone').value.trim();
    
    if (!newName) {
        alert('Tên không được để trống');
        return;
    }
    
    try {
        await db.collection('users').doc(currentUserId).update({
            name: newName,
            phone: newPhone || null
        });
        
        // Update local state
        user.name = newName;
        user.phone = newPhone;
        
        // Exit edit mode and reload modal
        isEditMode = false;
        openUserModal(currentUserId);
        
        alert('✅ Đã cập nhật thông tin user');
        
    } catch (error) {
        console.error('Error saving user changes:', error);
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
