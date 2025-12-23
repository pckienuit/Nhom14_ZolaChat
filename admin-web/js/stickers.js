/**
 * Sticker Management Logic for ZaloClone Admin
 * ==============================================
 * 
 * Handles CRUD operations for sticker packs.
 */

// Global state
let stickerPacks = [];
let currentPackId = null;
let deletePackId = null;

// DOM Elements
const stickerPacksGrid = document.getElementById('stickerPacksGrid');
const searchInput = document.getElementById('searchInput');
const filterType = document.getElementById('filterType');
const addPackBtn = document.getElementById('addPackBtn');
const packModal = document.getElementById('packModal');
const deleteModal = document.getElementById('deleteModal');
const packForm = document.getElementById('packForm');

// Wait for DOM and Auth to be ready
document.addEventListener('DOMContentLoaded', () => {
    auth.onAuthStateChanged((user) => {
        if (user) {
            loadStickerPacks();
            setupEventListeners();
        }
    });
});

/**
 * Setup event listeners
 */
function setupEventListeners() {
    // Add pack button
    addPackBtn.addEventListener('click', () => openModal());
    
    // Close modal buttons
    document.getElementById('closeModal').addEventListener('click', closeModal);
    document.getElementById('cancelBtn').addEventListener('click', closeModal);
    document.querySelector('#packModal .modal-overlay').addEventListener('click', closeModal);
    
    // Delete modal
    document.getElementById('closeDeleteModal').addEventListener('click', closeDeleteModal);
    document.getElementById('cancelDeleteBtn').addEventListener('click', closeDeleteModal);
    document.querySelector('#deleteModal .modal-overlay').addEventListener('click', closeDeleteModal);
    document.getElementById('confirmDeleteBtn').addEventListener('click', confirmDelete);
    
    // Form submission
    packForm.addEventListener('submit', handleFormSubmit);
    
    // Search and filter
    searchInput.addEventListener('input', debounce(filterPacks, 300));
    filterType.addEventListener('change', filterPacks);
    
    // Free checkbox toggles price
    document.getElementById('isFree').addEventListener('change', (e) => {
        document.getElementById('packPrice').disabled = e.target.checked;
        if (e.target.checked) {
            document.getElementById('packPrice').value = 0;
        }
    });
}

/**
 * Load sticker packs from Firestore
 */
async function loadStickerPacks() {
    stickerPacksGrid.innerHTML = `
        <div class="loading-placeholder">
            <i class="fas fa-spinner fa-spin"></i>
            <p>Đang tải sticker packs...</p>
        </div>
    `;
    
    try {
        const snapshot = await db.collection('stickerPacks')
            .orderBy('createdAt', 'desc')
            .get();
        
        stickerPacks = [];
        snapshot.forEach(doc => {
            stickerPacks.push({ id: doc.id, ...doc.data() });
        });
        
        renderPacks(stickerPacks);
        
    } catch (error) {
        console.error('Error loading sticker packs:', error);
        stickerPacksGrid.innerHTML = `
            <div class="loading-placeholder">
                <i class="fas fa-exclamation-triangle"></i>
                <p>Lỗi tải dữ liệu: ${error.message}</p>
            </div>
        `;
    }
}

/**
 * Render sticker packs grid
 */
function renderPacks(packs) {
    if (packs.length === 0) {
        stickerPacksGrid.innerHTML = `
            <div class="empty-state">
                <i class="fas fa-face-smile"></i>
                <h3>Chưa có sticker pack nào</h3>
                <p>Nhấn "Thêm Pack mới" để tạo sticker pack đầu tiên</p>
            </div>
        `;
        return;
    }
    
    stickerPacksGrid.innerHTML = packs.map(pack => createPackCard(pack)).join('');
    
    // Attach event listeners to cards
    document.querySelectorAll('.edit-pack-btn').forEach(btn => {
        btn.addEventListener('click', () => openModal(btn.dataset.id));
    });
    
    document.querySelectorAll('.delete-pack-btn').forEach(btn => {
        btn.addEventListener('click', () => openDeleteModal(btn.dataset.id, btn.dataset.name));
    });
    
    document.querySelectorAll('.toggle-publish-btn').forEach(btn => {
        btn.addEventListener('click', () => togglePublish(btn.dataset.id, btn.dataset.published === 'true'));
    });
}

/**
 * Create pack card HTML
 */
