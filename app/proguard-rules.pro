# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\lslyf\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt

# Keep Shizuku classes
-keep class rikka.shizuku.** { *; }

# Keep Room entities
-keep class com.privatednsguard.data.db.** { *; }
