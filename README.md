# Zola Chat - Android Messaging Application

A native Android messaging application built with Java, Firebase, and Node.js Backend.

--------------------------------------------------------------------------------
TABLE OF CONTENTS
--------------------------------------------------------------------------------

0. QUICKSTART (RECOMMENDED FOR TESTING)
1. SYSTEM REQUIREMENTS
2. JDK INSTALLATION (Eclipse Adoptium 17)
3. ANDROID STUDIO INSTALLATION
4. NODE.JS AND NPM INSTALLATION
5. FIREBASE CONFIGURATION
6. BACKEND SERVER SETUP
7. ANDROID APP CONFIGURATION
8. BUILD AND RUN
9. BUILD FLAVORS (LOCAL SERVER FALLBACK)
10. PRODUCTION SETUP (OPTIONAL)
11. PROJECT STRUCTURE
12. TROUBLESHOOTING

--------------------------------------------------------------------------------
0. QUICKSTART (RECOMMENDED FOR TESTING)
--------------------------------------------------------------------------------

This section allows you to test the app quickly without any configuration.
The pre-built APK connects to production server (zolachat.site).

PRE-BUILT APK:

    File: ZolaChat-v1.0-production.apk
    Location: Project root folder (D:\DoAn_ZaloClone\)
    Size: ~15MB
    Build: Production Debug (connects to zolachat.site)

TEST ACCOUNT:

    Email: admin@example.com
    Password: 123456
    Role: Administrator

INSTALLATION ON EMULATOR:

Step 1: Start Android Emulator

- Open Android Studio
- Tools > Device Manager
- Click Play button on any emulator (or create one with API 28+)
- Wait for emulator to fully boot

Step 2: Install APK via Command Line

Open PowerShell/Terminal and run:

    cd D:\DoAn_ZaloClone
    adb install ZolaChat-v1.0-production.apk

Expected output:
    Performing Streamed Install
    Success

Step 3: Install APK via Drag & Drop (Alternative)

- Open File Explorer
- Navigate to D:\DoAn_ZaloClone\
- Drag ZolaChat-v1.0-production.apk onto the emulator window
- Wait for installation to complete

Step 4: Launch and Login

- Find "Zola Chat" app in emulator app drawer
- Open the app
- Login with test account:
    Email: admin@example.com
    Password: 123456

NOTE: 
- Admin page: zolachat.site/admin/ (login with above account)
- If production server (zolachat.site) is down, see section 9 for
running local server with development build flavor.

--------------------------------------------------------------------------------
1. SYSTEM REQUIREMENTS
--------------------------------------------------------------------------------

REQUIRED SOFTWARE:

- Windows 10/11 (64-bit)
- Eclipse Adoptium JDK 17 (Temurin)
- Android Studio Hedgehog (2023.1.1) or newer
- Node.js 18.x or 20.x (LTS)
- Git

MINIMUM HARDWARE:

- RAM: 8GB (16GB recommended)
- Free disk space: 20GB
- CPU: Intel Core i5 or equivalent

TEST DEVICES:

- Android Emulator (API 28+) or
- Physical Android device (Android 9.0 or higher)

--------------------------------------------------------------------------------
2. JDK INSTALLATION (Eclipse Adoptium 17)
--------------------------------------------------------------------------------

STEP 1: Download Eclipse Adoptium JDK 17

- Visit: https://adoptium.net/temurin/releases/
- Select:
  + Operating System: Windows
  + Architecture: x64
  + Package Type: JDK
  + Version: 17 (LTS)
- Click ".msi" to download installer

STEP 2: Install JDK

- Run the downloaded .msi file
- Check "Set JAVA_HOME variable" during installation
- Check "Add to PATH" during installation
- Complete installation

STEP 3: Verify Installation

Open new PowerShell window and run:

    java -version

Expected output:
    openjdk version "17.0.x" 2024-xx-xx
    OpenJDK Runtime Environment Temurin-17.0.x+x (build 17.0.x+x)
    OpenJDK 64-Bit Server VM Temurin-17.0.x+x (build 17.0.x+x, mixed mode)

Verify JAVA_HOME:

    echo $env:JAVA_HOME

Expected output:
    C:\Program Files\Eclipse Adoptium\jdk-17.0.x.x-hotspot

--------------------------------------------------------------------------------
3. ANDROID STUDIO INSTALLATION
--------------------------------------------------------------------------------

STEP 1: Download Android Studio

- Visit: https://developer.android.com/studio
- Download the latest version for Windows

STEP 2: Install

- Run the downloaded .exe file
- Select "Standard" installation
- Accept all licenses
- Wait for Android Studio to download SDK and tools

STEP 3: Configure SDK

