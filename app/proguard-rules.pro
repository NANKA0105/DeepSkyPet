# Keep WebView JS interface
-keepclassmembers class com.deepsky.pet.service.OverlayService$* {
    *;
}
# Keep Supabase sync
-keep class com.deepsky.pet.sync.** { *; }
