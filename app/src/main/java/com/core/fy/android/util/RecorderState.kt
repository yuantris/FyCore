package com.core.fy.android.util

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.app.ActivityCompat
import io.core.appCtx
import io.core.common.helper.tryCatch
import io.core.common.util.log.AppLog
import io.core.common.util.log.LogCat
import io.core.common.util.tools.toastOnUi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile

// 状态密封类
sealed class RecorderState {
    object Idle : RecorderState()
    object Preparing : RecorderState()
    object Recording : RecorderState()
    object Paused : RecorderState()
    data class Error(val exception: Throwable) : RecorderState()
    object Stopped : RecorderState()
}

// 音频格式类型
enum class AudioFormatType {
    WAV, AAC, MP3
}

// 录音配置
data class RecorderConfig(
    val sampleRate: Int = 44100,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT
)

// 编码器接口
interface AudioEncoder {
    fun prepare(outputFile: File)
    fun write(data: ByteArray, length: Int)
    fun pause()
    fun resume()
    fun release()
}

// WAV 编码器实现
class WavEncoder(private val config: RecorderConfig) : AudioEncoder {
    private var outputStream: RandomAccessFile? = null
    private var dataSize = 0L
    private var isHeaderWritten = false

    override fun prepare(outputFile: File) {
        outputStream = RandomAccessFile(outputFile, "rw").apply {
            // 先写入临时头（最后会更新）
            write(WavHeader(config).bytes)
            isHeaderWritten = true
        }
    }

    override fun write(data: ByteArray, length: Int) {
        check(isHeaderWritten) { "Encoder not prepared" }
        outputStream?.write(data, 0, length)
        dataSize += length
    }

    override fun pause() = Unit // WAV 不需要特殊处理
    override fun resume() = Unit

    override fun release() {
        outputStream?.use {
            // 更新头信息
            it.seek(0)
            it.write(WavHeader(config, dataSize).bytes)
        }
    }
}

// 新增完整的 WAV 头处理实现
private class WavHeader(
    private val config: RecorderConfig,
    private val dataSize: Long = 0
) {
    companion object {
        private const val HEADER_SIZE = 44L
        private const val RIFF_CHUNK_ID = 0x52494646
        private const val WAVE_FORMAT = 0x57415645
        private const val FMT_CHUNK_ID = 0x666d7420
        private const val DATA_CHUNK_ID = 0x64617461
        private const val SUBCHUNK1_SIZE = 16
        private const val AUDIO_FORMAT_PCM = 1.toShort()
    }

    val bytes: ByteArray by lazy {
        val byteBuffer = java.nio.ByteBuffer.allocate(HEADER_SIZE.toInt())
            .order(java.nio.ByteOrder.LITTLE_ENDIAN)

        // RIFF chunk
        byteBuffer.putInt(RIFF_CHUNK_ID)
        byteBuffer.putInt((HEADER_SIZE + dataSize - 8).toInt()) // Total file size - 8
        byteBuffer.putInt(WAVE_FORMAT)

        // fmt sub-chunk
        byteBuffer.putInt(FMT_CHUNK_ID)
        byteBuffer.putInt(SUBCHUNK1_SIZE)
        byteBuffer.putShort(AUDIO_FORMAT_PCM)
        byteBuffer.putShort(getChannelCount(config.channelConfig).toShort())
        byteBuffer.putInt(config.sampleRate)
        byteBuffer.putInt(config.sampleRate * getChannelCount(config.channelConfig) * getBitsPerSample(config.audioFormat) / 8)
        byteBuffer.putShort((getChannelCount(config.channelConfig) * getBitsPerSample(config.audioFormat) / 8).toShort())
        byteBuffer.putShort(getBitsPerSample(config.audioFormat).toShort())

        // data sub-chunk
        byteBuffer.putInt(DATA_CHUNK_ID)
        byteBuffer.putInt(dataSize.toInt())

        byteBuffer.array()
    }

    private fun getChannelCount(channelConfig: Int): Int {
        return when (channelConfig) {
            AudioFormat.CHANNEL_IN_MONO -> 1
            AudioFormat.CHANNEL_IN_STEREO -> 2
            else -> 1
        }
    }

    private fun getBitsPerSample(audioFormat: Int): Int {
        return when (audioFormat) {
            AudioFormat.ENCODING_PCM_8BIT -> 8
            AudioFormat.ENCODING_PCM_16BIT -> 16
            AudioFormat.ENCODING_PCM_FLOAT -> 32
            else -> 16
        }
    }
}

// 录音管理器
class AudioRecorder(
    private val config: RecorderConfig,
    private val scope: CoroutineScope
) {
    private val _state = MutableStateFlow<RecorderState>(RecorderState.Idle)
    val state: StateFlow<RecorderState> = _state.asStateFlow()

    private var audioRecord: AudioRecord? = null
    private var encoder: AudioEncoder? = null
    private var job: Job? = null

    fun start(outputFile: File, format: AudioFormatType) {
        config.validate()
        stop()
        
        encoder = when (format) {
            AudioFormatType.WAV -> WavEncoder(config)
            // 添加其他格式支持...
            else -> null
        }

        job = scope.launch(Dispatchers.IO) {
            try {
                _state.value = RecorderState.Preparing
                encoder?.prepare(outputFile)

                val bufferSize = AudioRecord.getMinBufferSize(
                    config.sampleRate,
                    config.channelConfig,
                    config.audioFormat
                )

                if (ActivityCompat.checkSelfPermission(
                        appCtx,
                        Manifest.permission.RECORD_AUDIO
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    appCtx.toastOnUi("请先授予录音权限")
                }
                audioRecord = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.MIC)
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(config.audioFormat)
                        .setSampleRate(config.sampleRate)
                        .setChannelMask(config.channelConfig)
                        .build())
                    .setBufferSizeInBytes(bufferSize)
                    .build()
                    .apply { startRecording() }

                _state.value = RecorderState.Recording
                val buffer = ByteArray(bufferSize)

                while (isActive) {
                    when (_state.value) {
                        is RecorderState.Recording -> {
                            val bytesRead = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                            if (bytesRead > 0) {
                                encoder?.write(buffer, bytesRead)
                            }
                        }
                        is RecorderState.Paused -> delay(50)
                        else -> break
                    }
                }
            } catch (e: Exception) {
                _state.value = RecorderState.Error(e)
            } finally {
                shutdown()
            }
        }
    }

    fun pause() {
        if (_state.value == RecorderState.Recording) {
            _state.value = RecorderState.Paused
            encoder?.pause()
        }
    }

    fun resume() {
        if (_state.value == RecorderState.Paused) {
            _state.value = RecorderState.Recording
            encoder?.resume()
        }
    }

    fun stop() {
        job?.cancel()
        shutdown()
    }

    private fun shutdown() {
        audioRecord?.stop()
        audioRecord?.release()
        encoder?.release()
        _state.value = RecorderState.Stopped
    }

    private fun RecorderConfig.validate() {
        val channelCount = when (channelConfig) {
            AudioFormat.CHANNEL_IN_MONO -> 1
            AudioFormat.CHANNEL_IN_STEREO -> 2
            else -> throw IllegalArgumentException("Unsupported channel config")
        }

        when (audioFormat) {
            AudioFormat.ENCODING_PCM_8BIT,
            AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.ENCODING_PCM_FLOAT -> {}
            else -> throw IllegalArgumentException("Unsupported audio format")
        }

        if (sampleRate !in 8000..48000) {
            throw IllegalArgumentException("Unsupported sample rate")
        }
    }
}