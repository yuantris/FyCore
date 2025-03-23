package com.core.fy.android.ui.service

import com.core.fy.android.constants.EventKey
import com.core.fy.android.util.FFmpegHelper
import com.core.fy.android.util.showDxNotification
import io.core.common.base.component.service.BaseService
import io.core.common.util.MediaScanner
import io.core.common.util.extensions.cool.PathType
import io.core.common.util.extensions.cool.getSettingsPathV2
import io.core.common.util.extensions.cool.observeEvent
import io.core.common.util.log.LogPure
import io.core.common.util.tools.FileTools

class FFmpegService : BaseService() {
    override fun isForegroundService() = false

    private var count = 0

    override fun onCreate() {
        super.onCreate()

        observeEvent<List<MediaScanner.FileInfo>?>(EventKey.FFmpeg) {

            it?.forEach { file ->
                val settingsPathV2 = getSettingsPathV2(
                    PathType.DOCUMENTS,
                    subDir = "video",
                    fileName = FileTools.getName(file.path)
                )
                val command = arrayOf(
                    "-y",
                    "-i", file.path,
                    "-vf", "scale=720:-1",
                    "-crf", "28",
                    "-preset", "fast",
                    settingsPathV2
                )
                FFmpegHelper.executeCommand(
                    command,
                    onSuccess = {
                        count++
                        LogPure.d { "压缩: $count" }
                        if (count == it.size) {
                            showDxNotification {
                                content = "压缩完成"
                            }
                        }
                    },
                    onFailure = {

                    })

            }
        }
    }
}