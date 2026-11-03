# ============================================================
# ProGuard / R8 Rules for Astro Eleven App
# Play Store Release Build - Optimized for Google Play DEX Compliance
# ============================================================

# ---- General Android & Attributes ----
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ---- Firebase & Google Play Services ----
# (Allow R8 to optimize & obfuscate; consumer rules embedded in AAR will handle keep rules)
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ---- App Core Services & Entry Points ----
-keep class com.astroeleven.app.FCMService
-keep class com.astroeleven.app.CallForegroundService
-keep class com.astroeleven.app.AstrologerApp
-keep public class * extends android.app.Activity
-keep public class * extends androidx.fragment.app.Fragment

# ---- OkHttp ----
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**

# ---- Retrofit ----
-dontwarn retrofit2.**
-keepattributes Exceptions
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# ---- Gson ----
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep data/model classes used with Gson reflection
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# ---- Socket.IO ----
-keep class io.socket.** { *; }
-dontwarn io.socket.**
-keep class org.json.** { *; }
-keep class io.socket.client.** { *; }
-keep class io.socket.engineio.** { *; }
-dontwarn io.socket.engineio.**

# ---- WebRTC (Stream) ----
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**
-keep class io.getstream.** { *; }
-dontwarn io.getstream.**

# ---- PhonePe SDK ----
-keep class com.phonepe.** { *; }
-dontwarn com.phonepe.**
-keep class phonepe.** { *; }
-dontwarn phonepe.**

# ---- Glide ----
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
    *** rewind();
}
-dontwarn com.bumptech.glide.**

# ---- Coil (Compose image loading) ----
-dontwarn coil.**

# ---- Room Database ----
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.**

# ---- Jetpack Compose ----
-dontwarn androidx.compose.**

# ---- Kotlin ----
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Metadata {
    public <methods>;
}

# ---- Coroutines ----
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ---- AndroidX & Material (Let standard AAR rules govern) ----
-dontwarn com.google.android.material.**
-dontwarn androidx.**

# ---- Security Crypto ----
-dontwarn androidx.security.crypto.**

# ---- CircleImageView ----
-keep class de.hdodenhof.circleimageview.** { *; }

# ---- Keep all model/data classes in the app for JSON serialization ----
-keep class com.astroeleven.app.data.** { *; }
-keep class com.astroeleven.app.model.** { *; }
-keep class com.astroeleven.app.network.** { *; }
-keep class com.astroeleven.app.ui.chart.** { *; }
-keepclassmembers class com.astroeleven.app.ui.chart.** { *; }
-keep class com.astroeleven.app.ui.horoscope.** { *; }
-keepclassmembers class com.astroeleven.app.ui.horoscope.** { *; }
-keep class com.astroeleven.app.data.api.** { *; }
-keepclassmembers class com.astroeleven.app.data.api.** { *; }

# ---- Prevent R8 from removing JavascriptInterface ----
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ---- Razorpay SDK ----
-keep class com.razorpay.** { *; }
-dontwarn com.razorpay.**
-dontwarn com.razorpay.RzpAssist
-keep class com.razorpay.RzpAssist { *; }

# ---- Suppress external build warnings ----
-dontwarn java.lang.invoke.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn javax.security.**
-dontwarn com.sun.**
-dontwarn org.checkerframework.**
