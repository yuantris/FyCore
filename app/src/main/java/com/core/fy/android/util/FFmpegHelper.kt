package com.core.fy.android.util

import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import io.core.common.util.extensions.cool.GSON
import io.core.common.util.log.LogPure
import kotlin.coroutines.suspendCoroutine

object FFmpegHelper {
    const val TAG = "FFmpegHelper"

    // 基础执行方法
    fun executeCommand(
        command: Array<String>,
        onSuccess: () -> Unit = {},
        onFailure: (error: String) -> Unit = {}
    ): FFmpegSession {
        return FFmpegKit.executeAsync(command.toFFmpegCommand(), { session ->
            if (ReturnCode.isSuccess(session.returnCode)) {
                onSuccess()
            } else {
                onFailure("FFmpeg Error: ${session.returnCode}")
            }
        }, { log ->
            LogPure.d(TAG) {
                "log: ${GSON.toJson(log)}"
            }
        }, { stats -> /* 处理统计信息 */ })
    }

    // 使用协程的异步封装
    suspend fun executeCommandAsync(command: Array<String>) =
        suspendCoroutine<Boolean> { continuation ->
            FFmpegKit.executeAsync(command.toFFmpegCommand(), { session ->
                continuation.resumeWith(
                    Result.success(ReturnCode.isSuccess(session.returnCode))
                )
            })
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