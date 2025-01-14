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

## 保留类的原始结构，避免被移除或混淆
#-keep class com.core.libraries.** { *; }
#
## 保留子类和子包，防止相关逻辑被移除
#-keepclassmembers class com.core.libraries.** { *; }
#
## 保留所有协程相关类和 Lambda 类，防止混淆
#-keep class kotlinx.coroutines.** { *; }
#-keep class kotlin.coroutines.** { *; }
#-keep class kotlin.jvm.functions.Function1 { *; }
#-keep class kotlin.coroutines.jvm.internal.** { *; }
#-keep class kotlin.coroutines.Continuation { *; }
#
## 保留 Kotlin Lambda 生成的类
#-keep class com.core.libraries.**$$Lambda$* { *; }
#
## 保留 suspend 函数的签名
#-keepclassmembers class * {
#    suspend <methods>;
#}
#
## 保留 Kotlin 数据类及其构造函数
#-keep class * extends kotlin.Metadata { *; }
#
## 保留所有使用了注解的类和方法
#-keepattributes *Annotation*
#
## 保留泛型信息，防止 Room 和其他库运行时异常
#-keepattributes Signature
#
## Room Database 保留规则
#-keep class androidx.room.** { *; }
#
## 保留 Room 实体类的主构造函数和所有字段
#-keepclassmembers class * extends androidx.room.RoomDatabase {
#    public <init>(...);
#}
#
## 保留 DAOs（Data Access Object）接口
#-keep interface * extends androidx.room.Dao {
#    <methods>;
#}
#
## 保留数据库实体类的构造方法，避免混淆
#-keep class * extends androidx.room.Entity {
#    <init>(...);
#}
#
## 保留 Kotlin 数据类和默认构造函数，避免混淆
#-keep class **.data.** { *; }
#
## 防止反射类被移除（用于 Gson、Kotlin 序列化等）
#-keep class com.google.gson.** { *; }
#-keepclassmembers class ** {
#    @com.google.gson.annotations.SerializedName <fields>;
#}
#
#
## 避免移除 Parcelable 接口实现
#-keepclassmembers class * implements android.os.Parcelable {
#    public static final android.os.Parcelable$Creator *;
#}

# ShapeView：https://github.com/getActivity/ShapeView
-keep class com.hjq.shape.** {*;}

-dontwarn com.core.libraries.engine.livebus.**
-keep class com.core.libraries.engine.livebus.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.arch.core.** { *; }