Open Android Studio > Settings > Languages & Frameworks > Android SDK

"SDK Platforms" tab - select:
- Android 14.0 (API 34)
- Android 9.0 (API 28)

"SDK Tools" tab - select:
- Android SDK Build-Tools 34
- Android SDK Command-line Tools
- Android Emulator
- Android SDK Platform-Tools
- Google Play services

Click "Apply" to download.

STEP 4: Configure Environment Variables

Open System Properties > Environment Variables

Add to "System variables":
- ANDROID_HOME = C:\Users\<username>\AppData\Local\Android\Sdk
- JAVA_HOME = C:\Program Files\Eclipse Adoptium\jdk-17.0.x.x-hotspot
  (Already set if selected during JDK installation)

Add to "Path":
- %ANDROID_HOME%\platform-tools
- %ANDROID_HOME%\tools
- %ANDROID_HOME%\tools\bin

Restart your computer.

--------------------------------------------------------------------------------
4. NODE.JS AND NPM INSTALLATION
--------------------------------------------------------------------------------

STEP 1: Download Node.js

- Visit: https://nodejs.org/
- Download the LTS version (20.x recommended)

STEP 2: Install

- Run the downloaded .msi file
- Select all default options
- Check "Automatically install necessary tools"

STEP 3: Verify installation

Open Command Prompt or PowerShell:

    node --version
    npm --version

Expected output:
    v20.x.x
    10.x.x

--------------------------------------------------------------------------------
5. FIREBASE CONFIGURATION
--------------------------------------------------------------------------------

STEP 1: Create Firebase Project

- Visit: https://console.firebase.google.com/
- Sign in with Google account
- Click "Create a project"
- Name project: "zalo-clone" (or any name)
- Disable Google Analytics (not needed)
- Click "Create project"

STEP 2: Enable Authentication

- Go to "Authentication" menu
- Click "Get started"
- "Sign-in method" tab > "Email/Password" > Enable > Save

STEP 3: Create Firestore Database

- Go to "Firestore Database" menu
- Click "Create database"
- Select "Start in test mode"
- Select location: asia-southeast1 (Singapore)
- Click "Enable"

STEP 4: Register Android App

- Go to Project Settings (gear icon)
- Scroll to "Your apps"
- Click Android icon
- Fill in:
  + Package name: com.example.doan_zaloclone
  + App nickname: Zalo Clone
  + Debug signing certificate SHA-1: (see instructions below)
- Click "Register app"

STEP 5: Get SHA-1 Certificate

Open terminal in Android Studio (View > Tool Windows > Terminal):

    cd android
    .\gradlew signingReport

Or run directly:

    keytool -list -v -keystore "%USERPROFILE%\.android\debug.keystore" -alias androiddebugkey -storepass android -keypass android

Copy the SHA-1 fingerprint and paste into Firebase Console.

STEP 6: Download google-services.json

- After registering app, click "Download google-services.json"
- Copy file to folder: app/
- Full path: DoAn_ZaloClone/app/google-services.json

STEP 7: Create Service Account Key (for Backend)

- Go to Project Settings > Service accounts
- Click "Generate new private key"
- Click "Generate key"
- Rename file to: serviceAccountKey.json
- Copy to folder: server/

--------------------------------------------------------------------------------
6. BACKEND SERVER SETUP
--------------------------------------------------------------------------------

STEP 1: Open terminal and navigate to server folder

    cd D:\DoAn_ZaloClone\server

STEP 2: Install dependencies

    npm install

STEP 3: Create .env file

Create .env file in server/ folder with content:

    PORT=3000
    NODE_ENV=development
    ALLOWED_ORIGINS=http://localhost:3000,http://10.0.2.2:3000
    FIREBASE_SERVICE_ACCOUNT_PATH=./serviceAccountKey.json
    REDIS_HOST=localhost
    REDIS_PORT=6379
    REDIS_PASSWORD=
    RATE_LIMIT_WINDOW_MS=60000
    RATE_LIMIT_MAX_REQUESTS=1000
    WS_PING_INTERVAL=25000
    WS_PING_TIMEOUT=60000

STEP 4: Verify serviceAccountKey.json

Ensure serviceAccountKey.json has been copied to server/ folder.
This file is downloaded from Firebase Console (step 4.7).

STEP 5: Run server

    npm run dev

Expected output:
    Server running on port 3000
    Firebase Admin initialized successfully
    WebSocket server initialized

Keep this terminal open while testing the app.

--------------------------------------------------------------------------------
7. ANDROID APP CONFIGURATION
--------------------------------------------------------------------------------

STEP 1: Clone or copy project

    git clone <repository-url> D:\DoAn_ZaloClone

Or copy project folder to D:\DoAn_ZaloClone

