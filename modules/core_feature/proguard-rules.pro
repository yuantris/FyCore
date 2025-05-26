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

## 禁用代码优化
#-dontoptimize
#-printmapping mapping.txt

# 保留类的原始结构，避免被移除或混淆
-keep class io.core.** { *; }
# 或更细粒度控制（例如保留所有 public 方法名）
-keepclassmembernames class io.core.** {
    public *;
}
-keep class  io.core.databinding.* {*;}
# 允许方法体被混淆（例如移除无用代码、优化指令）
-optimizationpasses 5
-allowaccessmodification

# 保留 Kotlin Lambda 生成的类
-keep class io.core.**$$Lambda$* { *; }

# 保留通过反射调用的字段和方法
-keepclassmembers class * {
    public <methods>;
    public <fields>;
}

# 保留 Kotlin 数据类及其构造函数
-keep class * extends kotlin.Metadata { *; }
-keep class kotlin.Metadata { *; }
-keepclassmembers class ** {
    @kotlin.Metadata *;
}
# 保留所有使用 @Keep 注解的类和方法
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers @androidx.annotation.Keep class * { *; }
# 保留所有 Kotlin 编译器生成的代码
-keep class kotlin.jvm.internal.** { *; }
-keep class kotlin.jvm.functions.** { *; }
-keep class kotlin.reflect.** { *; }
-keep class kotlin.coroutines.** { *; }

# 防止反射类被移除（用于 Gson、Kotlin 序列化等）
-keep class com.google.gson.** { *; }
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 避免移除 Parcelable 接口实现
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 混合时不使用大小写混合，混合后的类名为小写
-dontusemixedcaseclassnames

# 指定不去忽略非公共库的类
-dontskipnonpubliclibraryclasses

# 这句话能够使我们的项目混淆后产生映射文件
# 包含有类名->混淆后类名的映射关系
-verbose

# 指定不去忽略非公共库的类成员
-dontskipnonpubliclibraryclassmembers

# 不做预校验，preverify是proguard的四个步骤之一，Android不需要preverify，去掉这一步能够加快混淆速度。
-dontpreverify

# 保留Annotation不混淆
-keepattributes *Annotation*,InnerClasses

# 避免混淆泛型
-keepattributes Signature

# 抛出异常时保留代码行号
-keepattributes SourceFile,LineNumberTable

# 指定混淆是采用的算法，后面的参数是一个过滤器
# 这个过滤器是谷歌推荐的算法，一般不做更改
-optimizations !code/simplification/cast,!field/*,!class/merging/*

# 保留R下面的资源
-keep class **.R$* {*;}

# 保留本地native方法不被混淆
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留在Activity中的方法参数是view的方法，
# 这样以来我们在layout中写的onClick就不会被影响
-keepclassmembers class * extends android.app.Activity{
    public void *(android.view.View);
}

# 保留枚举类不被混淆
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留我们自定义控件（继承自View）不被混淆
-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 保留Parcelable序列化类不被混淆
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留Serializable序列化的类不被混淆
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    !static !transient <fields>;
    !private <fields>;
    !private <methods>;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keep class android.support.v8.renderscript.** { *; }
-keep class androidx.renderscript.** { *; }

## ExoPlayer 反射设置ua 保证该私有变量不被混淆
-keepclassmembers class androidx.media3.datasource.cache.CacheDataSource$Factory {
    *** upstreamDataSourceFactory;
}
## ExoPlayer 如果还不能播放就取消注释这个
# -keep class com.google.android.exoplayer2.** {*;}

# ShapeView：https://github.com/getActivity/ShapeView
-keep class io.core.engine.shape.** {*;}
-keep class io.core.widget.layout.** {*;}
-keep class io.core.widget.view.** {*;}

-dontwarn io.core.engine.livebus.**
-keep class io.core.engine.livebus.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.arch.core.** { *; }

-keep class io.core.common.helper.JsonUltra{ *; }
-keep class io.core.common.helper.track.AppLifecycleTracker { *; }
-keep class io.core.common.helper.track.TurboTracker { *; }
-keep class io.core.common.helper.track.TurboTracker$DefaultLogger { *; }
-keep interface io.core.common.helper.track.TurboTracker$Logger { *; }
-keep interface io.core.common.helper.track.TurboTracker$ExtraInfoProvider { *; }

# 保持继承了MultiState的类不被混淆
-keep public class * extends io.core.engine.multi_state.MultiState { *; }
