plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

import java.util.Properties
import java.io.FileInputStream

android {
    namespace = "com.example.doan_zaloclone"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.doan_zaloclone"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Support for 16KB page size devices
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
        
        // Inject API keys from local.properties (secure, not committed to Git)
        val localProperties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { localProperties.load(it) }
        }
        
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", 
            "\"${localProperties.getProperty("CLOUDINARY_CLOUD_NAME", "")}\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", 
            "\"${localProperties.getProperty("CLOUDINARY_API_KEY", "")}\"")
        buildConfigField("String", "CLOUDINARY_API_SECRET", 
            "\"${localProperties.getProperty("CLOUDINARY_API_SECRET", "")}\"")
        buildConfigField("String", "TURN_SERVER_HOST", 
            "\"${localProperties.getProperty("TURN_SERVER_HOST", "")}\"")
        buildConfigField("String", "TURN_SERVER_USERNAME", 
            "\"${localProperties.getProperty("TURN_SERVER_USERNAME", "")}\"")
        buildConfigField("String", "TURN_SERVER_PASSWORD", 
            "\"${localProperties.getProperty("TURN_SERVER_PASSWORD", "")}\"")
    }

    // ============================================================
    // SIGNING CONFIGS - Production release signing
    // ============================================================
    signingConfigs {
        create("release") {
            // Read keystore properties from local.properties
            val localProperties = Properties()
            val localPropertiesFile = rootProject.file("local.properties")
            if (localPropertiesFile.exists()) {
                localPropertiesFile.inputStream().use { localProperties.load(it) }
            }
            
            storeFile = file(localProperties.getProperty("RELEASE_STORE_FILE", "keystore/release.jks"))
            storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD", "")
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS", "")
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD", "")
        }
    }

    buildTypes {
        release {
            // Apply signing config
            signingConfig = signingConfigs.getByName("release")
            
            // Enable code minification & obfuscation with R8
            isMinifyEnabled = true
            // Enable resource shrinking (removes unused resources)
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        
        debug {
            // Keep debug builds fast - no minification
            isMinifyEnabled = false
        }
    }
    
    // Product flavors for server environment switching
    flavorDimensions += "environment"
    
    productFlavors {
        create("development") {
            dimension = "environment"
            versionNameSuffix = "-dev"
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:3000/api/\"")
            buildConfigField("String", "SOCKET_URL", "\"http://10.0.2.2:3000\"")
            buildConfigField("String", "VPS_BASE_URL", "\"http://10.0.2.2:3000\"")
        }
        
        create("production") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"https://zolachat.site/api/\"")
            buildConfigField("String", "SOCKET_URL", "\"https://zolachat.site\"")
            buildConfigField("String", "VPS_BASE_URL", "\"https://zolachat.site\"")
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true  // Enable BuildConfig generation
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    
    // Lifecycle process for app-level presence tracking
    implementation("androidx.lifecycle:lifecycle-process:2.7.0")
    
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.glide)

    // WebRTC
    implementation("io.getstream:stream-webrtc-android:1.3.10")

    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")  // For AppGlideModule
    
    // ExifInterface for image rotation
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    
    // SwipeRefreshLayout for pull-to-refresh
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    
    // Cloudinary for image uploads
    implementation("com.cloudinary:cloudinary-android:3.0.2")
    
    // Firebase BoM - quản lý versions tự động
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    
    // OpenStreetMap - osmdroid (FREE, no API key needed)
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    
    // Location services (FREE)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    
    // OkHttp for HTTP requests (background removal API)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Socket.IO for WebSocket real-time messaging
    implementation("io.socket:socket.io-client:2.1.0")
    
    // UCrop for image cropping in sticker creation
    implementation("com.github.yalantis:ucrop:2.2.8")
    
    // ZXing for QR code scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.3")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    
}