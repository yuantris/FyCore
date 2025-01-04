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

# ShapeView：https://github.com/getActivity/ShapeView
-keep class com.hjq.shape.** {*;}

# Room Database 保留规则
-keep class androidx.room.** { *; }

# 保留 Room 实体类的主构造函数和所有字段
-keepclassmembers class * extends androidx.room.RoomDatabase {
    public <init>(...);
}

# 保留 DAOs（Data Access Object）接口
-keep interface * extends androidx.room.Dao {
    <methods>;
}

# 保留数据库实体类的构造方法，避免混淆
-keep class * extends androidx.room.Entity {
    <init>(...);
}

# 保留 Kotlin 数据类和默认构造函数，避免混淆
-keep class **.data.** { *; }
