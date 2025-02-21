package io.core.common.util

import android.provider.MediaStore


object SQL {

    // region 排序扩展
    // 按添加时间排序
    @JvmStatic
    val timeAddedDESC: String
        get() = "${MediaStore.MediaColumns.DATE_ADDED} DESC"
    @JvmStatic
    val timeAddedASC: String
        get() = "${MediaStore.MediaColumns.DATE_ADDED} ASC"

    // 按修改时间排序
    @JvmStatic
    val dateModifiedDESC: String
        get() = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
    @JvmStatic
    val dateModifiedASC: String
        get() = "${MediaStore.MediaColumns.DATE_MODIFIED} ASC"

    // 按文件名排序
    @JvmStatic
    val displayNameASC: String
        get() = "${MediaStore.MediaColumns.DISPLAY_NAME} ASC"
    @JvmStatic
    val displayNameDESC: String
        get() = "${MediaStore.MediaColumns.DISPLAY_NAME} DESC"

    // 按文件大小排序
    @JvmStatic
    val sizeDESC: String
        get() = "${MediaStore.MediaColumns.SIZE} DESC"
    @JvmStatic
    val sizeASC: String
        get() = "${MediaStore.MediaColumns.SIZE} ASC"

    // 按 MIME 类型排序
    @JvmStatic
    val mimeTypeASC: String
        get() = "${MediaStore.MediaColumns.MIME_TYPE} ASC"
    @JvmStatic
    val mimeTypeDESC: String
        get() = "${MediaStore.MediaColumns.MIME_TYPE} DESC"

    // 图片拍摄时间排序
    @JvmStatic
    val dateTakenDESC: String
        get() = "${MediaStore.Images.ImageColumns.DATE_TAKEN} DESC"
    @JvmStatic
    val dateTakenASC: String
        get() = "${MediaStore.Images.ImageColumns.DATE_TAKEN} ASC"

    // 视频时长排序
    @JvmStatic
    val durationDESC: String
        get() = "${MediaStore.Video.VideoColumns.DURATION} DESC"
    @JvmStatic
    val durationASC: String
        get() = "${MediaStore.Video.VideoColumns.DURATION} ASC"

    // 文件路径排序
    @JvmStatic
    val dataASC: String
        get() = "${MediaStore.MediaColumns.DATA} ASC"
    @JvmStatic
    val dataDESC: String
        get() = "${MediaStore.MediaColumns.DATA} DESC"
    // endregion

    // region 常用查询条件
    // 媒体类型筛选（适用于 MediaStore.Files 查询）
    @JvmStatic
    val isImage: String
        get() = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE}"
    @JvmStatic
    val isVideo: String
        get() = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO}"
    @JvmStatic
    val isAudio: String
        get() = "${MediaStore.Files.FileColumns.MEDIA_TYPE} = ${MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO}"

    // 非空/有效文件条件
    @JvmStatic
    val sizeGreaterThanZero: String
        get() = "${MediaStore.MediaColumns.SIZE} > 0"
    @JvmStatic
    val mimeTypeNotEmpty: String
        get() = "${MediaStore.MediaColumns.MIME_TYPE} != ''"

    // 常用 MIME 类型过滤（示例）
    @JvmStatic
    val isJPEGImage: String
        get() = "${MediaStore.MediaColumns.MIME_TYPE} = 'image/jpeg'"
    @JvmStatic
    val isMP4Video: String
        get() = "${MediaStore.MediaColumns.MIME_TYPE} = 'video/mp4'"
    // endregion
}