# Preserve Line Numbers for Crash Logs
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Retrofit Keep Rules
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**

# Gson Keep Rules
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }

# Keep Network and Data Models (Serialized API & Entity Classes)
-keep class com.ntando.ivu.network.** { *; }
-keep class com.ntando.ivu.data.entity.** { *; }
-keep class com.ntando.ivu.data.model.** { *; }

# Room Keep Rules
-keep class * extends androidx.room.RoomDatabase
-keep class * implements androidx.room.RoomDatabase
-dontwarn androidx.room.**

# OkHttp & Coroutines
-dontwarn okhttp3.**
-dontwarn okio.**
