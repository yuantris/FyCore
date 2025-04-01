package com.core.fy.android.function.record

import android.annotation.SuppressLint
import android.os.Bundle
import com.core.fy.android.databinding.ActivityAudioRecordBinding
import com.hjq.permissions.Permission
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.util.extensions.cool.PathType
import io.core.common.util.extensions.cool.getBasePath
import io.core.common.util.extensions.cool.refreshMediaLibrary
import io.core.common.util.extensions.cool.requestPermission
import io.core.common.util.extensions.cool.runMain
import io.core.common.util.extensions.cool.timeFormat
import io.core.common.util.extensions.currentTimeMillis
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.log.LogPure
import io.core.constant.FileSize
import io.core.constant.FileSize.toFormattedPattern
import io.core.constant.TimeFormat
import java.io.File

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
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
                                    TimeFormat.TIME_MM_SS
                                )
                            }"
                        )

                        is RecordingState.Paused -> updateUI(
                            "Paused: ${
                                FileSize.formatDuration(state.duration, TimeFormat.TIME_MM_SS)
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
                        "保存路径：${file.path}"
                    }
                }
            })

        binding.start.onClick {
            requestPermission(Permission.RECORD_AUDIO) {
                // 开始录音
                recorder.start(
                    outputDir = getBasePath(PathType.MUSIC),
                    fileName = "recording_${currentTimeMillis.timeFormat(TimeFormat.FILE_SAFE_TIMESTAMP)}"
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