STEP 2: Open project in Android Studio

- Open Android Studio
- Select "Open"
- Choose folder D:\DoAn_ZaloClone
- Wait for Gradle sync to complete (may take 5-10 minutes first time)

STEP 3: Configure local.properties

Open local.properties file in project root folder.
Add the following lines (replace with your information):

    # Android SDK path (auto-generated by Android Studio)
    sdk.dir=C\:\\Users\\<username>\\AppData\\Local\\Android\\Sdk

    # Cloudinary Configuration (register at https://cloudinary.com/)
    CLOUDINARY_CLOUD_NAME=your_cloud_name
    CLOUDINARY_API_KEY=your_api_key
    CLOUDINARY_API_SECRET=your_api_secret

    # TURN Server Credentials (for video call - can leave empty if not using)
    TURN_SERVER_HOST=
    TURN_SERVER_USERNAME=
    TURN_SERVER_PASSWORD=

NOTE: If you don't have a Cloudinary account, register free at:
https://cloudinary.com/users/register/free

STEP 4: Verify google-services.json

Ensure file app/google-services.json exists and matches your Firebase project.

STEP 5: Sync Gradle

- Click "Sync Now" on yellow notification bar
- Or: File > Sync Project with Gradle Files
- Wait until complete with no errors

--------------------------------------------------------------------------------
8. BUILD AND RUN
--------------------------------------------------------------------------------

METHOD 1: Using Android Studio (Recommended)

Step 1: Create Emulator (if not exists)

- Tools > Device Manager
- Click "Create device"
- Select "Pixel 6" or similar device
- Click "Next"
- Select system image "API 34" (download if not available)
- Click "Next" > "Finish"

Step 2: Run application

- Select device/emulator from dropdown on toolbar
- Select build variant: "developmentDebug"
  (Build > Select Build Variant > select "developmentDebug")
- Click Run button (green play icon)
- Wait for build and install (first time takes about 3-5 minutes)

METHOD 2: Using Command Line

Open terminal in project folder:

    # Build APK (Development Debug)
    .\gradlew assembleDevelopmentDebug

    # Install to emulator/device
    .\gradlew installDevelopmentDebug

    # Build and run
    .\gradlew installDevelopmentDebug
    adb shell am start -n com.example.doan_zaloclone/.ui.login.LoginActivity

BUILD VARIANTS:

- developmentDebug: Connect to localhost (10.0.2.2:3000)
- developmentRelease: Localhost + minification
- productionDebug: Connect to production server (zolachat.site)
- productionRelease: Production + minification + signing

--------------------------------------------------------------------------------
9. BUILD FLAVORS (LOCAL SERVER FALLBACK)
--------------------------------------------------------------------------------

This project uses build flavors to switch between server environments.
Use this when the production server (zolachat.site) is unavailable.

UNDERSTANDING BUILD FLAVORS:

The app has 2 environment flavors configured in app/build.gradle.kts:

    development:
        - API_BASE_URL: http://10.0.2.2:3000/api/
        - SOCKET_URL: http://10.0.2.2:3000
        - Used for: Local development with local Node.js server
        - Note: 10.0.2.2 is Android emulator's alias for host localhost

    production:
        - API_BASE_URL: https://zolachat.site/api/
        - SOCKET_URL: https://zolachat.site
        - Used for: Testing with live production server

WHEN TO USE DEVELOPMENT FLAVOR:

- Production server (zolachat.site) is down or unreachable
- You want to test with your own local server
- You want to debug API requests locally
- You're developing new backend features

HOW TO RUN WITH LOCAL SERVER:

Step 1: Start local backend server

    cd D:\DoAn_ZaloClone\server
    npm install          (first time only)
    npm run dev

    Ensure you see: "Server running on port 3000"

Step 2: Switch build variant in Android Studio

    - Build > Select Build Variant
    - Change from "productionDebug" to "developmentDebug"
    - Wait for Gradle sync

Step 3: Run the app

    - Click Run button (green play icon)
    - App will now connect to localhost:3000

COMMAND LINE BUILD:

    # Build development flavor
    .\gradlew assembleDevelopmentDebug
    .\gradlew installDevelopmentDebug

    # Build production flavor
    .\gradlew assembleProductionDebug
    .\gradlew installProductionDebug

SWITCHING BACK TO PRODUCTION:

    - Build > Select Build Variant
    - Change to "productionDebug"
    - Run the app

--------------------------------------------------------------------------------
10. PRODUCTION SETUP (OPTIONAL)
--------------------------------------------------------------------------------

Only needed when deploying to Play Store or production server.

STEP 1: Create Release Keystore

