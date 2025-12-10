# Zalo Clone - Android Messaging App

Native Android messaging application built with Java and Firebase.

## Tech Stack

- Java 11
- Firebase (Auth, Firestore, Storage, Messaging)
- Material Design 3
- Android SDK 28-36

## Project Structure

```
app/src/main/java/com/example/doan_zaloclone/
├── repository/         # Data layer (AuthRepository)
├── ui/
│   ├── login/         # LoginActivity, SignUpActivity
│   ├── home/          # HomeFragment, ConversationAdapter
│   ├── contact/       # ContactFragment, FriendRequestAdapter
│   └── room/          # RoomActivity, MessageAdapter
└── MainActivity.java  # Main container with bottom navigation
```

## Firebase Configuration

### Prerequisites

1. Enable Firestore API:
   https://console.developers.google.com/apis/api/firestore.googleapis.com/overview?project=zalo-clone-dd021

2. Create Firestore Database:
   - Go to Firebase Console > Firestore
   - Create database in test mode
   - Location: asia-southeast1 (Singapore)

3. Add SHA-1 certificate:
   - Firebase Console > Project Settings > Your apps
   - Add fingerprint: `AA:61:22:72:F5:62:38:19:77:22:12:19:9C:96:73:40:9E:F4:A4:B1`

### Firestore Schema

```
users/{userId}
  - userId: string
  - name: string
  - email: string
  - createdAt: timestamp
  - isOnline: boolean
  - lastSeen: timestamp
```

## Build & Run

```bash
# Build APK
.\gradlew.bat assembleDebug

# Install to device/emulator
.\gradlew.bat installDebug

# Clean build
.\gradlew.bat clean
```

Or use Android Studio: Run > Run 'app'

## Demo Account

```
Email: demo@gmail.com
Password: 123456
```

## Features

- Firebase Authentication (Email/Password)
- Auto-login with session persistence
- Instant logout (no delay)
- User registration with Firestore sync
- Bottom navigation (Messages/Contacts)
- Chat conversations with RecyclerView
- Friend requests management

## Development Notes

- Min SDK: 28 (Android 9.0)
- Target SDK: 36
- Package: com.example.doan_zaloclone
- Firebase Project: zalo-clone-dd021
- Branch: feature/firebase-setup

## Architecture Pattern

- Repository pattern for data access
- Activity/Fragment with RecyclerView adapters
- Callback-based async operations
- ViewBinding enabled

## Navigation Flow

```
LoginActivity (Launcher)
  ├─> SignUpActivity
  └─> MainActivity
       ├─> HomeFragment (default)
       ├─> ContactFragment
       └─> RoomActivity
```

## Known Issues

- Firestore API must be enabled before first run
- SHA-1 certificate required for production
- Test mode Firestore rules expire after 30 days
