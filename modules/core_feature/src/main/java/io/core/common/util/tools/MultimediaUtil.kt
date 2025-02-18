package io.core.common.util.tools

import android.media.MediaMetadataRetriever
import io.core.common.helper.coroutine.launchSuspend
import io.core.common.helper.coroutine.runSuspend
import io.core.common.helper.tryCatchWithDefault
import io.core.common.util.log.LogCat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

fun Long.formatDuration(formatStr: String): String {
    return MultimediaUtil.formatDuration(this, formatStr)
}

object MultimediaUtil {

    /**
     * 获取音频/视频文件的时长，并按指定格式化输出
     *
     * @param filePath  媒体文件路径
     * @param formatStr 格式化字符串（例如 "HH:mm:ss", "mm:ss", "m:ss.SS"）
     * @return 格式化后的时长（获取失败返回 null）
     *
     * val duration1 = MultimediaUtil.getDuration(filePath, "HH:mm:ss") // 00:03:45
     * val duration2 = MultimediaUtil.getDuration(filePath, "mm:ss")     // 03:45
     * val duration3 = MultimediaUtil.getDuration(filePath, "m:ss.SSS")  // 3:45.230
     *
     */
    @JvmStatic
    fun getDuration(filePath: String, formatStr: String = "mm:ss"): String? = runSuspend {
        obtainDuration(filePath, formatStr)
    }

    suspend fun obtainDuration(filePath: String, formatStr: String = "mm:ss"): String? {
        return suspendCoroutine { continuation ->
            launchSuspend(dispatcher = Dispatchers.IO) {
                // 在IO线程中执行操作
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(filePath)
                    val durationMs =
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                            ?.toLongOrNull()
                    val formattedDuration = durationMs?.let { formatDuration(it, formatStr) }
                    continuation.resume(formattedDuration) // 返回格式化后的时长
                } catch (e: Exception) {
                    LogCat.e(e)
                    continuation.resume("") // 异常时返回 ""
                } finally {
                    retriever.release()
                }
            }
        }
    }

    /**
     * 格式化时间
     * @param durationMs 时长（毫秒）
     * @param formatStr  格式化字符串（支持 HH, mm, ss, SSS）
     * @return 格式化后的时间字符串
     */
    fun formatDuration(durationMs: Long, formatStr: String): String {
        val hours = TimeUnit.MILLISECONDS.toHours(durationMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
        val millis = durationMs % 1000

        return formatStr
            .replace("HH", "%02d".format(hours))
            .replace("mm", "%02d".format(minutes))
            .replace("ss", "%02d".format(seconds))
            .replace("SSS", "%03d".format(millis))
    }
}
