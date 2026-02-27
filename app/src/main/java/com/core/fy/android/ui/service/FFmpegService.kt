package com.core.fy.android.ui.service

import androidx.lifecycle.lifecycleScope
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.core.fy.android.constants.EventKey
import com.core.fy.android.util.FFmpegTool
import io.core.ui.base.component.service.BaseService
import io.core.utils.MediaScanner
import io.core.utils.extensions.cool.PathType
import io.core.utils.extensions.cool.formatToFixedDecimal
import io.core.utils.extensions.cool.getFileName
import io.core.utils.extensions.cool.getSettingsPathV2
import io.core.utils.extensions.cool.isFilePath
import io.core.utils.extensions.cool.observeEvent
import io.core.utils.extensions.logE
import io.core.utils.extensions.logI
import io.core.utils.log.LogPure
import io.core.utils.tools.FileTools
import io.core.utils.media.MediaHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FFmpegService : BaseService() {
    override fun isForegroundService() = false

    override fun onCreate() {
        super.onCreate()

        observeEvent<List<MediaScanner.FileInfo>?>(EventKey.FFmpeg) {

            val testPath = "/storage/emulated/0/Airdrop/Video/很可�?mp4"
            testPath.isFilePath().logI()
            if (!FileTools.exist(testPath)) return@observeEvent
            val settingsPathV2 = getSettingsPathV2(
                PathType.DOCUMENTS,
                subDir = "video",
                fileName = testPath.getFileName()
            )
            val command = arrayOf(
                "-y",
                "-i", testPath,
                "-vf", "scale=720:-1",
                "-crf", "28",
                "-preset", "fast",
                settingsPathV2
            )

            execute {
                val duration = MediaHelper.getDurationSuspend(testPath, null)
                if (duration.isNullOrEmpty()) return@execute
                FFmpegTool.executeCommand(
                    command,
                    taskId = testPath,
                    onResult = { result ->
                        when (result) {
                            is FFmpegTool.FFmpegResult.Success -> {
                                lifecycleScope.launch(Dispatchers.Main) {
                                    LogPure.d { "${result.id}_${result.task}压缩成功" }

                                    val session = FFmpegKitConfig.getSession(result.id)
                                    session.command.logE()
                                }
                            }

                            is FFmpegTool.FFmpegResult.Cancel -> {
                                LogPure.w { "${result.id}_${result.task}压缩取消" }
                            }

                            is FFmpegTool.FFmpegResult.Failure -> {
                                LogPure.e { "${result.id}_${result.task}压缩失败: ${result.error}" }
                            }

                            is FFmpegTool.FFmpegResult.Progress -> {
                                val time = result.stats?.time ?: 0.0
                                val totalTime = duration.toDouble()
                                val progress = time / totalTime * 100

                                LogPure.d { "${result.id}_${result.task}压缩�?..${progress.formatToFixedDecimal()}" }
                            }
                        }
                    })
            }


        }
    }
}