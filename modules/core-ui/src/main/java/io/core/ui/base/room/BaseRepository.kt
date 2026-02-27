package io.core.ui.base.room

import io.core.utils.log.LogPure
import io.core.utils.tools.TimeTools

/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/1/3 15:38
 * @description
 * @author Yuan
 */
open class BaseRepository {
    init {
        LogPure.v { "Repository init: ${TimeTools.getNowString()}" }
    }
}