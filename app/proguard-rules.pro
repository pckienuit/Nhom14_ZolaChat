# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line number information for debugging stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================
# GENERAL ANDROID RULES
# ============================================================
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ============================================================
# FIREBASE & FIRESTORE
# ============================================================
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep Firebase Firestore model classes (data binding)
-keep class com.example.doan_zaloclone.models.** { *; }
-keepclassmembers class com.example.doan_zaloclone.models.** { *; }

# ============================================================
# RETROFIT & GSON
# ============================================================
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Gson specific classes
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep model classes for JSON serialization
-keep class com.example.doan_zaloclone.models.** { *; }
-keep class com.example.doan_zaloclone.api.** { *; }

# Gson TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ============================================================
# OKHTTP & LOGGING INTERCEPTOR
# ============================================================
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ============================================================
# SOCKET.IO
# ============================================================
-keep class io.socket.** { *; }
-keep class io.socket.client.** { *; }
-keep class io.socket.emitter.** { *; }
-keep class io.socket.engineio.** { *; }
-dontwarn io.socket.**

# ============================================================
# WEBRTC (Stream)
# ============================================================
-keep class org.webrtc.** { *; }
-keep class io.getstream.webrtc.** { *; }
-keepclassmembers class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# Keep WebRTC helper classes
-keep class com.example.doan_zaloclone.utils.WebRtcHelper { *; }
-keep class com.example.doan_zaloclone.service.OngoingCallService { *; }

# ============================================================
# GLIDE
# ============================================================
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.** { *; }
-keep class com.example.doan_zaloclone.MyAppGlideModule { *; }

# ============================================================
# CLOUDINARY
# ============================================================
-keep class com.cloudinary.** { *; }
-keepclassmembers class com.cloudinary.** { *; }
-dontwarn com.cloudinary.**

# ============================================================
# OSMDROID (OpenStreetMap)
# ============================================================
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# ============================================================
# UCROP (Image Cropping)
# ============================================================
-keep class com.yalantis.ucrop.** { *; }
-dontwarn com.yalantis.ucrop.**

# ============================================================
# ZXING (QR Code)
# ============================================================
-keep class com.google.zxing.** { *; }
-keep class com.journeyapps.barcodescanner.** { *; }
-dontwarn com.google.zxing.**

# ============================================================
# ANDROIDVIEWMODEL & LIFECYCLE
# ============================================================
-keep class * extends androidx.lifecycle.ViewModel {
    <init>();
}
-keep class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>();
}

# ============================================================
# REMOVE DEBUG LOGGING IN RELEASE
# ============================================================
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}