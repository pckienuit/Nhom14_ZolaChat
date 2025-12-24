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
    
    // Icon file upload
    document.getElementById('iconFileInput').addEventListener('change', handleIconUpload);
    
    // Stickers upload zone
    const uploadZone = document.getElementById('stickerUploadZone');
    const stickerFilesInput = document.getElementById('stickerFilesInput');
    
    // Click to select files
    uploadZone.addEventListener('click', () => stickerFilesInput.click());
    
    // File input change
    stickerFilesInput.addEventListener('change', (e) => handleStickerFiles(e.target.files));
    
    // Drag and drop
    uploadZone.addEventListener('dragover', (e) => {
        e.preventDefault();
        uploadZone.classList.add('dragover');
    });
    uploadZone.addEventListener('dragleave', () => {
        uploadZone.classList.remove('dragover');
    });
    uploadZone.addEventListener('drop', (e) => {
        e.preventDefault();
        uploadZone.classList.remove('dragover');
        handleStickerFiles(e.dataTransfer.files);
    });
}

// Store uploaded stickers temporarily
let uploadedStickers = [];

/**
 * Handle sticker files selection
 */
async function handleStickerFiles(files) {
    const previewGrid = document.getElementById('stickerPreviewGrid');
    const placeholder = document.getElementById('uploadPlaceholder');
    const progressDiv = document.getElementById('uploadProgress');
    const progressFill = document.getElementById('progressFill');
    const progressText = document.getElementById('progressText');
    
    if (!files || files.length === 0) return;
    
    // Hide placeholder, show progress
    placeholder.style.display = 'none';
    progressDiv.classList.remove('hidden');
    
    let completed = 0;
    const total = files.length;
    
    for (const file of files) {
        // Validate
        if (!file.type.startsWith('image/')) continue;
        if (file.size > 2 * 1024 * 1024) {
            alert(`${file.name} quá lớn (max 2MB)`);
            continue;
        }
        
        // Create preview item
        const previewId = 'preview_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
        const previewItem = document.createElement('div');
        previewItem.className = 'sticker-preview-item uploading';
        previewItem.id = previewId;
        previewItem.innerHTML = `
            <img src="${URL.createObjectURL(file)}" alt="preview">
            <button class="remove-btn" style="display:none;"><i class="fas fa-times"></i></button>
        `;
        previewGrid.appendChild(previewItem);
        
        try {
            // Upload to VPS
            const formData = new FormData();
            formData.append('sticker', file);
            formData.append('userId', auth.currentUser.uid);
            
            const response = await fetch('http://163.61.182.20/api/stickers/upload', {
                method: 'POST',
                body: formData
            });
            
            const result = await response.json();
            
            if (result.success && result.sticker) {
                // Store sticker info
                uploadedStickers.push(result.sticker);
                
                // Update preview
                previewItem.classList.remove('uploading');
                const removeBtn = previewItem.querySelector('.remove-btn');
                removeBtn.style.display = 'flex';
                removeBtn.onclick = (e) => {
                    e.stopPropagation();
                    previewItem.remove();
                    uploadedStickers = uploadedStickers.filter(s => s.id !== result.sticker.id);
                    if (previewGrid.children.length === 0) {
                        placeholder.style.display = 'block';
                    }
                };
            } else {
                previewItem.remove();
                console.error('Upload failed:', result.error);
            }
        } catch (error) {
            previewItem.remove();
            console.error('Upload error:', error);
        }
        
        // Update progress
        completed++;
        const percent = Math.round((completed / total) * 100);
        progressFill.style.width = percent + '%';
        progressText.textContent = percent + '%';
    }
    
    // Hide progress
    setTimeout(() => {
        progressDiv.classList.add('hidden');
        progressFill.style.width = '0%';
    }, 500);
}

/**
 * Handle icon file upload
 */
