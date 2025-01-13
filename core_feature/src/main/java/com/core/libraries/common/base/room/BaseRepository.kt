package com.core.libraries.common.base.room

import com.core.libraries.common.util.ext.tool.TAG
import com.core.libraries.common.util.CoreUtil
import com.core.libraries.common.util.log.LogUtils

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/3 15:38
 * @description
 * @author Yuan
 */
open class BaseRepository {
    init {
        LogUtils.logV(TAG, "Repository init: ${CoreUtil.Time.getNowTime()}")
    }
}