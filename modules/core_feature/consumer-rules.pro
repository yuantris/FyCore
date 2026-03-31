# Consumer ProGuard Rules for core_feature
# 此文件仅包含库正常运行必需的规则
# 不要放置全局选项（如 -dontpreverify、-optimizations、-verbose）

# ============================================
# 保留属性（Kotlin内联函数、协程必需）
# ============================================
-keepattributes *Annotation*,InnerClasses,Signature,EnclosingMethod
-keepattributes SourceFile,LineNumberTable
-keepattributes LocalVariableTable,LocalVariableTypeTable
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations,RuntimeInvisibleParameterAnnotations
-keepattributes RuntimeVisibleTypeAnnotations,RuntimeInvisibleTypeAnnotations
-keepattributes MethodParameters
-keepattributes Exceptions
-keepattributes Deprecated
-keepattributes AnnotationDefault

# ============================================
# Kotlin核心保留
# ============================================
# Kotlin元数据（反射、序列化必需）
-keep class kotlin.Metadata { *; }

# Kotlin JVM内部类
-keep class kotlin.jvm.internal.** { *; }
-keep class kotlin.jvm.internal.Intrinsics { *; }

# Kotlin函数接口（Lambda必需）
-keep class kotlin.jvm.functions.** { *; }

# Kotlin反射
-keep class kotlin.reflect.** { *; }

# Kotlin协程
-keep class kotlin.coroutines.** { *; }
-keep class kotlin.coroutines.intrinsics.** { *; }
-keep class kotlin.coroutines.jvm.internal.** { *; }
-keep class kotlinx.coroutines.** { *; }

# Kotlin编译器生成的辅助类
-keep class **.*Mappings { *; }
-keep class **.*WhenMappings { *; }
-keep class **.$WhenMappings { *; }

# Kotlin内联函数生成的类
-keep class **.*Inlined { *; }
-keep class **.*$Inline { *; }

# ============================================
# io.core公共API
# ============================================
-keep class io.core.** { *; }
-keep class io.core.*Kt { *; }

# ============================================
# DataBinding / ViewBinding
# ============================================
-keep class io.core.databinding.* {*;}
-keep class **BR { *; }

# ============================================
# Lambda表达式
# ============================================
-keep class io.core.**$$Lambda$* { *; }
-keep class **.*$$Lambda$* { *; }

# ============================================
# @Keep注解
# ============================================
-keep @androidx.annotation.Keep class * { *; }
-keepclassmembers @androidx.annotation.Keep class * { *; }

# ============================================
# Gson序列化
# ============================================
-keep class com.google.gson.** { *; }
-keepclassmembers class ** {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ============================================
# Kotlinx Serialization
# ============================================
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1>$Companion {
    *** serializer(...);
}
-if @kotlinx.serialization.Serializable class ** {
    static **$Companion Companion;
}
-keepclassmembers class <2>$Companion {
    *** serializer(...);
}
-keepattributes RuntimeVisibleAnnotations, AnnotationDefault

# ============================================
# Parcelable / Serializable
# ============================================
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
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

# ============================================
# 自定义View（XML反射构造）
# ============================================
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers public class * extends android.view.View {
    void set*(***);
    *** get*();
}

# ============================================
# ShapeView等引擎组件
# ============================================
-keep class io.core.engine.shape.** {*;}
-keep class io.core.widget.layout.** {*;}
-keep class io.core.widget.view.** {*;}

# ============================================
# LiveBus
# ============================================
-dontwarn io.core.engine.livebus.**
-keep class io.core.engine.livebus.** { *; }

# ============================================
# Lifecycle / Arch
# ============================================
-keep class androidx.lifecycle.** { *; }
-keep class androidx.arch.core.** { *; }

# ============================================
# 反射相关类
# ============================================
-keep class io.core.common.helper.JsonUltra { *; }
-keep class io.core.common.helper.track.AppLifecycleTracker { *; }
-keep class io.core.common.helper.track.TurboTracker { *; }
-keep class io.core.common.helper.track.TurboTracker$DefaultLogger { *; }
-keep interface io.core.common.helper.track.TurboTracker$Logger { *; }
-keep interface io.core.common.helper.track.TurboTracker$ExtraInfoProvider { *; }

# ============================================
# MultiState
# ============================================
-keep public class * extends io.core.engine.multi_state.MultiState { *; }

# ============================================
# 枚举
# ============================================
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ============================================
# Native方法
# ============================================
-keepclasseswithmembernames class * {
    native <methods>;
}

# ============================================
# R资源
# ============================================
-keep class **.R$* {*;}

# ============================================
# Activity onClick
# ============================================
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

# ============================================
# Renderscript
# ============================================
-keep class android.support.v8.renderscript.** { *; }
-keep class androidx.renderscript.** { *; }

# ============================================
# ExoPlayer反射
# ============================================
-keepclassmembers class androidx.media3.datasource.cache.CacheDataSource$Factory {
    *** upstreamDataSourceFactory;
}

# ============================================
# 避免警告
# ============================================
-dontwarn kotlin.**
-dontwarn kotlinx.**
-dontwarn org.jetbrains.**
