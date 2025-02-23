@file:Suppress("DEPRECATION")

package com.core.fy.android.function.record

import android.media.MediaRecorder
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.io.File
import java.io.IOException
import java.nio.file.Files

// 增强录音状态密封类，提取公共文件属性
sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(val file: File, val duration: Long) : RecordingState()
    data class Paused(val file: File, val duration: Long) : RecordingState()
}

// 增强音频格式枚举，添加采样率配置
enum class Format(
    val fileExtension: String,
    val mediaRecorderFormat: Int,
    val mediaRecorderEncoder: Int,
    val sampleRate: Int = 44100 // 默认采样率
) {
    M4A("m4a", MediaRecorder.OutputFormat.MPEG_4, MediaRecorder.AudioEncoder.AAC),
    OGG("ogg", MediaRecorder.OutputFormat.OGG, MediaRecorder.AudioEncoder.OPUS),
    AAC("aac", MediaRecorder.OutputFormat.AAC_ADTS, MediaRecorder.AudioEncoder.AAC);
}

// 增强录音策略接口
interface RecorderStrategy {
    val audioFormat: Format

    // 新增文件名配置属性
    var customFileName: String?

    @Throws(IOException::class, SecurityException::class)
    fun start(outputFile: File)

    fun pause()
    fun resume()

    @Throws(IllegalStateException::class)
    fun stop(): File

    fun getCurrentDuration(): Long
    var onError: ((String) -> Unit)?

    fun generateOutputFile(outputDir: File, fileName: String? = null): File {
        // 1. 兼容的目录创建方式（替换 NIO API）
        if (!outputDir.mkdirs() && !outputDir.isDirectory) {
            throw IOException("Cannot create directory: ${outputDir.absolutePath}")
        }

        // 2. 增强目录验证（包含符号链接检查）
        require(outputDir.isDirectory && !isSymbolicLink(outputDir)) {
            "Invalid directory: ${outputDir.absolutePath}"
        }
        require(outputDir.canWrite()) {
            "Directory not writable: ${outputDir.absolutePath}"
        }

        // 3. 文件名安全处理（过滤非法字符并限制长度）
        val baseName = (fileName ?: "recording_${System.currentTimeMillis()}")
            .replace(Regex("[\\\\/:*?\"<>|]"), "_") // 过滤非法字符
            .take(100) // 防止过长文件名
            .ifEmpty { "recording_${System.currentTimeMillis()}" }

        val extension = audioFormat.fileExtension

        // 4. 使用序列生成避免竞态条件
        return generateSequence(0) { it + 1 }
            .map { if (it == 0) baseName else "${baseName}_$it" }
            .map { name -> File(outputDir, "$name.$extension") }
            .first { !it.exists() }
            .also { file ->
                // 5. 使用原子性文件创建
                if (!file.createNewFile()) {
                    throw IOException("File already exists: ${file.absolutePath}")
                }
            }
    }

    private fun isSymbolicLink(file: File): Boolean {
        return try {
            val canonicalPath = file.canonicalPath
            val absolutePath = file.absolutePath
            canonicalPath != absolutePath
        } catch (e: IOException) {
            false
        }
    }

}

// 增强回调接口
interface RecorderCallback {
    fun onStateChanged(state: RecordingState)
    fun onTimeUpdate(durationMillis: Long)
    fun onError(message: String)
    fun onRecordDone(file: File) {} // 新增保存成功回调
}