async function handleIconUpload(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    // Validate file type
    if (!file.type.startsWith('image/')) {
        alert('Vui lòng chọn file ảnh!');
        return;
    }
    
    // Validate file size (max 2MB)
    if (file.size > 2 * 1024 * 1024) {
        alert('File quá lớn! Tối đa 2MB.');
        return;
    }
    
    // Show progress
    const iconInput = document.getElementById('packIcon');
    const originalPlaceholder = iconInput.placeholder;
    iconInput.placeholder = 'Đang upload...';
    iconInput.disabled = true;
    
    try {
        // Upload to VPS
        const formData = new FormData();
        formData.append('sticker', file);
        formData.append('userId', auth.currentUser.uid);
        
        const response = await fetch('https://zolachat.site/api/stickers/upload', {
            method: 'POST',
            body: formData
        });
        
        if (!response.ok) {
            const errorText = await response.text();
            console.error('Upload response:', errorText);
            throw new Error('Upload failed: ' + response.statusText);
        }
        
        const result = await response.json();
        console.log('Upload result:', result);
        
        // Server trả về { success, sticker: { url, ... } }
        if (result.success && result.sticker && result.sticker.url) {
            iconInput.value = result.sticker.url;
            iconInput.placeholder = originalPlaceholder;
            console.log('Icon uploaded successfully:', result.sticker.url);
        } else if (result.error) {
            throw new Error(result.error);
        } else {
            throw new Error('Upload failed: Invalid response');
        }
        
    } catch (error) {
        console.error('Error uploading icon:', error);
        alert('Lỗi upload file: ' + error.message);
        iconInput.placeholder = originalPlaceholder;
    } finally {
        iconInput.disabled = false;
        // Reset file input
        event.target.value = '';
    }
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
                ${pack.iconUrl 
                    ? `<img class="pack-icon" src="${pack.iconUrl}" alt="${escapeHtml(pack.name)}">` 
                    : `<div class="pack-icon" style="display: flex; align-items: center; justify-content: center; background: var(--gray-700); font-size: 2rem;">😊</div>`}
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
    
    // Reset sticker upload
    uploadedStickers = [];
    document.getElementById('stickerPreviewGrid').innerHTML = '';
    document.getElementById('uploadPlaceholder').style.display = 'block';
    document.getElementById('uploadProgress').classList.add('hidden');
    
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
            
            // Load existing stickers
            loadExistingStickers(packId);
        }
    } else {
        // Add mode
        document.getElementById('modalTitle').textContent = 'Thêm Sticker Pack mới';
        document.getElementById('existingStickersSection').style.display = 'none';
    }
    
    packModal.classList.add('active');
}

/**
 * Load existing stickers in pack
 */
async function loadExistingStickers(packId) {
    const section = document.getElementById('existingStickersSection');
    const grid = document.getElementById('existingStickersGrid');
    const countLabel = document.getElementById('existingStickerCount');
    
    section.style.display = 'block';
    grid.innerHTML = `
        <div class="loading-placeholder">
            <i class="fas fa-spinner fa-spin"></i>
            <p>Đang tải stickers...</p>
        </div>
    `;
    
    try {
        const stickersSnapshot = await db.collection('stickerPacks')
            .doc(packId)
            .collection('stickers')
            .orderBy('createdAt', 'asc')
            .get();
        
        const stickers = [];
        stickersSnapshot.forEach(doc => {
            stickers.push({ id: doc.id, ...doc.data() });
        });
        
        // --- PROACTIVE FIX: Sync stickerCount if mismatch detected ---
        const realCount = stickers.length;
        const currentPack = stickerPacks.find(p => p.id === packId);
        
        if (currentPack && currentPack.stickerCount !== realCount) {
            console.log(`Fixing sticker count mismatch. Meta: ${currentPack.stickerCount}, Real: ${realCount}`);
            
            // Update Firestore silently
            db.collection('stickerPacks').doc(packId).update({
                stickerCount: realCount
            }).then(() => {
                // Refresh outside list to show correct count
                loadStickerPacks();
            }).catch(err => console.error("Auto-fix count failed:", err));
        }
        // -----------------------------------------------------------
        
        if (stickers.length === 0) {
            grid.innerHTML = `
                <div class="loading-placeholder">
                    <i class="fas fa-info-circle"></i>
                    <p>Chưa có sticker nào trong pack này</p>
                </div>
            `;
            countLabel.textContent = '(0 stickers)';
            return;
        }
        
        countLabel.textContent = `(${stickers.length} stickers)`;
        grid.innerHTML = stickers.map(sticker => `
            <div class="existing-sticker-item" data-sticker-id="${sticker.id}">
                <img src="${sticker.imageUrl || sticker.thumbnailUrl}" 
                     alt="Sticker ${sticker.id}"
                     onerror="this.src='data:image/svg+xml,%3Csvg xmlns=%22http://www.w3.org/2000/svg%22 viewBox=%220 0 100 100%22%3E%3Ctext y=%2250%22 font-size=%2250%22%3E❓%3C/text%3E%3C/svg%3E'">
                <button class="delete-sticker-btn" 
                        onclick="deleteSticker('${packId}', '${sticker.id}')"
                        title="Xóa sticker">
                    <i class="fas fa-times"></i>
                </button>
            </div>
        `).join('');
        
    } catch (error) {
        console.error('Error loading existing stickers:', error);
        grid.innerHTML = `
            <div class="loading-placeholder">
                <i class="fas fa-exclamation-triangle"></i>
                <p>Lỗi tải stickers: ${error.message}</p>
            </div>
        `;
    }
}