function createPackCard(pack) {
    const badges = [];
    
    // Type badge
    if (pack.type === 'official') {
        badges.push('<span class="badge badge-official">Official</span>');
    } else if (pack.type === 'trending') {
        badges.push('<span class="badge badge-trending">Trending</span>');
    } else {
        badges.push('<span class="badge badge-user">User</span>');
    }
    
    // Featured badge
    if (pack.isFeatured) {
        badges.push('<span class="badge badge-featured">Nổi bật</span>');
    }
    
    // Hidden badge
    if (!pack.isPublished) {
        badges.push('<span class="badge badge-hidden">Ẩn</span>');
    }
    
    // Price badge
    if (pack.isFree) {
        badges.push('<span class="badge badge-free">Miễn phí</span>');
    } else {
        badges.push(`<span class="badge badge-paid">${formatPrice(pack.price)}đ</span>`);
    }
    
    return `
        <div class="pack-card" data-id="${pack.id}">
            <div class="pack-card-header">
                <img class="pack-icon" src="${pack.iconUrl || 'assets/images/default-sticker.png'}" 
                     alt="${escapeHtml(pack.name)}" 
                     onerror="this.src='assets/images/default-sticker.png'">
                <div class="pack-info">
                    <div class="pack-name">${escapeHtml(pack.name)}</div>
                    <div class="pack-description">${escapeHtml(pack.description || 'Không có mô tả')}</div>
                    <div class="pack-badges">${badges.join('')}</div>
                </div>
            </div>
            <div class="pack-card-body">
                <div class="pack-stats">
                    <div class="pack-stat">
                        <span class="pack-stat-value">${pack.stickerCount || 0}</span>
                        <span class="pack-stat-label">Stickers</span>
                    </div>
                    <div class="pack-stat">
                        <span class="pack-stat-value">${formatNumber(pack.downloadCount || 0)}</span>
                        <span class="pack-stat-label">Downloads</span>
                    </div>
                </div>
            </div>
            <div class="pack-card-footer">
                <button class="btn btn-icon toggle-publish-btn" 
                        data-id="${pack.id}" 
                        data-published="${pack.isPublished}"
                        title="${pack.isPublished ? 'Ẩn pack' : 'Hiện pack'}">
                    <i class="fas ${pack.isPublished ? 'fa-eye-slash' : 'fa-eye'}"></i>
                </button>
                <button class="btn btn-icon edit-pack-btn" data-id="${pack.id}" title="Chỉnh sửa">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="btn btn-icon danger delete-pack-btn" 
                        data-id="${pack.id}" 
                        data-name="${escapeHtml(pack.name)}" 
                        title="Xóa">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        </div>
    `;
}

/**
 * Filter packs based on search and type
 */
function filterPacks() {
    const searchTerm = searchInput.value.toLowerCase().trim();
    const filterValue = filterType.value;
    
    let filtered = stickerPacks;
    
    // Filter by search term
    if (searchTerm) {
        filtered = filtered.filter(pack => 
            pack.name.toLowerCase().includes(searchTerm) ||
            (pack.description && pack.description.toLowerCase().includes(searchTerm))
        );
    }
    
    // Filter by type
    if (filterValue !== 'all') {
        filtered = filtered.filter(pack => pack.type === filterValue);
    }
    
    renderPacks(filtered);
}

/**
 * Open add/edit modal
 */
function openModal(packId = null) {
    currentPackId = packId;
    
    // Reset form
    packForm.reset();
    document.getElementById('packId').value = '';
    document.getElementById('formError').classList.add('hidden');
    document.getElementById('packPrice').disabled = true;
    
    if (packId) {
        // Edit mode - populate form
        const pack = stickerPacks.find(p => p.id === packId);
        if (pack) {
            document.getElementById('modalTitle').textContent = 'Chỉnh sửa Sticker Pack';
            document.getElementById('packId').value = pack.id;
            document.getElementById('packName').value = pack.name || '';
            document.getElementById('packDescription').value = pack.description || '';
            document.getElementById('packType').value = pack.type || 'official';
            document.getElementById('packIcon').value = pack.iconUrl || '';
            document.getElementById('packPrice').value = pack.price || 0;
            document.getElementById('isPublished').checked = pack.isPublished !== false;
            document.getElementById('isFeatured').checked = pack.isFeatured === true;
            document.getElementById('isFree').checked = pack.isFree !== false;
            document.getElementById('packPrice').disabled = pack.isFree !== false;
        }
    } else {
        // Add mode
        document.getElementById('modalTitle').textContent = 'Thêm Sticker Pack mới';
    }
    
    packModal.classList.add('active');
}

/**
 * Close modal
 */
function closeModal() {
    packModal.classList.remove('active');
    currentPackId = null;
}

