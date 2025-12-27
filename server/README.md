# ZaloClone API Server

Backend API cho ứng dụng ZaloClone - Thay thế direct Firebase access bằng API trung gian.

## ✅ Status: Phase 2 Completed

Server đã chạy thành công với đầy đủ tính năng!

## Quick Start

### 1. Cài đặt dependencies
```bash
cd server
npm install
```

### 2. Cấu hình
Copy `.env.example` thành `.env` và điền thông tin.

### 3. Chạy server
```bash
npm start
```

Server sẽ chạy tại: `http://localhost:3000`

## API Documentation

### Health Check
```
GET /health
Response: {"status":"ok","timestamp":1234567890,"uptime":123}
```

### Authentication Required
Tất cả API endpoints (trừ `/health` và `/api/stickers/packs`) yêu cầu Firebase ID token:

```
Authorization: Bearer <firebase-id-token>
```

### Available Endpoints

**Users:**
- `GET /api/users/:userId` - Get user
- `PUT /api/users/:userId` - Update user
- `POST /api/users/:userId/status` - Update status
- `GET /api/users` - List users (admin)
- `POST /api/users/:userId/ban` - Ban user (admin)

**Messages:**
- `GET /api/chats/:conversationId/messages` - Get messages
- `POST /api/chats/:conversationId/messages` - Send message

**Conversations:**
- `GET /api/conversations` - List conversations
- `POST /api/conversations` - Create conversation

**Calls:**
- `GET /api/calls` - Get call history
- `POST /api/calls` - Create call log

**Friends:**
- `GET /api/friends` - Get friends
- `GET /api/friends/requests` - Get requests
- `POST /api/friends/requests` - Send request
- `PUT /api/friends/requests/:id` - Accept/reject

**Stickers:**
- `GET /api/stickers/packs` - Get all packs
- `GET /api/stickers/packs/:id` - Get pack details

### WebSocket
Connect to: `ws://localhost:3000`

Events:
- `join_conversation` - Join room
- `typing` - Typing indicator
- `new_message` - New message broadcast

## Testing

Test với browser:
```
http://localhost:3000/health
```

Test với Postman:
1. Get Firebase ID token từ app
2. Add header: `Authorization: Bearer <token>`
3. Test các endpoints

## Deployment

Xem `docs/32_migration_api-intermediary-deployment.md` để deploy lên VPS.

Quick deploy:
```bash
ssh root@163.61.182.20
cd /opt/zaloclone-api/server
git pull
npm install
pm2 restart zaloclone-api
```

## Troubleshooting

**Server không start:**
- Check `serviceAccountKey.json` có đúng path không
- Verify `.env` file đầy đủ
- Port 3000 có bị chiếm không

**Firebase error:**
- Verify `serviceAccountKey.json` format
- Check Firebase project ID

**CORS error:**
- Thêm domain vào `ALLOWED_ORIGINS` trong `.env`

## Next Steps

Phase 3: Migrate Android app để dùng API này.

Xem: `docs/33_phase2-backend-api-completed.md` để biết chi tiết.
