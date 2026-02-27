package io.core.utils.tools.file

import androidx.annotation.Keep

/**
 * 重命名操作结果数据类
 * @property success 操作是否成功
 * @property newPath 新的文件路径，如果操作失败则为null
 */
@Keep
data class RenameResult(
    val success: Boolean,
    val newPath: String?
)