/**
 * Delete a sticker from pack
 */
async function deleteSticker(packId, stickerId) {
    if (!confirm('Bạn có chắc chắn muốn xóa sticker này?')) {
        return;
    }
    
    // UI Feedback immediately (Optimistic UI could go here, but let's stick to safe)
    const btn = document.querySelector(`div[data-sticker-id="${stickerId}"] button`);
    if (btn) btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';

    try {
        // 1. Critical Phase: Delete Data
        await db.collection('stickerPacks')
            .doc(packId)
            .collection('stickers')
            .doc(stickerId)
            .delete();
        
        await db.collection('stickerPacks')
            .doc(packId)
            .update({
                stickerCount: firebase.firestore.FieldValue.increment(-1)
            });

        // Delete success flag
        console.log('Sticker deleted successfully from DB');
        
        try {
            // 2. Non-critical Phase: Refresh UI
            // Reload existing stickers to update grid
            await loadExistingStickers(packId);
            
            // Reload packs to update count on card
            loadStickerPacks(); 
        } catch (refreshError) {
            console.error('Sticker deleted but UI refresh failed:', refreshError);
            // Don't alert the user, just log it. The sticker is gone.
        }
        
    } catch (error) {
        console.error('Error deleting sticker:', error);
        alert('Lỗi xóa sticker: ' + error.message);
        // Reset button if failed
        if (btn) btn.innerHTML = '<i class="fas fa-times"></i>';
    }
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
        let savedPackId = packId;
        
        if (packId) {
            // Update existing pack
            await db.collection('stickerPacks').doc(packId).update(packData);
            console.log('Pack updated:', packId);
        } else {
            // Create new pack
            packData.createdAt = Date.now();
            packData.creatorId = auth.currentUser.uid;
            packData.stickerCount = uploadedStickers.length;
            packData.downloadCount = 0;
            
            const docRef = await db.collection('stickerPacks').add(packData);
            savedPackId = docRef.id;
            console.log('Pack created:', savedPackId);
        }
        
        // Save uploaded stickers to Firestore subcollection
        if (uploadedStickers.length > 0) {
            const batch = db.batch();
            const packRef = db.collection('stickerPacks').doc(savedPackId);
            
            uploadedStickers.forEach((sticker, index) => {
                const stickerRef = packRef.collection('stickers').doc(sticker.id);
                batch.set(stickerRef, {
                    imageUrl: sticker.url,
                    thumbnailUrl: sticker.thumbnailUrl,
                    width: sticker.width || 512,
                    height: sticker.height || 512,
                    isAnimated: sticker.isAnimated || false,
                    format: sticker.format || 'webp',
                    order: index,
                    createdAt: Date.now()
                });
            });
            
            // Update sticker count on pack
            batch.update(packRef, {
                stickerCount: firebase.firestore.FieldValue.increment(uploadedStickers.length)
            });
            
            await batch.commit();
            console.log('Saved', uploadedStickers.length, 'stickers to pack:', savedPackId);
        }
        
        // Reset uploaded stickers array
        uploadedStickers = [];
        
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
