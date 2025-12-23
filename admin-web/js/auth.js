/**
 * Authentication Logic for ZaloClone Admin
 * =========================================
 * 
 * Handles login, logout, and admin verification.
 * Admin users must have 'admin: true' custom claim in Firebase Auth.
 */

// Check if user is on login page
const isLoginPage = window.location.pathname.endsWith('index.html') || 
                   window.location.pathname === '/' ||
                   window.location.pathname.endsWith('/');

// Authentication state observer
auth.onAuthStateChanged(async (user) => {
    if (user) {
        // User is signed in
        console.log('User signed in:', user.email);
        
        // Check if user is admin
        const isAdmin = await checkAdminClaim(user);
        
        if (isAdmin) {
            // User is admin
            if (isLoginPage) {
                // Redirect to dashboard if on login page
                window.location.href = 'dashboard.html';
            } else {
                // Update UI with user info
                updateUserInfo(user);
            }
        } else {
            // User is not admin
            console.warn('User is not an admin');
            if (!isLoginPage) {
                // Sign out and redirect to login
                await auth.signOut();
                window.location.href = 'index.html';
            } else {
                showError('Tài khoản không có quyền Admin');
            }
        }
    } else {
        // User is signed out
        console.log('No user signed in');
        if (!isLoginPage) {
            // Redirect to login page
            window.location.href = 'index.html';
        }
    }
});

/**
 * Check if user has admin custom claim
 * For now, we'll check if user email contains 'admin' or is in a whitelist
 * In production, use Firebase Custom Claims
 */
async function checkAdminClaim(user) {
    try {
        // Method 1: Check custom claims (preferred)
        const idTokenResult = await user.getIdTokenResult();
        if (idTokenResult.claims.admin === true) {
            return true;
        }
        
        // Method 2: Check against Firestore admin list
        const adminDoc = await db.collection('admins').doc(user.uid).get();
        if (adminDoc.exists) {
            return true;
        }
        
        // Method 3: Fallback - check if user document has isAdmin flag
        const userDoc = await db.collection('users').doc(user.uid).get();
        if (userDoc.exists && userDoc.data().isAdmin === true) {
            return true;
        }
        
        // Method 4: For development - allow specific emails
        const adminEmails = [
            'admin@example.com',
            'admin@zaloclone.com'
            // Add your admin email here
        ];
        if (adminEmails.includes(user.email)) {
            return true;
        }
        
        return false;
    } catch (error) {
        console.error('Error checking admin claim:', error);
        return false;
    }
}

/**
 * Update UI with user information
 */
function updateUserInfo(user) {
    const adminEmailElement = document.getElementById('adminEmail');
    if (adminEmailElement) {
        adminEmailElement.textContent = user.email;
    }
}

/**
 * Show error message on login page
 */
function showError(message) {
    const errorElement = document.getElementById('errorMessage');
    if (errorElement) {
        errorElement.textContent = message;
        errorElement.classList.remove('hidden');
    }
}

/**
 * Hide error message
 */
function hideError() {
    const errorElement = document.getElementById('errorMessage');
    if (errorElement) {
        errorElement.classList.add('hidden');
    }
}

/**
 * Set loading state for button
 */
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

// Login form handler
const loginForm = document.getElementById('loginForm');
if (loginForm) {
    loginForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        hideError();
        
        const email = document.getElementById('email').value.trim();
        const password = document.getElementById('password').value;
        const submitBtn = loginForm.querySelector('button[type="submit"]');
        
        if (!email || !password) {
            showError('Vui lòng nhập email và mật khẩu');
            return;
        }
        
        setButtonLoading(submitBtn, true);
        
        try {
            await auth.signInWithEmailAndPassword(email, password);
            // Auth state observer will handle redirect
        } catch (error) {
            console.error('Login error:', error);
            let message = 'Đăng nhập thất bại';
            
            switch (error.code) {
                case 'auth/user-not-found':
                    message = 'Tài khoản không tồn tại';
                    break;
                case 'auth/wrong-password':
                    message = 'Mật khẩu không đúng';
                    break;
                case 'auth/invalid-email':
                    message = 'Email không hợp lệ';
                    break;
                case 'auth/too-many-requests':
                    message = 'Quá nhiều lần thử, vui lòng thử lại sau';
                    break;
                case 'auth/invalid-credential':
                    message = 'Email hoặc mật khẩu không đúng';
                    break;
            }
            
            showError(message);
            setButtonLoading(submitBtn, false);
        }
    });
}

// Toggle password visibility
const togglePasswordBtn = document.querySelector('.toggle-password');
if (togglePasswordBtn) {
    togglePasswordBtn.addEventListener('click', () => {
        const passwordInput = document.getElementById('password');
        const icon = togglePasswordBtn.querySelector('i');
        
        if (passwordInput.type === 'password') {
            passwordInput.type = 'text';
            icon.classList.remove('fa-eye');
            icon.classList.add('fa-eye-slash');
        } else {
            passwordInput.type = 'password';
            icon.classList.remove('fa-eye-slash');
            icon.classList.add('fa-eye');
        }
    });
}

// Logout button handler
const logoutBtn = document.getElementById('logoutBtn');
if (logoutBtn) {
    logoutBtn.addEventListener('click', async () => {
        try {
            await auth.signOut();
            window.location.href = 'index.html';
        } catch (error) {
            console.error('Logout error:', error);
        }
    });
}

// Mobile menu toggle
const menuToggle = document.getElementById('menuToggle');
const sidebar = document.getElementById('sidebar');
if (menuToggle && sidebar) {
    menuToggle.addEventListener('click', () => {
        sidebar.classList.toggle('open');
    });
    
    // Close sidebar when clicking outside
    document.addEventListener('click', (e) => {
        if (!sidebar.contains(e.target) && !menuToggle.contains(e.target)) {
            sidebar.classList.remove('open');
        }
    });
}
