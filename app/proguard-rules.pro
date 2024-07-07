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

#-dontskipnonpubliclibraryclasses # 不忽略非公共的库类
#-optimizationpasses 5            # 代码混淆压缩比，在0~7之间，默认为5，一般不做修改
#-dontusemixedcaseclassnames      # 是否使用大小写混合
#-dontpreverify                   # 混淆时是否做预校验 不做预校验，preverify是proguard的四个步骤之一，Android不需要preverify，去掉这一步能够加快混淆速度。
#-verbose                         # 混淆时是否记录日志 这句话能够使我们的项目混淆后产生映射文件
#-keepattributes *Annotation*,InnerClasses # 保留Annotation不混淆
#-ignorewarning                   # 忽略警告
#-dontoptimize                    # 优化不优化输入的类文件
#-dontskipnonpubliclibraryclassmembers # 指定不去忽略非公共库的类成员

#-optimizations !code/simplification/arithmetic,!field/*,!class/merging/*  # 指定混淆是采用的算法，后面的参数是一个过滤器.这个过滤器是谷歌推荐的算法，一般不做更改

## 保留我们使用的四大组件，自定义的Application等等这些类不被混淆，因为这些子类都有可能被外部调用
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class * extends android.app.Fragment

## 保留support下的所有类及其内部类
#-keep class android.support.** {*;}

## 泛型
#-keepattributes Signature

## 保留R下面的资源
#-keep class **.R$* {*;}

## 保留枚举类不被混淆
#-keepclassmembers enum * {
#    public static **[] values();
#    public static ** valueOf(java.lang.String);
#}

-keep class com.sdt.diagnose.Device.** {*;}
-keep class com.sdt.diagnose.command.** {*;}
-keep class com.sdt.diagnose.common.configuration.** {*;}
-keep class com.sdt.diagnose.common.bean.LogResponseBean { private *; }
-keep class com.sdt.diagnose.common.bean.ShortMessageBean { private *; }
-keep class com.sdt.diagnose.common.bean.LocationInfo { private *; }

-keep class com.sdt.opentr369.OpenTR369Native {*;}

-keep class com.sdt.android.tr369.Bean.MqttConfigsResponseBean { private *; }
-keep class com.sdt.android.tr369.Bean.CaCertBean { private *; }
-keep class com.sdt.android.tr369.Bean.ClientCertBean { private *; }

-keep class com.realtek.hardware.** {*;}

-keep class com.google.gson.reflect.TypeToken {*;}

-dontwarn android.app.PropertyInvalidatedCache$AutoCorker
-dontwarn android.app.PropertyInvalidatedCache
-dontwarn android.hardware.display.VirtualDisplayConfig
-dontwarn android.system.Int32Ref
-dontwarn com.android.okhttp.internalandroidapi.HttpURLConnectionFactory
-dontwarn com.android.org.conscrypt.TrustManagerImpl
-dontwarn com.droidlogic.app.OutputModeManager
-dontwarn com.droidlogic.app.SystemControlManager
-dontwarn com.google.j2objc.annotations.RetainedWith
-dontwarn com.google.j2objc.annotations.Weak
-dontwarn dalvik.system.BlockGuard$VmPolicy
-dontwarn dalvik.system.CloseGuard
-dontwarn javax.lang.model.SourceVersion
-dontwarn libcore.icu.LocaleData
-dontwarn libcore.util.NativeAllocationRegistry
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.conscrypt.ConscryptHostnameVerifier
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
