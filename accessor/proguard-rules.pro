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

#-keep class com.sdt.diagnose.Device.** {*;}
#-keep class com.sdt.diagnose.command.** {*;}
#-keep class com.sdt.diagnose.common.configuration.** {*;}

#-keep class com.sdt.opentr369.OpenTR369Native {*;}

-dontwarn android.app.PropertyInvalidatedCache$AutoCorker
-dontwarn android.app.PropertyInvalidatedCache
-dontwarn android.content.pm.PackageManager$OnChecksumsReadyListener
-dontwarn android.content.pm.PackageManager$Property
-dontwarn android.hardware.display.DeviceProductInfo
-dontwarn android.hardware.display.VirtualDisplayConfig
-dontwarn android.system.Int32Ref
-dontwarn com.android.okhttp.internalandroidapi.HttpURLConnectionFactory
-dontwarn dalvik.system.CloseGuard
-dontwarn libcore.icu.LocaleData
-dontwarn vendor.amlogic.hardware.systemcontrol.V1_0.ISystemControlCallback
-dontwarn vendor.amlogic.hardware.systemcontrol.V1_0.SourceInputParam
-dontwarn vendor.amlogic.hardware.systemcontrol.V1_1.ISystemControl
