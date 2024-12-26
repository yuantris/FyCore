package com.core.libraries.util

import com.blankj.utilcode.util.TimeUtils

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2024/12/26 14:48
 * @description
 * @author Yuan
 */
object CoreUtil {

    class File {
        companion object {
            /**
             * 根据指定格式生成文件名
             * 文件名的格式由用户指定的格式字符串决定，时间戳部分使用"yyyyMMdd_HHmmss"格式
             *
             * @param format 指定的文件名格式字符串，也是文件的扩展名
             * @return 返回生成的文件名
             */
            fun generateName(format: String): String {
                val dateFormat = TimeUtils.getSafeDateFormat("yyyyMMdd_HHmmss")
                val name = "${format.uppercase()}_${TimeUtils.getNowString(dateFormat)}.$format"
                return name
            }
        }
    }
}