package com.core.fy.android.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.AbsoluteSizeSpan
import android.text.style.BackgroundColorSpan
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.LineBackgroundSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.URLSpan
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.TextView
import androidx.annotation.ColorInt

// ======================== 核心 DSL 构建�?========================
class SpannableBuilder {
    private val spans = mutableListOf<SpanHolder>()
    private var currentPosition = 0

    fun text(content: String, block: SpanConfig.() -> Unit = {}) {
        val start = currentPosition
        val end = start + content.length
        spans.add(SpanHolder(content, block))
        currentPosition = end
    }

    fun build(): SpannableString {
        val fullText = spans.joinToString("") { it.content }
        val spannable = SpannableString(fullText)
        var cursor = 0

        spans.forEach { holder ->
            val start = cursor
            val end = start + holder.content.length
            val config = SpanConfig().apply(holder.block)

            config.spans.forEach { span ->
                spannable.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            cursor = end
        }

        return spannable
    }

    private data class SpanHolder(val content: String, val block: SpanConfig.() -> Unit)
}

// ======================== 类型安全配置�?========================
open class SpanConfig {
    internal val spans = mutableListOf<Any>()

    // ---------- 基础样式 ----------
    fun bold() = addSpan(StyleSpan(Typeface.BOLD))
    fun italic() = addSpan(StyleSpan(Typeface.ITALIC))
    fun underline() = addSpan(UnderlineSpan())
    fun strikeThrough() = addSpan(StrikethroughSpan())

    fun color(@ColorInt color: Int) = addSpan(ForegroundColorSpan(color))
    fun bgColor(@ColorInt color: Int) = addSpan(BackgroundColorSpan(color))

    fun size(pixels: Int) = addSpan(AbsoluteSizeSpan(pixels))
    fun relativeSize(scale: Float) = addSpan(RelativeSizeSpan(scale))

    fun clickable(
        isUnderline: Boolean = true,
        onClick: (View) -> Unit
    ) = addSpan(object : ClickableSpan() {
        override fun onClick(widget: View) = onClick(widget)
        override fun updateDrawState(ds: TextPaint) {
            ds.isUnderlineText = isUnderline
        }
    })

    fun url(url: String) = addSpan(URLSpan(url))

    // =============== 高级用法扩展�?===============
    // 自定义圆角背景色
    fun roundedBg(
        @ColorInt color: Int,
        radius: Float = 8f
    ) = addSpan(object : LineBackgroundSpan {
        override fun drawBackground(
            canvas: Canvas,
            paint: Paint,
            left: Int,
            right: Int,
            top: Int,
            baseline: Int,
            bottom: Int,
            text: CharSequence,
            start: Int,
            end: Int,
            lineNumber: Int
        ) {
            val originalColor = paint.color
            paint.color = color
            canvas.drawRoundRect(
                left.toFloat(),
                top.toFloat(),
                right.toFloat(),
                bottom.toFloat(),
                radius,
                radius,
                paint
            )
            paint.color = originalColor
        }
    })

    // 组合样式复用
    fun highlight() {
        color(Color.YELLOW)
        bgColor(Color.DKGRAY)
        relativeSize(1.2f)
    }

    protected fun addSpan(span: Any) {
        spans.add(span)
    }
}

// ======================== TextView 扩展函数 ========================
fun TextView.buildSpannable(block: SpannableBuilder.() -> Unit) {
    val builder = SpannableBuilder().apply(block)
    text = builder.build()
}

fun TextView.enableLinkMovementMethod() {
    movementMethod = LinkMovementMethod.getInstance()
    highlightColor = Color.TRANSPARENT
}

// ======================== 完整使用示例 ========================
/*
// �?Activity 中使用：
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val textView = findViewById<TextView>(R.id.textView)

        textView.buildSpannable {
            text("Hello") {
                bold()
                color(Color.RED)
                roundedBg(Color.LTGRAY, 12f)
            }

            text(" Kotlin") {
                italic()
                highlight() // 使用组合样式
            }

            text("\nClick Me") {
                clickable {
                    Toast.makeText(this@MainActivity, "Clicked!", Toast.LENGTH_SHORT).show()
                }
                underline()
            }

            text("\nVisit Google") {
                url("https://www.google.com")
                color(Color.BLUE)
            }
        }
    }
}
*/