# Keep AI interact classes
-keep class com.chatai.aiinteract.** { *; }
-keep class com.chatai.aiinteract.models.** { *; }
-keep class com.chatai.aiinteract.callback.** { *; }

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }

# Gson
-keep class com.google.gson.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
