package io.core.common.helper

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.annotation.WorkerThread
import io.core.common.util.log.LogCat
import io.core.constant.FileSize.formatDuration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaHelper {
    private const val TAG = "MediaHelper"

    /**
     * 获取音频/视频文件的时长，并按指定格式化输出
     * 使用阻塞方式运行挂起函数
     * @param filePath  媒体文件路径
     * @param formatStr 格式化字符串（例如 "HH:mm:ss", "mm:ss", "m:ss.SS"）
     * @return 格式化后的时长（获取失败返回 null）
     *
     * val duration1 = MediaHelper.getDuration(filePath, "HH:mm:ss") // 00:03:45
     * val duration2 = MediaHelper.getDuration(filePath, "mm:ss")     // 03:45
     * val duration3 = MediaHelper.getDuration(filePath, "m:ss.SSS")  // 3:45.230
     *
     */
    @WorkerThread
    fun getDuration(filePath: String, formatStr: String? = "mm:ss"): String? {
        return try {
            MediaMetadataRetriever().use {
                it.setDataSource(filePath)
                val durationMs = it.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_DURATION
                )?.toLongOrNull()
                formatStr?.let {
                    durationMs?.let { ms -> formatDuration(ms, it) }
                } ?: durationMs?.toString()
            }
        } catch (e: Exception) {
            LogCat.e("获取时长失败", tag = TAG, e)
            null
        }
    }

    /**
     * 获取视频分辨率（同步版本）
     */
    @WorkerThread
    fun getVideoResolution(filePath: String): Pair<Int, Int>? {
        return try {
            MediaMetadataRetriever().use {
                it.setDataSource(filePath)
                val width = it.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
                )?.toIntOrNull()
                val height = it.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
                )?.toIntOrNull()
                if (width != null && height != null) width to height else null
            }
        } catch (e: Exception) {
            LogCat.e("获取分辨率失败", tag = TAG, e)
            null
        }
    }

    /**
     * 获取视频旋转角度（同步版本）
     */
    @WorkerThread
    fun getVideoRotation(filePath: String): Int {
        return try {
            MediaMetadataRetriever().use {
                it.setDataSource(filePath)
                it.extractMetadata(
                    MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION
                )?.toIntOrNull() ?: 0
            }

        } catch (e: Exception) {
            LogCat.e("获取旋转角度失败", tag = TAG, e)
            0
        }
    }

    /**
     * 获取视频缩略图（第一帧）
     * @return Bitmap 失败返回null
     */
    @WorkerThread
    fun getVideoThumbnail(filePath: String): Bitmap? {
        return try {
            MediaMetadataRetriever().use {
                it.setDataSource(filePath)
                it.frameAtTime
            }
        } catch (e: Exception) {
            LogCat.e("获取视频缩略图失败", tag = TAG, e)
            null
        }
    }

    /**
     * 获取音频专辑封面
     * @return Bitmap 失败返回null
     */
    @WorkerThread
    fun getAudioAlbumArt(filePath: String): Bitmap? {
        return try {
            MediaMetadataRetriever().use {
                it.setDataSource(filePath)
                it.embeddedPicture?.let { bytes ->
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }
        } catch (e: Exception) {
            LogCat.e("获取专辑封面失败", tag = TAG, e)
            null
        }
    }

    /**
     * 获取视频帧率
     * @return 帧率(单位：fps)，获取失败返回null
     */
    @WorkerThread
    fun getVideoFrameRate(filePath: String): Int? {
        return try {
            MediaMetadataRetriever().use {
                it.setDataSource(filePath)
                it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CAPTURE_FRAMERATE)?.toIntOrNull()
            }
        } catch (e: Exception) {
            LogCat.e("获取视频帧率失败", tag = TAG, e)
            null
        }
    }

    /**
     * 获取音频比特率
     * @return 比特率(单位：kbps)，获取失败返回null
     */
    @WorkerThread
    fun getAudioBitrate(filePath: String): Int? {
        return try {
            MediaMetadataRetriever().use {
                it.setDataSource(filePath)
                it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull()?.div(1000)
            }
        } catch (e: Exception) {
            LogCat.e("获取音频比特率失败", tag = TAG, e)
            null
        }
    }

    /**
     * 获取视频关键帧缩略图
     * @param timeUs 指定时间(微秒)，默认获取第一个关键帧
     * @return Bitmap 失败返回null
     */
    @WorkerThread
    fun getVideoKeyFrame(filePath: String, timeUs: Long = 0): Bitmap? {
        return try {
            MediaMetadataRetriever().use {
                it.setDataSource(filePath)
                it.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            }
        } catch (e: Exception) {
            LogCat.e("获取关键帧失败", tag = TAG, e)
            null
        }
    }

    /**
     * 检查文件是否为有效媒体文件(注意性能问题)
     */
    @WorkerThread
    fun isMediaFileValid(filePath: String): Boolean {
        return try {
            MediaMetadataRetriever().use {
                it.setDataSource(filePath)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取媒体文件类型
     * @return 媒体类型字符串 (如 "video/mp4", "audio/mpeg")
     */
    @WorkerThread
    fun getMimeType(filePath: String): String? {
        return try {
            MediaMetadataRetriever().use {
                it.setDataSource(filePath)
                it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
            }
        } catch (e: Exception) {
            LogCat.e("获取MIME类型失败", tag = TAG, e)
            null
        }
    }

    /**
     * 批量验证媒体文件是否有效
     * @param filePaths 媒体文件路径列表
     * @return Map<String, Boolean> 文件路径与验证结果映射
     */
    fun validateMediaFiles(filePaths: List<String>): Map<String, Boolean> {
        return MediaMetadataRetriever().use { retriever ->
            filePaths.associateWith { path ->
                try {
                    retriever.setDataSource(path)
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }
    }


    // 协程版本扩展
    suspend fun getDurationSuspend(
        filePath: String,
        formatStr: String? = "mm:ss"
    ) = withContext(Dispatchers.IO) {
        getDuration(filePath, formatStr)
    }

    suspend fun getVideoResolutionSuspend(
        filePath: String
    ) = withContext(Dispatchers.IO) {
        getVideoResolution(filePath)
    }

    suspend fun getVideoRotationSuspend(
        filePath: String
    ) = withContext(Dispatchers.IO) {
        getVideoRotation(filePath)
    }

    suspend fun getVideoThumbnailSuspend(filePath: String) = withContext(Dispatchers.IO) {
        getVideoThumbnail(filePath)
    }

    suspend fun getAudioAlbumArtSuspend(filePath: String) = withContext(Dispatchers.IO) {
        getAudioAlbumArt(filePath)
    }

    suspend fun getMimeTypeSuspend(filePath: String) = withContext(Dispatchers.IO) {
        getMimeType(filePath)
    }

    suspend fun getVideoFrameRateSuspend(filePath: String) = withContext(Dispatchers.IO) {
        getVideoFrameRate(filePath)
    }

    suspend fun getAudioBitrateSuspend(filePath: String) = withContext(Dispatchers.IO) {
        getAudioBitrate(filePath)
    }

    suspend fun getVideoKeyFrameSuspend(filePath: String, timeUs: Long = 0) = withContext(Dispatchers.IO) {
        getVideoKeyFrame(filePath, timeUs)
    }

    suspend fun isMediaFileValidSuspend(filePath: String) = withContext(Dispatchers.IO) {
        isMediaFileValid(filePath)
    }

}