Open terminal in project folder:

    mkdir keystore
    keytool -genkey -v -keystore keystore/release.jks -alias zaloclone -keyalg RSA -keysize 2048 -validity 10000

Enter information when prompted:
- Keystore password: (remember this password)
- Key password: (can be same as keystore password)
- Name, organization, city, country...

STEP 2: Configure local.properties

Add to end of local.properties file:

    RELEASE_STORE_FILE=keystore/release.jks
    RELEASE_STORE_PASSWORD=your_keystore_password
    RELEASE_KEY_ALIAS=zaloclone
    RELEASE_KEY_PASSWORD=your_key_password

STEP 3: Build Production APK

    .\gradlew assembleProductionRelease

APK will be created at:
app/build/outputs/apk/production/release/app-production-release.apk

--------------------------------------------------------------------------------
11. PROJECT STRUCTURE
--------------------------------------------------------------------------------

DoAn_ZaloClone/
|
|-- app/                          # Android application
|   |-- src/main/
|   |   |-- java/.../             # Java source code
|   |   |   |-- api/              # Retrofit API interfaces
|   |   |   |-- models/           # Data models
|   |   |   |-- repository/       # Data repositories
|   |   |   |-- service/          # Background services
|   |   |   |-- ui/               # Activities & Fragments
|   |   |   |-- utils/            # Helper classes
|   |   |   |-- viewmodel/        # ViewModels (MVVM)
|   |   |   +-- websocket/        # WebSocket handlers
|   |   |-- res/                  # Resources (layouts, drawables, strings)
|   |   +-- AndroidManifest.xml
|   |-- build.gradle.kts          # App-level build config
|   +-- google-services.json      # Firebase config
|
|-- server/                       # Node.js Backend
|   |-- src/
|   |   |-- index.js              # Entry point
|   |   |-- routes/               # API routes
|   |   |-- middleware/           # Express middleware
|   |   +-- websocket/            # Socket.IO handlers
|   |-- .env                      # Environment variables
|   |-- package.json              # Dependencies
|   +-- serviceAccountKey.json    # Firebase Admin SDK key
|
|-- admin-web/                    # Admin dashboard (HTML/JS)
|
|-- docs/                         # Documentation (92 files)
|
|-- landing-page/                 # Landing page website
|
|-- local.properties              # Local config (API keys, SDK path)
|-- build.gradle.kts              # Root build config
+-- README.md                     # This file

--------------------------------------------------------------------------------
12. TROUBLESHOOTING
--------------------------------------------------------------------------------

ERROR 1: Gradle sync failed

Cause: Missing SDK or tools
Solution:
- Open SDK Manager (Tools > SDK Manager)
- Install SDK Platform 34 and Build Tools 34
- File > Invalidate Caches > Invalidate and Restart

ERROR 2: Cannot find symbol BuildConfig

Cause: Gradle not synced after editing local.properties
Solution:
- Build > Clean Project
- Build > Rebuild Project

ERROR 3: Firebase: No Firebase App

Cause: Missing or incorrect google-services.json
Solution:
- Verify file app/google-services.json exists
- Ensure package name in file matches app package
- Re-download file from Firebase Console if needed

ERROR 4: Server connection refused (10.0.2.2:3000)

Cause: Backend server not running
Solution:
- Open new terminal
- cd server
- npm run dev
- Ensure you see "Server running on port 3000"

ERROR 5: Emulator cannot connect to internet

Cause: DNS or proxy settings
Solution:
- Open Emulator settings
- Settings > Proxy > No proxy
- Cold boot emulator (Device Manager > Cold Boot Now)

ERROR 6: Build slow or hangs

Cause: Low RAM or Gradle daemon error
Solution:
- Close other applications to free RAM
- Run: .\gradlew --stop
- Delete .gradle folder in user directory
- Increase Gradle RAM in gradle.properties:
  org.gradle.jvmargs=-Xmx4096m

ERROR 7: Cloudinary upload failed

Cause: Wrong API credentials
Solution:
- Verify CLOUDINARY_CLOUD_NAME, API_KEY, API_SECRET in local.properties
- Ensure no extra whitespace
- Sync Gradle again

ERROR 8: SHA-1 mismatch

Cause: SHA-1 in Firebase doesn't match debug keystore
Solution:
- Get new SHA-1 using signingReport command
- Update in Firebase Console > Project Settings > Your apps
- Re-download google-services.json

--------------------------------------------------------------------------------
CONTACT INFORMATION
--------------------------------------------------------------------------------

- Repository: https://github.com/pckien/DoAn_ZaloClone
- Firebase Project: zalo-clone-dd021
- Production URL: https://zolachat.site

--------------------------------------------------------------------------------
LAST UPDATED: 27/12/2025
--------------------------------------------------------------------------------
