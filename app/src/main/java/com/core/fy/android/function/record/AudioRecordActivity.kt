package com.core.fy.android.function.record

import android.os.Bundle
import android.os.Environment
import androidx.lifecycle.lifecycleScope
import com.core.fy.android.databinding.ActivityAudioRecordBinding
import com.core.fy.android.util.AudioFormatType
import com.core.fy.android.util.AudioRecorder
import com.core.fy.android.util.RecorderConfig
import com.core.fy.android.util.RecorderState
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.util.ext.cool.launchAsync
import io.core.common.util.ext.cool.launchSync
import io.core.common.util.ext.ui.onClick
import kotlinx.coroutines.flow.collectLatest
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

    private val config = RecorderConfig()
    private val recorder = AudioRecorder(config, lifecycleScope)

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)

        binding.start.onClick {
            XXPermissions.with(this@AudioRecordActivity)
                .permission(Permission.RECORD_AUDIO)
                .request { _, _ ->
                    val outputFile = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                        "recording.wav"
                    )
                    // recorder.start(outputFile, AudioFormatType.WAV)
                }

        }

        binding.stop.onClick {
            // recorder.stop()
        }
    }

    override fun observers() {
        launchSync {
            recorder.state.collectLatest { state ->
                when (state) {
                    is RecorderState.Idle -> {
                        binding.tip.text = "开始"
                    }

                    is RecorderState.Preparing -> {
                        binding.tip.text = "准备中"
                    }

                    is RecorderState.Recording -> {
                        binding.tip.text = "正在录制"
                    }

                    is RecorderState.Paused -> {
                        binding.tip.text = "暂停"
                    }

                    is RecorderState.Stopped -> {
                        binding.tip.text = "停止"
                    }

                    is RecorderState.Error -> {
                        binding.tip.text = "出错 ${state.exception}"
                    }
                }
            }
        }
    }
}