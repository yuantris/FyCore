package com.core.fy.android.util

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import io.core.common.util.log.LogPure

object FFmpegTool {
    private const val TAG = "FFmpegHelper"

    sealed class FFmpegResult {
        class Success(val id: Long, val task: String) : FFmpegResult()
        class Cancel(val id: Long, val task: String) : FFmpegResult()
        class Failure(val id: Long, val task: String, val error: String) : FFmpegResult()
        class Progress(
            val id: Long,
            val task: String,
            val stats: com.arthenica.ffmpegkit.Statistics?
        ) : FFmpegResult()
    }

    // 基础执行方法
    fun executeCommand(
        command: Array<String>,
        taskId: String = "",
        onResult: (FFmpegResult) -> Unit = {}
    ): FFmpegSession {
        return FFmpegKit.executeWithArgumentsAsync(command, { session ->
            if (ReturnCode.isSuccess(session.returnCode)) {
                onResult(FFmpegResult.Success(session.sessionId, taskId))
            } else if (ReturnCode.isCancel(session.returnCode)) {
                onResult(FFmpegResult.Cancel(session.sessionId, taskId))
            } else {
                onResult(
                    FFmpegResult.Failure(
                        session.sessionId,
                        taskId,
                        "FFmpeg Error: Command failed with state ${session.state} and rc ${session.returnCode}.${session.failStackTrace}"
                    )
                )
            }
        }, { log ->
            LogPure.d(TAG) { "log [${log.sessionId}_$taskId]: ${log.message}" }
        }, { stats -> onResult(FFmpegResult.Progress(stats.sessionId, taskId, stats)) })
    }


    // 取消正在执行的任务
    fun cancel(sessionId: Long) {
        FFmpegKit.cancel(sessionId)
    }

    // 改进后的参数转换方法
    fun String.toFFmpegArgs() = split("\\s+(?=([^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex())
        .map { it.replace("\"", "") }
        .toTypedArray()

    fun Array<String>.toFFmpegCommand() = joinToString(" ") {
        if (it.contains(" ")) "\"$it\"" else it
    }

}