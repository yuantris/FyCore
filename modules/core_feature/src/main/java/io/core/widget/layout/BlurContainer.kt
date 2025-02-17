package io.core.widget.layout// BlurContainer.kt
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class BlurContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    // 配置参数
    private var blurRadius: Float = 90f
    private var downSampleFactor: Int = 8
    private var isBlurEnabled: Boolean = true

    // 图形资源
    private var overlayBitmap: Bitmap? = null
    private var blurBitmap: Bitmap? = null
    private var blurCanvas: Canvas? = null

    // 线程控制
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val isDirty = AtomicBoolean(true)

    // RenderScript 资源 (低版本备用)
    private val rs: RenderScript by lazy { RenderScript.create(context) }
    private val blurScript: ScriptIntrinsicBlur by lazy {
        ScriptIntrinsicBlur.create(rs, Element.U8_4(rs)).apply {
            setRadius(blurRadius)
        }
    }

    init {
        setWillNotDraw(false)
        setupHardwareLayer()
    }

    private fun setupHardwareLayer() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                // Android 12+ 使用原生硬件加速模糊
                setRenderEffect(RenderEffect.createBlurEffect(
                    blurRadius,
                    blurRadius,
                    Shader.TileMode.MIRROR
                ))
                setLayerType(LAYER_TYPE_HARDWARE, null)
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN -> {
                // 低版本使用软件层+RenderScript优化
                setLayerType(LAYER_TYPE_SOFTWARE, null)
            }
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w == oldw && h == oldh) return
        releaseBitmaps()
        val scaledW = (w / downSampleFactor).coerceAtLeast(1)
        val scaledH = (h / downSampleFactor).coerceAtLeast(1)
        overlayBitmap = Bitmap.createBitmap(scaledW, scaledH, Bitmap.Config.ARGB_8888)
        blurCanvas = overlayBitmap?.let { Canvas(it) }
    }

    override fun invalidate() {
        super.invalidate()
        isDirty.set(true)
    }

    override fun dispatchDraw(canvas: Canvas) {
        if (shouldSkipBlur()) {
            super.dispatchDraw(canvas)
            return
        }

        drawChildrenToCache()
        dispatchCachedBlur(canvas)
        triggerAsyncBlurProcessing()
    }

    private fun shouldSkipBlur(): Boolean {
        return !isBlurEnabled || Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    }

    private fun drawChildrenToCache() {
        overlayBitmap?.let { bitmap ->
            blurCanvas?.apply {
                save()
                scale(1f / downSampleFactor, 1f / downSampleFactor)
                super.dispatchDraw(this)
                restore()
                // 添加脏区域检测的扩展点
                isDirty.compareAndSet(false, true)
            }
        }
    }

    private fun dispatchCachedBlur(canvas: Canvas) {
        blurBitmap?.let {
            canvas.save()
            canvas.scale(downSampleFactor.toFloat(), downSampleFactor.toFloat())
            canvas.drawBitmap(it, 0f, 0f, null)
            canvas.restore()
        }
    }

    private fun triggerAsyncBlurProcessing() {
        if (isDirty.compareAndSet(true, false)) {
            executor.execute(::processMultiStageBlur)
        }
    }

    private fun processMultiStageBlur() {
        val srcBitmap = overlayBitmap ?: return
        val blurred = performMultiStageBlur(srcBitmap)
        mainHandler.post { updateBlurResult(blurred) }
    }

    private fun performMultiStageBlur(src: Bitmap): Bitmap {
        // 多级降采样模糊（L1->L2->L1）
        val l1 = downscaleBitmap(src, 4)
        var temp = applyBlur(l1)
        val l2 = downscaleBitmap(temp, 2)
        temp = applyBlur(l2)
        return upscaleToOriginal(temp, src.width, src.height)
    }

    private fun downscaleBitmap(src: Bitmap, factor: Int): Bitmap {
        return Bitmap.createScaledBitmap(
            src,
            src.width / factor,
            src.height / factor,
            true
        )
    }

    private fun applyBlur(src: Bitmap): Bitmap {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            renderScriptBlur(src)
        } else {
            stackBlur(src) // 自定义模糊算法备用
        }
    }

    private fun renderScriptBlur(src: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val input = Allocation.createFromBitmap(rs, src)
        val outputAlloc = Allocation.createTyped(rs, input.type)
        blurScript.apply {
            setInput(input)
            forEach(outputAlloc)
        }
        outputAlloc.copyTo(output)
        input.destroy()
        outputAlloc.destroy()
        return output
    }

    private fun stackBlur(bitmap: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)


        val w = bitmap.width
        val h = bitmap.height
        val r = 10


        // 应用水平模糊
        val horizontal = IntArray(pixels.size)
        for (y in 0 until h) {
            for (x in 0 until w) {
                var sumRed = 0
                var sumGreen = 0
                var sumBlue = 0
                var sumAlpha = 0
                for (i in -r..r) {
                    val x2 = minOf(maxOf(x + i, 0), w - 1)
                    val pixel = pixels[y * w + x2]
                    sumAlpha += (pixel shr 24) and 0xFF
                    sumRed += (pixel shr 16) and 0xFF
                    sumGreen += (pixel shr 8) and 0xFF
                    sumBlue += pixel and 0xFF
                }
                val count = 2 * r + 1
                horizontal[y * w + x] = ((sumAlpha / count) shl 24) or
                        ((sumRed / count) shl 16) or
                        ((sumGreen / count) shl 8) or
                        (sumBlue / count)
            }
        }


        // 应用垂直模糊
        for (x in 0 until w) {
            for (y in 0 until h) {
                var sumRed = 0
                var sumGreen = 0
                var sumBlue = 0
                var sumAlpha = 0
                for (i in -r..r) {
                    val y2 = minOf(maxOf(y + i, 0), h - 1)
                    val pixel = horizontal[y2 * w + x]
                    sumAlpha += (pixel shr 24) and 0xFF
                    sumRed += (pixel shr 16) and 0xFF
                    sumGreen += (pixel shr 8) and 0xFF
                    sumBlue += pixel and 0xFF
                }
                val count = 2 * r + 1
                output.setPixel(x, y, ((sumAlpha / count) shl 24) or
                        ((sumRed / count) shl 16) or
                        ((sumGreen / count) shl 8) or
                        (sumBlue / count))
            }
        }
        return output

    }

    private fun upscaleToOriginal(src: Bitmap, targetW: Int, targetH: Int): Bitmap {
        return Bitmap.createScaledBitmap(src, targetW, targetH, true)
    }

    private fun updateBlurResult(result: Bitmap) {
        blurBitmap?.recycle()
        blurBitmap = result
        invalidate()
    }

    private fun releaseBitmaps() {
        overlayBitmap?.recycle()
        blurBitmap?.recycle()
        overlayBitmap = null
        blurBitmap = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        executor.shutdownNow()
        releaseBitmaps()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) rs.destroy()
    }

    // 公开方法
    fun setBlurRadius(radius: Float) {
        blurRadius = radius.coerceIn(0f, 25f)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            blurScript.setRadius(blurRadius)
        }
        invalidate()
    }

    fun configure(downSample: Int = 8, enable: Boolean = true) {
        downSampleFactor = downSample
        isBlurEnabled = enable
        releaseBitmaps()
        requestLayout()
    }
}
