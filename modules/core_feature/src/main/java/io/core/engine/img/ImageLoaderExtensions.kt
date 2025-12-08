package io.core.engine.img

import android.content.Context
import android.widget.ImageView
import androidx.annotation.DrawableRes

/**
 * 通用图片加载函数，支持 URL 和资源 ID
 * usage: imageView.load("http://...")
 * usage: imageView.load(R.drawable.xxx)
 */
fun ImageView.load(source: Any?, block: (ImageOptions.Builder.() -> Unit)? = null) {
    val builder = ImageOptions.Builder().url(source).into(this)
    block?.let { builder.apply(it) }
    ImageLoader.load(builder.build())
}

/**
 * Kotlin 极其简洁的 DSL 写法
 * usage: imageView.loadUrl("http://...") {
 * placeholder(R.drawable.xxx)
 * circle()
 * }
 */
fun ImageView.loadUrl(url: Any?, block: (ImageOptions.Builder.() -> Unit)? = null) {
    val builder = ImageOptions.Builder().url(url).into(this)
    // 如果有自定义配置，应用它
    block?.let { builder.apply(it) }

    ImageLoader.load(builder.build())
}

/**
 * 加载资源图片
 * usage: imageView.loadResource(R.drawable.xxx)
 */
fun ImageView.loadResource(@DrawableRes resId: Int, block: (ImageOptions.Builder.() -> Unit)? = null) {
    val builder = ImageOptions.Builder().url(resId).into(this)
    block?.let { builder.apply(it) }
    ImageLoader.load(builder.build())
}

/**
 * 加载圆形资源图片
 * usage: imageView.loadCircleResource(R.drawable.xxx)
 */
fun ImageView.loadCircleResource(@DrawableRes resId: Int, block: (ImageOptions.Builder.() -> Unit)? = null) {
    val builder = ImageOptions.Builder().url(resId).into(this).asCircle()
    block?.let { builder.apply(it) }
    ImageLoader.load(builder.build())
}

/**
 * 加载圆角资源图片
 * usage: imageView.loadRoundResource(R.drawable.xxx, 10)
 */
fun ImageView.loadRoundResource(@DrawableRes resId: Int, radiusDp: Int, block: (ImageOptions.Builder.() -> Unit)? = null) {
    val builder = ImageOptions.Builder().url(resId).into(this).corner(radiusDp)
    block?.let { builder.apply(it) }
    ImageLoader.load(builder.build())
}

/**
 * 加载模糊资源图片
 * usage: imageView.loadBlurResource(R.drawable.xxx, 20)
 */
fun ImageView.loadBlurResource(@DrawableRes resId: Int, blurRadius: Int, block: (ImageOptions.Builder.() -> Unit)? = null) {
    val builder = ImageOptions.Builder().url(resId).into(this).blur(blurRadius)
    block?.let { builder.apply(it) }
    ImageLoader.load(builder.build())
}

/**
 * 加载灰度资源图片
 * usage: imageView.loadGrayscaleResource(R.drawable.xxx)
 */
fun ImageView.loadGrayscaleResource(@DrawableRes resId: Int, block: (ImageOptions.Builder.() -> Unit)? = null) {
    val builder = ImageOptions.Builder().url(resId).into(this).grayscale()
    block?.let { builder.apply(it) }
    ImageLoader.load(builder.build())
}

/**
 * 加载圆形图片
 * usage: imageView.loadCircle("http://...")
 */
fun ImageView.loadCircle(url: Any?, block: (ImageOptions.Builder.() -> Unit)? = null) {
    val builder = ImageOptions.Builder().url(url).into(this).asCircle()
    block?.let { builder.apply(it) }
    ImageLoader.load(builder.build())
}

/**
 * 加载圆角图片
 * usage: imageView.loadRound("http://...", 10)
 */
fun ImageView.loadRound(url: Any?, radiusDp: Int, block: (ImageOptions.Builder.() -> Unit)? = null) {
    val builder = ImageOptions.Builder().url(url).into(this).corner(radiusDp)
    block?.let { builder.apply(it) }
    ImageLoader.load(builder.build())
}

/**
 * 加载模糊图片
 * usage: imageView.loadBlur("http://...", 20)
 */
fun ImageView.loadBlur(url: Any?, blurRadius: Int, block: (ImageOptions.Builder.() -> Unit)? = null) {
    val builder = ImageOptions.Builder().url(url).into(this).blur(blurRadius)
    block?.let { builder.apply(it) }
    ImageLoader.load(builder.build())
}

/**
 * 加载灰度图片
 * usage: imageView.loadGrayscale("http://...")
 */
fun ImageView.loadGrayscale(url: Any?, block: (ImageOptions.Builder.() -> Unit)? = null) {
    val builder = ImageOptions.Builder().url(url).into(this).grayscale()
    block?.let { builder.apply(it) }
    ImageLoader.load(builder.build())
}

/**
 * 预加载图片
 * usage: context.preloadImage("http://...")
 */
fun Context.preloadImage(url: Any?, block: (ImageOptions.Builder.() -> Unit)? = null) {
    val builder = ImageOptions.Builder().url(url)
    block?.let { builder.apply(it) }
    ImageLoader.preload(this, builder.build())
}

/**
 * 清除图片加载请求
 * usage: imageView.clearImage()
 */
fun ImageView.clearImage() {
    ImageLoader.clear(this)
}

/**
 * 支持 DSL 风格配置的图片加载扩展函数
 * usage: imageView.loadWithConfig("http://...") {
 *     placeholderResId = R.drawable.placeholder
 *     isCircle = true
 *     cornerRadius = 10
 * }
 */
fun ImageView.loadWithConfig(source: Any?, config: ImageOptions.() -> Unit = {}) {
    ImageLoader.load(this, source, config)
}