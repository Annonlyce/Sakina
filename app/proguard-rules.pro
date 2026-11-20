# Keep Moshi generated adapters and Retrofit service interfaces
-keep class com.example.data.network.** { *; }
-keep class com.example.data.repository.*Json* { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}
-keepattributes Signature,InnerClasses,EnclosingMethod,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# Keep Room database and entities
-keep class * extends androidx.room.RoomDatabase
-keep class com.example.data.local.db.** { *; }

# Keep Media3 PlaybackService and Worker classes referenced from AndroidManifest / WorkManager
-keep class com.example.player.SakinaPlaybackService { *; }
-keep class com.example.downloads.SurahDownloadWorker { *; }

# Preserve source line numbers for crash diagnostics
-keepattributes SourceFile,LineNumberTable
