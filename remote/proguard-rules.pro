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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

-keep interface android.view.IRotationWatcher { *; }
-keep interface android.view.InputEvent { *; }
-keep interface android.view.Surface { *; }
-keep interface android.content.IOnPrimaryClipChangedListener { *; }
-keep class android.content.ClipData { *; }
-keep class android.graphics.Rect { *; }

-keep class org.webrtc.** { *; }

-dontwarn android.content.IOnPrimaryClipChangedListener$Stub
-dontwarn android.content.IOnPrimaryClipChangedListener
-dontwarn android.view.IRotationWatcher$Stub
-dontwarn android.view.IRotationWatcher
