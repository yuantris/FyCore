package com.core.fy.android.constants

import android.annotation.SuppressLint
import java.text.SimpleDateFormat

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/14 8:49
 * @description
 * @author Yuan
 */
@SuppressLint("SimpleDateFormat")
object AppConst {
    val timeFormat: SimpleDateFormat by lazy {
        SimpleDateFormat("HH:mm")
    }
    const val channelIdReadAloud = "channel_read_aloud_fy"

}