package io.core.common.util.tools

import android.media.MediaMetadataRetriever
import androidx.annotation.WorkerThread
import io.core.common.util.log.LogCat
import io.core.constant.FileSize.formatDuration
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object MultimediaUtil {

    /**
     * 获取音频/视频文件的时长，并按指定格式化输出
     * 使用阻塞方式运行挂起函数
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
    @WorkerThread
    fun getDuration(filePath: String, formatStr: String? = "mm:ss"): String? = runBlocking {
        obtainDuration(filePath, formatStr)
    }

    suspend fun obtainDuration(filePath: String, formatStr: String? = "mm:ss"): String? {
        return suspendCoroutine { continuation ->
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(filePath)
                val durationMs =
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                        ?.toLongOrNull()
                val formattedDuration = formatStr?.let {
                    durationMs?.let { ms -> formatDuration(ms, it) }
                } ?: kotlin.run { durationMs?.toString() ?: "" }
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
