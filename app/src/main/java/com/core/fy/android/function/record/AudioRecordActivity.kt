package com.core.fy.android.function.record

import android.os.Bundle
import com.core.fy.android.databinding.ActivityAudioRecordBinding
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import io.core.ui.base.component.activity.ReflectBindingActivity
import io.core.utils.extensions.cool.PathType
import io.core.utils.extensions.cool.getBasePath
import io.core.utils.extensions.cool.refreshMediaLibrary
import io.core.utils.extensions.cool.runMain
import io.core.utils.extensions.cool.timeFormat
import io.core.utils.extensions.currentTimeMillis
import io.core.utils.extensions.ui.onClick
import io.core.utils.log.LogPure
import io.core.base.constant.FileSize
import io.core.base.constant.FileSize.toFormattedPattern
import io.core.base.constant.TimePatterns
import java.io.File

/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/2/17 16:32
 * @description
 * @author Yuan
 */
class AudioRecordActivity : ReflectBindingActivity<ActivityAudioRecordBinding>() {

    private lateinit var recorder: AudioRecorder

    private fun updateUI(text: String) = runMain {
        binding.tip.text = text
    }

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)

        // 初始化录音器
        val strategy = MediaRecorderStrategy(Format.M4A)
        recorder = AudioRecorder(
            strategy = strategy,
            callback = object : RecorderCallback {
                override fun onStateChanged(state: RecordingState) {
                    when (state) {
                        is RecordingState.Recording -> updateUI(
                            "Recording: ${
                                FileSize.formatDuration(
                                    state.duration,
                                    TimePatterns.TIME_MM_SS
                                )
                            }"
                        )

                        is RecordingState.Paused -> updateUI(
                            "Paused: ${
                                FileSize.formatDuration(state.duration, TimePatterns.TIME_MM_SS)
                            }"
                        )

                        RecordingState.Idle -> updateUI("Ready")
                    }
                }

                override fun onTimeUpdate(durationMillis: Long) {
                    binding.duration.text =
                        "onTimeUpdate: ${durationMillis.toFormattedPattern()}"
                }

                override fun onError(message: String) {
                    updateUI("Error: $message")
                }

                override fun onRecordDone(file: File) {
                    file.refreshMediaLibrary()

                    LogPure.v {
                        "保存路径�?{file.path}"
                    }
                }
            })

        binding.start.onClick {
            XXPermissions.with(this)
                .permission(Permission.RECORD_AUDIO)
                .request { permissions, allGranted ->
                    // 开始录�?
                    recorder.start(
                        outputDir = getBasePath(PathType.MUSIC),
                        fileName = "recording_${currentTimeMillis.timeFormat(TimePatterns.FILE_SAFE_TIMESTAMP)}"
                    )
                }
        }

        // 暂停/恢复
        binding.pause.setOnClickListener {
            recorder.pause()
        }

        binding.resume.onClick {
            recorder.resume()
        }

        binding.stop.onClick {
            recorder.stop()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recorder.release()
    }
}