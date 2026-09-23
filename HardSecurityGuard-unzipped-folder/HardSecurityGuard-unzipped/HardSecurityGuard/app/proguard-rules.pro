# Keep Retrofit/OkHttp/Gson model classes used for Cloudflare Worker sync
-keep class com.pkfuturegkgs.hardsecurityguard.data.remote.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# Room
-keep class androidx.room.** { *; }

# Retrofit / OkHttp (standard rules)
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keepattributes Exceptions