/**
 * Handle form submission
 */
async function handleFormSubmit(e) {
    e.preventDefault();
    
    const submitBtn = packForm.querySelector('button[type="submit"]');
    const errorElement = document.getElementById('formError');
    
    // Get form values
    const packData = {
        name: document.getElementById('packName').value.trim(),
        description: document.getElementById('packDescription').value.trim(),
        type: document.getElementById('packType').value,
        iconUrl: document.getElementById('packIcon').value.trim(),
        price: parseInt(document.getElementById('packPrice').value) || 0,
        isPublished: document.getElementById('isPublished').checked,
        isFeatured: document.getElementById('isFeatured').checked,
        isFree: document.getElementById('isFree').checked,
        updatedAt: Date.now()
    };
    
    // Validation
    if (!packData.name) {
        errorElement.textContent = 'Vui lòng nhập tên pack';
        errorElement.classList.remove('hidden');
        return;
    }
    
    errorElement.classList.add('hidden');
    setButtonLoading(submitBtn, true);
    
    try {
        const packId = document.getElementById('packId').value;
        
        if (packId) {
            // Update existing pack
            await db.collection('stickerPacks').doc(packId).update(packData);
            console.log('Pack updated:', packId);
        } else {
            // Create new pack
            packData.createdAt = Date.now();
            packData.creatorId = auth.currentUser.uid;
            packData.stickerCount = 0;
            packData.downloadCount = 0;
            
            const docRef = await db.collection('stickerPacks').add(packData);
            console.log('Pack created:', docRef.id);
        }
        
        closeModal();
        loadStickerPacks();
        
    } catch (error) {
        console.error('Error saving pack:', error);
        errorElement.textContent = 'Lỗi: ' + error.message;
        errorElement.classList.remove('hidden');
    } finally {
        setButtonLoading(submitBtn, false);
    }
}

/**
 * Toggle publish status
 */
async function togglePublish(packId, currentStatus) {
    try {
        await db.collection('stickerPacks').doc(packId).update({
            isPublished: !currentStatus,
            updatedAt: Date.now()
        });
        
        loadStickerPacks();
        
    } catch (error) {
        console.error('Error toggling publish:', error);
        alert('Lỗi: ' + error.message);
    }
}

/**
 * Open delete confirmation modal
 */
function openDeleteModal(packId, packName) {
    deletePackId = packId;
    document.getElementById('deletePackName').textContent = packName;
    deleteModal.classList.add('active');
}

/**
 * Close delete modal
 */
function closeDeleteModal() {
    deleteModal.classList.remove('active');
    deletePackId = null;
}

/**
 * Confirm delete
 */
async function confirmDelete() {
    if (!deletePackId) return;
    
    const confirmBtn = document.getElementById('confirmDeleteBtn');
    confirmBtn.disabled = true;
    confirmBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Đang xóa...';
    
    try {
        // Delete all stickers in the pack first
        const stickersSnapshot = await db.collection('stickerPacks')
            .doc(deletePackId)
            .collection('stickers')
            .get();
        
        const batch = db.batch();
        stickersSnapshot.forEach(doc => {
            batch.delete(doc.ref);
        });
        
        // Delete the pack document
        batch.delete(db.collection('stickerPacks').doc(deletePackId));
        
        await batch.commit();
        
        console.log('Pack deleted:', deletePackId);
        closeDeleteModal();
        loadStickerPacks();
        
    } catch (error) {
        console.error('Error deleting pack:', error);
        alert('Lỗi xóa pack: ' + error.message);
    } finally {
        confirmBtn.disabled = false;
        confirmBtn.innerHTML = '<i class="fas fa-trash"></i> Xóa';
    }
}

/**
 * Utility functions
 */
function formatNumber(num) {
    if (num >= 1000000) {
        return (num / 1000000).toFixed(1) + 'M';
    } else if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K';
    }
    return num.toString();
}

function formatPrice(price) {
    return price.toString().replace(/\B(?=(\d{3})+(?!\d))/g, '.');
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function setButtonLoading(button, isLoading) {
    const btnText = button.querySelector('.btn-text');
    const btnLoader = button.querySelector('.btn-loader');
    
    if (isLoading) {
        button.disabled = true;
        if (btnText) btnText.classList.add('hidden');
        if (btnLoader) btnLoader.classList.remove('hidden');
    } else {
        button.disabled = false;
        if (btnText) btnText.classList.remove('hidden');
        if (btnLoader) btnLoader.classList.add('hidden');
    }
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
