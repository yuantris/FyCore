package com.core.fy.android.function.camerax

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import io.core.common.util.tools.runOnUI
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraHelper private constructor(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val builder: Builder
) {
    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    // Use cases
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null

    // 初始化相机
    fun initializeCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(context))
    }

    @SuppressLint("RestrictedApi")
    private fun bindCameraUseCases() {
        val cameraProvider = cameraProvider ?: return

        // 创建用例构建器
        val useCaseGroupBuilder = UseCaseGroup.Builder().apply {
            // 预览用例
            if (builder.enablePreview) {
                preview = Preview.Builder()
                    .setTargetAspectRatio(builder.previewAspectRatio)
                    .setTargetRotation(previewView.display.rotation)
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                        addUseCase(it)
                    }
            }

            // 拍照用例
            if (builder.enableImageCapture) {
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(builder.captureMode)
                    .setFlashMode(builder.flashMode)
                    .setTargetAspectRatio(builder.imageCaptureAspectRatio)
                    //.setTargetResolution(builder.imageResolution!!)
                    .build()
                    .also {
                        addUseCase(it)
                    }
            }

            // 图像分析用例
            if (builder.enableImageAnalysis) {
                imageAnalyzer = ImageAnalysis.Builder()
                    .setTargetResolution(builder.analysisResolution)
                    .setBackpressureStrategy(builder.backpressureStrategy)
                    .setOutputImageFormat(builder.outputImageFormat)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, builder.analyzer)
                        addUseCase(it)
                    }

            }

            // 视频录制用例
            if (builder.enableVideoCapture) {
                val recorder = Recorder.Builder()
                    .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                    .build()
                videoCapture = VideoCapture.withOutput(recorder)
                    .also {
                        addUseCase(it)
                    }
            }
        }

        // 相机选择
        val cameraSelector = CameraSelector.Builder()
            .requireLensFacing(builder.lensFacing)
            .build()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                useCaseGroupBuilder.build()
            )

            // 设置相机控制参数
            camera?.cameraControl?.enableTorch(builder.enableTorch)

        } catch (exc: Exception) {
            builder.errorCallback?.invoke("Camera initialization failed: ${exc.message}")
        }
    }

    // 拍照
    fun takePicture(callback: (Bitmap?, Exception?) -> Unit) {
        val imageCapture = imageCapture ?: run {
            callback(null, IllegalStateException("ImageCapture not enabled"))
            return
        }

        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            File.createTempFile(
                "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}",
                ".jpg",
                context.externalCacheDir
            )
        ).build()

        imageCapture.takePicture(
            outputFileOptions,
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val bitmap = BitmapFactory.decodeFile(output.savedUri?.path)
                    runOnUI {
                        callback(bitmap, null)
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    runOnUI {
                        callback(null, exc)
                    }
                }
            })
    }

    // 开始录像
    @SuppressLint("MissingPermission")
    fun startRecording(callback: (File?, Exception?) -> Unit) {
        val videoCapture = videoCapture ?: run {
            callback(null, IllegalStateException("VideoCapture not enabled"))
            return
        }

        val outputFile = File.createTempFile(
            "VID_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}",
            ".mp4",
            context.externalCacheDir
        )

        recording = videoCapture.output
            .prepareRecording(context, FileOutputOptions.Builder(outputFile).build())
            .withAudioEnabled()
            .start(cameraExecutor) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        // 录制开始
                    }

                    is VideoRecordEvent.Finalize -> {
                        if (event.hasError()) {
                            callback(null, event.cause as Exception?)
                        } else {
                            callback(outputFile, null)
                        }
                    }
                }
            }
    }

    // 停止录像
    fun stopRecording() {
        recording?.stop()
        recording = null
    }

    // 切换摄像头
    fun switchCamera() {
        builder.lensFacing = when (builder.lensFacing) {
            CameraSelector.LENS_FACING_BACK -> CameraSelector.LENS_FACING_FRONT
            else -> CameraSelector.LENS_FACING_BACK
        }
        bindCameraUseCases()
    }

    // 调整缩放比例
    fun setZoomRatio(ratio: Float) {
        camera?.cameraControl?.setZoomRatio(
            ratio.coerceIn(
                camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f,
                camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f
            )
        )
    }

    // 清理资源
    @SuppressLint("RestrictedApi")
    fun shutdown() {
        cameraExecutor.shutdown()
        cameraProvider?.unbindAll()
        cameraProvider?.shutdown()
    }

    class Builder(
        val context: Context,
        val lifecycleOwner: LifecycleOwner,
        val previewView: PreviewView
    ) {
        // 功能开关
        var enablePreview = true
        var enableImageCapture = false
        var enableImageAnalysis = false
        var enableVideoCapture = false

        // 相机配置
        var lensFacing: Int = CameraSelector.LENS_FACING_BACK
        var enableTorch: Boolean = false

        // 预览配置
        var previewAspectRatio: Int = AspectRatio.RATIO_16_9

        // 拍照配置
        var flashMode: Int = ImageCapture.FLASH_MODE_OFF
        var captureMode: Int = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        var imageCaptureAspectRatio: Int = AspectRatio.RATIO_16_9
        var imageResolution: Size? = null

        // 分析配置
        var analysisResolution: Size = Size(1280, 720)
        var backpressureStrategy: Int = ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
        var outputImageFormat: Int = ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888
        var analyzer: ImageAnalysis.Analyzer = ImageAnalysis.Analyzer { image ->
            image.close()
        }

        // 错误回调
        var errorCallback: ((String) -> Unit)? = null

        fun build(): CameraHelper {
            return CameraHelper(context, lifecycleOwner, previewView, this)
        }

        // 链式配置方法
        fun enablePreview(enable: Boolean) = apply { this.enablePreview = enable }
        fun enableImageCapture(enable: Boolean) = apply { this.enableImageCapture = enable }
        fun enableImageAnalysis(enable: Boolean) = apply { this.enableImageAnalysis = enable }
        fun enableVideoCapture(enable: Boolean) = apply { this.enableVideoCapture = enable }
        fun setLensFacing(facing: Int) = apply { this.lensFacing = facing }
        fun setTorch(enabled: Boolean) = apply { this.enableTorch = enabled }
        fun setPreviewAspectRatio(ratio: Int) = apply { this.previewAspectRatio = ratio }
        fun setFlashMode(mode: Int) = apply { this.flashMode = mode }
        fun setCaptureMode(mode: Int) = apply { this.captureMode = mode }
        fun setImageResolution(size: Size) = apply { this.imageResolution = size }
        fun setAnalysisResolution(size: Size) = apply { this.analysisResolution = size }
        fun setBackpressureStrategy(strategy: Int) = apply { this.backpressureStrategy = strategy }
        fun setOutputImageFormat(format: Int) = apply { this.outputImageFormat = format }
        fun setAnalyzer(analyzer: ImageAnalysis.Analyzer) = apply { this.analyzer = analyzer }
        fun setErrorCallback(callback: (String) -> Unit) = apply { this.errorCallback = callback }
    }
}

// 扩展函数：将ImageProxy转换为Bitmap
fun ImageProxy.toBitmap(): Bitmap? {
    val buffer = planes[0].buffer
    buffer.rewind()
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
