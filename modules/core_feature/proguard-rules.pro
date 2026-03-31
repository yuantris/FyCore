# 库模块混淆配置
# 此文件仅用于库模块本身的混淆，不用于consumer

# 基本优化配置
-optimizationpasses 5
-allowaccessmodification
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-verbose
-dontpreverify

# 保留属性
-keepattributes *Annotation*,InnerClasses,Signature,SourceFile,LineNumberTable

# 优化算法
-optimizations !code/simplification/cast,!field/*,!class/merging/*

# 渲染脚本
-keep class android.support.v8.renderscript.** { *; }
-keep class androidx.renderscript.** { *; }

# ExoPlayer
-keepclassmembers class androidx.media3.datasource.cache.CacheDataSource$Factory {
    *** upstreamDataSourceFactory;
}

# Activity onClick
-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}
