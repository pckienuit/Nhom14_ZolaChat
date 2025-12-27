# Zalo Clone - Android App Documentation

## Tổng quan
Ứng dụng Android được tạo dựa trên codebase React, bao gồm các màn hình chính:
- **Login/SignUp** - Đăng nhập và đăng ký
- **Home (Messages)** - Danh sách cuộc trò chuyện
- **Contact (Friend Requests)** - Yêu cầu kết bạn
- **Room (Chat)** - Chi tiết cuộc trò chuyện

## Cấu trúc Project

```
app/src/main/java/com/example/doan_zaloclone/
├── models/                      # Data models
│   ├── Conversation.java        # Model cuộc trò chuyện
│   ├── Message.java            # Model tin nhắn
│   └── FriendRequest.java      # Model yêu cầu kết bạn
│
├── ui/
│   ├── login/                  # Authentication screens
│   │   ├── LoginActivity.java
│   │   └── SignUpActivity.java
│   │
│   ├── home/                   # Home/Messages screen
│   │   ├── HomeFragment.java
│   │   └── ConversationAdapter.java
│   │
│   ├── contact/                # Friend requests screen
│   │   ├── ContactFragment.java
│   │   └── FriendRequestAdapter.java
│   │
│   └── room/                   # Chat screen
│       ├── RoomActivity.java
│       └── MessageAdapter.java
│
└── MainActivity.java           # Main activity with bottom navigation
```

## Các thành phần chính

### 1. Authentication Flow
- **LoginActivity**: Màn hình đăng nhập (launcher activity)
- **SignUpActivity**: Màn hình đăng ký
- Demo mode: Tự động đăng nhập thành công, chưa tích hợp backend

### 2. Bottom Navigation
**MainActivity** chứa:
- `BottomNavigationView` với 2 tabs: Messages và Contacts
- `FragmentContainer` để hiển thị HomeFragment hoặc ContactFragment

### 3. Home (Messages)
- **HomeFragment**: Hiển thị danh sách conversations
- **ConversationAdapter**: RecyclerView adapter cho conversations
- Click vào conversation → mở RoomActivity

### 4. Contact (Friend Requests)
- **ContactFragment**: Hiển thị friend requests
- **FriendRequestAdapter**: Adapter với nút Accept/Reject
- Demo data: 2 friend requests mẫu

### 5. Room (Chat)
- **RoomActivity**: Màn hình chat chi tiết
- **MessageAdapter**: Hiển thị tin nhắn (sent/received)
- Hỗ trợ gửi tin nhắn (demo mode)
- Toolbar với back button và tên conversation

## Layouts XML

### Main Layouts
- `activity_main.xml` - BottomNavigation + Fragment container
- `activity_login.xml` - Form đăng nhập
- `activity_signup.xml` - Form đăng ký
- `activity_room.xml` - Chat interface
- `fragment_home.xml` - Danh sách conversations
- `fragment_contact.xml` - Danh sách friend requests

### Item Layouts
- `item_conversation.xml` - Card view cho conversation
- `item_friend_request.xml` - Card view cho friend request
- `item_message_sent.xml` - Tin nhắn đã gửi (màu xanh, bên phải)
- `item_message_received.xml` - Tin nhắn nhận (màu xám, bên trái)

### Resources
- `menu/bottom_navigation_menu.xml` - Bottom nav menu items
- `drawable/` - Icons (ic_message, ic_contacts, ic_send, ic_arrow_back, ic_avatar)
- `colors.xml` - Color scheme (colorPrimary: #0084FF - màu xanh Zalo)

## Dependencies (build.gradle.kts)

```kotlin
// Navigation
implementation(libs.navigation.fragment)
implementation(libs.navigation.ui)

// Lifecycle
implementation(libs.lifecycle.viewmodel)
implementation(libs.lifecycle.livedata)

// UI Components
implementation(libs.recyclerview)
implementation(libs.cardview)
implementation(libs.material)

// Image Loading
implementation(libs.glide)
```

## Chạy ứng dụng

### Build và cài đặt:
```bash
# Windows
.\gradlew build
.\gradlew installDebug

# Hoặc từ Android Studio
Run → Run 'app'
```

### Demo Mode
- Login: Nhập bất kỳ email/password (tối thiểu 6 ký tự) → tự động đăng nhập
- Conversations: Hiển thị 3 conversations mẫu
- Friend Requests: Hiển thị 2 requests mẫu
- Chat: Gửi tin nhắn demo (lưu local, không gọi API)

## Navigation Flow

```
LoginActivity (Launcher)
    ↓
    ├─→ SignUpActivity (click "Sign Up")
    │       ↓
    │       └─→ back to LoginActivity
    ↓
MainActivity (sau khi login)
    ├─→ HomeFragment (tab Messages)
    │       ↓
    │       └─→ RoomActivity (click conversation)
    │               ↓
    │               └─→ back to MainActivity
    └─→ ContactFragment (tab Contacts)
```

## Tính năng đã implement

✅ **Authentication**
- Login form với validation
- SignUp form với confirm password
- Navigation giữa Login/SignUp

✅ **Bottom Navigation**
- Tab Messages (HomeFragment)
- Tab Contacts (ContactFragment)
- State persistence

✅ **Messages/Conversations**
- RecyclerView với conversations
- Click để mở chat detail
- Hiển thị last message và timestamp

✅ **Friend Requests**
- List requests với Accept/Reject buttons
- Toast notifications khi accept/reject

✅ **Chat Room**
- Hiển thị messages (sent/received)
- Input field với send button
- Scroll to bottom khi gửi message
- Toolbar với back button

## Chưa implement (cần backend)

❌ Real authentication API
❌ Socket.io integration cho real-time chat
❌ Load conversations từ API
❌ Load friend requests từ API
❌ Send/receive messages qua API
❌ Image upload và avatar loading
❌ Push notifications
❌ User profile management

## Tích hợp Backend (Next Steps)

Để tích hợp với backend React (localhost:3003):

1. **Add dependencies** trong `build.gradle.kts`:
```kotlin
implementation("com.squareup.retrofit2:retrofit:2.9.0")
implementation("com.squareup.retrofit2:converter-gson:2.9.0")
implementation("io.socket:socket.io-client:2.1.0")
```

2. **Tạo API Service** tương tự như React services:
```java
// ApiClient.java
public interface ApiService {
    @POST("api/v1/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);
    
    @GET("api/v1/conversations/{userId}")
    Call<List<Conversation>> getConversations(@Path("userId") String userId);
}
```

3. **Socket.io Integration**:
```java
// Trong RoomActivity
Socket socket = IO.socket("http://10.0.2.2:3003/chat");
socket.on("mess", args -> {
    // Handle incoming message
});
```

4. **Cập nhật base URL** cho Android emulator:
- Localhost trên Android emulator: `http://10.0.2.2:3003`
- Device thật: Dùng IP máy host

## Ghi chú kỹ thuật

- **Min SDK**: 28 (Android 9.0)
- **Target SDK**: 36
- **ViewBinding**: Enabled
- **Architecture**: Fragment-based với RecyclerView adapters
- **Demo data**: Hardcoded trong các Fragment/Activity
- **Navigation**: Activity + Fragment navigation (chưa dùng Navigation Component)

## Màn hình Demo

1. **Login** → Nhập email + password → Login
2. **Home** → Hiển thị 3 conversations → Click để xem chat
3. **Contact** → Hiển thị 2 friend requests → Accept/Reject
4. **Chat** → Gửi tin nhắn → Hiển thị ngay lập tức
