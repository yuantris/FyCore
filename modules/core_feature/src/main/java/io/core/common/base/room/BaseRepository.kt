package io.core.common.base.room

import android.util.Log
import io.core.common.util.log.LogPure
import io.core.common.util.log.TAG
import io.core.common.util.tools.TimeUtils

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
        LogPure.logV("Repository init: ${TimeUtils.getNowString()}", TAG)
    }
}