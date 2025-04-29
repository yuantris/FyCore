package com.core.fy.android.function.media

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.core.fy.android.databinding.ActivityMediaPlayerBinding
import com.hjq.permissions.Permission
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.helper.media.MediaHelper
import io.core.common.helper.coroutine.Coroutine
import io.core.common.helper.media.FlowMediaPlayer
import io.core.common.helper.media.PlayerState
import io.core.common.util.MediaScanner
import io.core.common.util.extensions.cool.GSON
import io.core.common.util.extensions.cool.requestPermission
import io.core.common.util.extensions.cool.runMain
import io.core.common.util.extensions.cool.toastOnUI
import io.core.common.util.extensions.logE
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.extensions.ui.onTrackingTouch
import io.core.common.util.log.LogCat
import io.core.common.util.tools.UriTools
import io.core.constant.FileSize
import io.core.constant.FileSize.toFormattedPattern
import io.core.other.TimeMeasurer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/18 15:36
 * @description
 * @author Yuan
 */
class MediaPlayerActivity : ReflectBindingActivity<ActivityMediaPlayerBinding>() {

    private val player by lazy {
        FlowMediaPlayer(
            context = this@MediaPlayerActivity,
            scope = lifecycleScope
        )
    }

    private var list: List<MediaScanner.FileInfo>? = null

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)


    }

    override fun setListener() {
        with(binding) {
            audioList.onClick {
                requestPermission(Permission.MANAGE_EXTERNAL_STORAGE) {
                    Coroutine.async {
                        val files = MediaScanner.queryFiles(
                            types = setOf(
                                MediaScanner.MediaFileType.MP4,
                            ),
                            addFilter = {
                                it.size > 1024 * 1024
                            }
                        )
                        files
                    }.onSuccess { result ->
                        list = result
                        val map = mutableMapOf<String, Boolean>()
                        lifecycleScope.launch(Dispatchers.IO) {
                            val timeSilent = TimeMeasurer.measureTimeSilent {
                                val paths = result.map { it.path }
                                val validateMediaFiles = MediaHelper.areVideosValid(paths)
                                if (validateMediaFiles.isNotEmpty()) {
                                    val errorFiles = validateMediaFiles.filter { !it.value }
                                    if (errorFiles.isNotEmpty()) {
                                        errorFiles.forEach {
                                            LogCat.e("文件：${it.key}，校验失败")
                                        }
                                    }
                                }
                                LogCat.v("文件校验：${GSON.toJson(validateMediaFiles)}")
                            }
                            "validateMediaFiles_校验耗时：${timeSilent}ms".logE()
                        }

                        lifecycleScope.launch(Dispatchers.IO) {
                            val timeSilent = TimeMeasurer.measureTimeSilent {
                                result.forEach {
                                    val valid = MediaHelper.isMediaFileValid(it.path)
                                    map[it.path] = valid
                                }
                                LogCat.v("文件校验：${GSON.toJson(map)}")
                            }
                            "isMediaFileValid_校验耗时：${timeSilent}ms".logE()
                        }

                        val file = File(result[0].path)
                        val uri = UriTools.file2Uri(File(file.path))

                        FileSize.format(result[0].size).logE()

                        tip.text = "文件路径：${file.path}"
                        player.prepare(uri)
                    }
                }
            }

            play.onClick {
                player.playPause()
            }

            pause.onClick {
                player.playPause()
            }

            keep.onClick {
                player.playPause()
            }

            stop.onClick {
                player.stop()
            }

            playerProgressbar.onTrackingTouch(start = {
                isTouch.set(true)
            }) {
                isTouch.set(false)
                player.seekTo(it.progress)
            }
        }
    }


    /*触摸Seekbar标记*/
    private var isTouch = AtomicBoolean(false)

    override fun observers() {
        lifecycleScope.launch {
            player.playerState
                .collect { state ->
                    when (state) {
                        is PlayerState.Ready -> {
                            binding.playerProgressbar.max = state.duration
                            val duration = FileSize.formatDuration(state.duration.toLong(), "mm:ss")
                            runMain {
                                binding.playerProgressMax.text = duration
                                binding.playerProgressCurrent.text = "00:00"
                            }
                        }

                        is PlayerState.Playing -> {
                            val progress = state.progress
                            runMain {
                                if (!isTouch.get()) {
                                    binding.playerProgressbar.progress = progress
                                }

                                binding.playerProgressCurrent.text =
                                    progress.toLong().toFormattedPattern()
                            }
                        }

                        is PlayerState.Paused -> {
                            toastOnUI("暂停")
                        }

                        is PlayerState.Error -> {
                            toastOnUI("错误")
                            LogCat.e(state.error)
                        }

                        is PlayerState.Completed -> {
                            toastOnUI("完成")
                        }

                        else -> {
                            LogCat.v("state:${state::class.simpleName}")
                        }
                    }
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}