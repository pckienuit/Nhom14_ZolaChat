import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.services)
}

// ĐÚNG: Logic đọc file properties được đặt ở cấp cao nhất của script.
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists() && localPropertiesFile.isFile) {
    FileInputStream(localPropertiesFile).use {
        localProperties.load(it)
    }
}

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

        // Lấy API key từ properties và tạo build config field
        val youtubeApiKey = localProperties.getProperty("YOUTUBE_API_KEY", "") // Cung cấp giá trị mặc định
        buildConfigField("String", "YOUTUBE_API_KEY", "\"$youtubeApiKey\"")
        
        // Support for 16KB page size devices
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true // BẬT LẠI TÍNH NĂNG BUILDCONFIG
    }
    packagingOptions {
        exclude("META-INF/DEPENDENCIES")
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
    
    implementation("com.google.apis:google-api-services-youtube:v3-rev20210915-1.32.1")
    implementation("com.google.http-client:google-http-client-android:1.41.0")
    implementation("com.google.api-client:google-api-client-gson:1.32.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // Thư viện bo tròn ảnh (CircleImageView) - Cần cho Newsfeed
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // (Tùy chọn) Thư viện hiển thị thời gian kiểu "vừa xong", "1 giờ trước"
    implementation("org.ocpsoft.prettytime:prettytime:5.0.4.Final")


}