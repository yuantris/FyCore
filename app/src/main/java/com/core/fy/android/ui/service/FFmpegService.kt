package com.core.fy.android.ui.service

import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.core.fy.android.constants.EventKey
import com.core.fy.android.util.FFmpegTool
import io.core.common.base.component.service.BaseService
import io.core.common.helper.coroutine.info.CoroutineLauncher
import io.core.common.helper.coroutine.info.GlobalScopeManager
import io.core.common.util.MediaScanner
import io.core.common.util.extensions.cool.PathType
import io.core.common.util.extensions.cool.formatToFixedDecimal
import io.core.common.util.extensions.cool.getFileName
import io.core.common.util.extensions.cool.getSettingsPathV2
import io.core.common.util.extensions.cool.isFilePath
import io.core.common.util.extensions.cool.observeEvent
import io.core.common.util.extensions.logE
import io.core.common.util.extensions.logI
import io.core.common.util.log.LogPure
import io.core.common.util.tools.FileTools
import io.core.common.util.tools.MultimediaUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FFmpegService : BaseService() {
    override fun isForegroundService() = false

    override fun onCreate() {
        super.onCreate()

        observeEvent<List<MediaScanner.FileInfo>?>(EventKey.FFmpeg) {

            val testPath = "/storage/emulated/0/Airdrop/Video/很可能.mp4"
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
                val duration = MultimediaUtil.obtainDuration(testPath, null)
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

                                LogPure.d { "${result.id}_${result.task}压缩中...${progress.formatToFixedDecimal()}" }
                            }
                        }
                    })
            }


        }
    }
}