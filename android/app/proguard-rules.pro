# OkHttp
-dontwarn okhttp3.internal.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.wallkraft.app.data.api.**$$serializer { *; }
-keepclassmembers class com.wallkraft.app.data.api.** { *** Companion; }
-keepclasseswithmembers class com.wallkraft.app.data.api.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.wallkraft.app.domain.model.**$$serializer { *; }
-keepclassmembers class com.wallkraft.app.domain.model.** { *** Companion; }
-keepclasseswithmembers class com.wallkraft.app.domain.model.** { kotlinx.serialization.KSerializer serializer(...); }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-dontwarn androidx.room.paging.**

# Coil
-dontwarn coil3.**

# security-crypto / Tink — errorprone annotations are compile-only
-dontwarn com.google.errorprone.annotations.**