// 增强的MediaRecorder策略实现
class MediaRecorderStrategy(
    override val audioFormat: Format
) : RecorderStrategy {
    private var mediaRecorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var paused = false
    private var startTime = 0L
    private var totalDuration = 0L

    override var onError: ((String) -> Unit)? = null
    override var customFileName: String? = null

    override fun start(outputFile: File) {
        if (mediaRecorder != null) return

        this.outputFile = outputFile
        totalDuration = 0L
        startTime = SystemClock.elapsedRealtime()

        try {
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(audioFormat.mediaRecorderFormat)
                setAudioEncoder(audioFormat.mediaRecorderEncoder)
                setAudioSamplingRate(audioFormat.sampleRate) // 设置采样率
                setOutputFile(outputFile.absolutePath)
                prepare()
                start()
            }
            paused = false
        } catch (e: Exception) {
            onError?.invoke("Recording initialization failed: ${e.javaClass.simpleName} - ${e.message}")
            resetResources()
        }
    }

    override fun pause() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            onError?.invoke("Pause/resume requires API 24+")
            return
        }

        mediaRecorder?.let {
            if (!paused) {
                try {
                    it.pause()
                    totalDuration += SystemClock.elapsedRealtime() - startTime
                    paused = true
                } catch (e: IllegalStateException) {
                    onError?.invoke("Pause failed: ${e.message}")
                }
            }
        }
    }

    override fun resume() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return

        mediaRecorder?.let {
            if (paused) {
                try {
                    it.resume()
                    startTime = SystemClock.elapsedRealtime()
                    paused = false
                } catch (e: IllegalStateException) {
                    onError?.invoke("Resume failed: ${e.message}")
                }
            }
        }
    }

    override fun stop(): File {
        val outputFile = outputFile ?: throw IllegalStateException("No active recording")

        mediaRecorder?.let {
            try {
                if (!paused) it.stop() else {
                } // 仅在非暂停状态调用stop
            } catch (e: Exception) {
                onError?.invoke("Stop failed: ${e.javaClass.simpleName} - ${e.message}")
            } finally {
                resetResources()
            }
        }
        return outputFile
    }

    override fun getCurrentDuration(): Long {
        return if (paused) {
            totalDuration
        } else {
            totalDuration + (SystemClock.elapsedRealtime() - startTime)
        }
    }

    private fun resetResources() {
        mediaRecorder?.apply {
            try {
                reset()
            } catch (e: Exception) {
                // 忽略reset异常
            }
            release()
        }
        mediaRecorder = null
        outputFile = null
        paused = false
    }
}

// 增强的录音控制器
class AudioRecorder(
    private val strategy: RecorderStrategy,
    private val callback: RecorderCallback
) {
    private val handler = Handler(Looper.getMainLooper())
    private var timeUpdateRunnable: Runnable? = null
    private var currentState: RecordingState = RecordingState.Idle

    init {
        strategy.onError = { message ->
            handler.post {
                callback.onError(message)
                transitionToIdle()
            }
        }
    }

    fun start(outputDir: File, fileName: String? = null) {
        if (currentState !is RecordingState.Idle) return

        try {
            val outputFile = strategy.generateOutputFile(outputDir, fileName)
            strategy.start(outputFile)
            currentState = RecordingState.Recording(outputFile, 0)
            notifyStateChange()
            startTimeUpdates()
        } catch (e: Exception) {
            callback.onError("Recording start failed: ${e.javaClass.simpleName} - ${e.message}")
            transitionToIdle()
        }
    }

    fun pause() {
        if (currentState !is RecordingState.Recording) return

        strategy.pause()
        currentState = RecordingState.Paused(
            (currentState as RecordingState.Recording).file,
            strategy.getCurrentDuration()
        )
        notifyStateChange()
        stopTimeUpdates()
    }

    fun resume() {
        if (currentState !is RecordingState.Paused) return

        strategy.resume()
        currentState = RecordingState.Recording(
            (currentState as RecordingState.Paused).file,
            strategy.getCurrentDuration()
        )
        notifyStateChange()
        startTimeUpdates()
    }

    fun stop() {
        if (currentState is RecordingState.Idle) return

        try {
            val savedFile = strategy.stop()
            callback.onRecordDone(savedFile) // 新增保存成功回调
        } catch (e: Exception) {
            callback.onError("File save failed: ${e.message}")
        } finally {
            transitionToIdle()
        }
    }

    fun release() {
        stop()
        handler.removeCallbacksAndMessages(null)
    }

    private fun startTimeUpdates() {
        stopTimeUpdates()
        timeUpdateRunnable = object : Runnable {
            override fun run() {
                val duration = strategy.getCurrentDuration()
                handler.post { callback.onTimeUpdate(duration) }

                currentState = when (currentState) {
                    is RecordingState.Recording -> (currentState as RecordingState.Recording).copy(
                        duration = duration
                    )

                    is RecordingState.Paused -> (currentState as RecordingState.Paused).copy(
                        duration = duration
                    )

                    else -> currentState
                }

                handler.postDelayed(this, 1000)
            }
        }
        timeUpdateRunnable?.let { handler.post(it) }
    }

    private fun stopTimeUpdates() {
        timeUpdateRunnable?.let { handler.removeCallbacks(it) }
        timeUpdateRunnable = null
    }

    private fun notifyStateChange() {
        handler.post { callback.onStateChanged(currentState) }
    }

    private fun transitionToIdle() {
        currentState = RecordingState.Idle
        notifyStateChange()
        stopTimeUpdates()
    }
}