# Admin Web - ZaloClone Management

## Thông tin VPS
- **IP**: 163.61.182.20
- **Web Path**: `/var/www/admin/`
- **URL**: http://163.61.182.20/admin/

---

## 🚀 SETUP LẦN ĐẦU (Chỉ chạy 1 lần)

### Bước 1: SSH vào VPS
```cmd
ssh root@163.61.182.20
```

### Bước 2: Tạo thư mục và cấu hình Apache (chạy trên VPS)
```bash
# Tạo thư mục
mkdir -p /var/www/admin

# Tạo Apache config
cat > /etc/apache2/sites-available/admin.conf << 'EOF'
Alias /admin /var/www/admin
<Directory /var/www/admin>
    Options -Indexes +FollowSymLinks
    AllowOverride All
    Require all granted
</Directory>
EOF

# Enable site và reload
a2ensite admin.conf
systemctl reload apache2

# Thoát SSH
exit
```

### Bước 3: Upload files lần đầu (chạy trên Windows CMD/PowerShell)
```cmd
cd d:\DoAn_ZaloClone
scp -r admin-web/* root@163.61.182.20:/var/www/admin/
```

### Bước 4: Set permissions (SSH vào VPS)
```bash
ssh root@163.61.182.20 "chown -R www-data:www-data /var/www/admin && chmod -R 755 /var/www/admin"
```

✅ **Xong!** Truy cập: http://163.61.182.20/admin/

---

## 🔄 CẬP NHẬT & DEPLOY LẠI (Mỗi lần update code)

Mỗi khi sửa code trong `admin-web/`, chỉ cần chạy **1 lệnh** từ Windows:

### Cách 1: Upload tất cả files (đơn giản nhất)
```cmd
cd d:\DoAn_ZaloClone
scp -r admin-web/* root@163.61.182.20:/var/www/admin/
```

### Cách 2: Upload file cụ thể (nhanh hơn)
```cmd
:: Upload 1 file
scp d:\DoAn_ZaloClone\admin-web\js\stickers.js root@163.61.182.20:/var/www/admin/js/

:: Upload 1 thư mục
scp -r d:\DoAn_ZaloClone\admin-web\css/* root@163.61.182.20:/var/www/admin/css/
```

### Cách 3: Sync với rsync (cần Git Bash hoặc WSL)
```bash
rsync -avz --delete admin-web/ root@163.61.182.20:/var/www/admin/
```

> 💡 **Tip**: Không cần restart Apache sau khi update HTML/CSS/JS files!

---

## 🔐 Thiết lập Admin Account

### Cách 1: Thêm email vào whitelist (Development)
Mở `admin-web/js/auth.js`, tìm và sửa:
```javascript
const adminEmails = [
    'admin@example.com',
    'your-email@gmail.com'  // ← Thêm email của bạn
];
```

### Cách 2: Tạo document trong Firestore
Tạo document `/admins/{userId}` với bất kỳ nội dung nào.

### Cách 3: Set custom claim (Production)
```javascript
// Chạy bằng Firebase Admin SDK
admin.auth().setCustomUserClaims(uid, { admin: true });
```

---

## 🔥 Cập nhật Firestore Rules

Copy nội dung file `firestore.rules.updated` lên Firebase Console:
1. Vào Firebase Console → Firestore → Rules
2. Paste nội dung từ file
3. Click **Publish**

---

## ❓ Troubleshooting

### 404 Not Found
```bash
# Kiểm tra files đã upload chưa
ssh root@163.61.182.20 "ls -la /var/www/admin/"

# Kiểm tra Apache config
ssh root@163.61.182.20 "apache2ctl -S"
```

### Permission Denied (Firestore)
- Đảm bảo đã deploy Firestore rules mới
- Kiểm tra tài khoản có trong whitelist hoặc `/admins` collection

### Không load được CSS/JS
```bash
ssh root@163.61.182.20 "chown -R www-data:www-data /var/www/admin"
```

---

## 📁 Cấu trúc Files

```
admin-web/
├── index.html          # Trang login
├── dashboard.html      # Dashboard thống kê
├── stickers.html       # Quản lý sticker packs
├── users.html          # Quản lý users
├── css/
│   ├── style.css       # Design system
│   ├── dashboard.css   # Layout
│   └── components.css  # Components
└── js/
    ├── firebase-config.js
    ├── auth.js
    ├── dashboard.js
    ├── stickers.js
    └── users.js
```
