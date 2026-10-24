# Keep Media3 ExoPlayer classes
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep Coroutines
-keepnames class kotlinx.coroutines.** { *; }
-keepclassmembers class * extends kotlinx.coroutines.internal.LockFreeLinkedListNode {
    volatile java.lang.Object _next;
    volatile java.lang.Object _prev;
    volatile java.lang.Object _parent;
}

# Keep project domain, data, and UI components
-keep class com.tv.stream.domain.** { *; }
-keep class com.tv.stream.data.** { *; }
-keep class com.tv.stream.ui.components.** { *; }
-keep class com.tv.stream.playback.** { *; }
