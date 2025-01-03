package com.core.libraries.util

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import com.blankj.utilcode.util.TimeUtils
import com.core.libraries.Android
import com.core.libraries.base.ext.logD
import com.core.libraries.base.ext.logI
import com.core.libraries.constant.DateFormatPatterns
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

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
class CoreUtil {

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
                return "${format.uppercase()}_${TimeUtils.getNowString(dateFormat)}.$format"
            }

            /**
             * 根据指定格式生成无后缀文件名
             * 文件名的格式由用户指定的格式字符串决定，时间戳部分使用"yyyyMMdd_HHmmss"格式
             *
             * @param prefix 指定的文件名前缀字符串
             * @return 返回生成的无后缀文件名
             */
            fun generateNameNoExtension(prefix: String): String {
                val dateFormat = TimeUtils.getSafeDateFormat("yyyyMMdd_HHmmss")
                return "${prefix.uppercase()}_${TimeUtils.getNowString(dateFormat)}"
            }

            /**
             * 刷新整个媒体库
             * 该函数通过扫描全部路径来更新系统媒体库，以便媒体文件能够被系统识别和索引
             */
            fun refreshMediaLibrary() {
                MediaScannerConnection.scanFile(
                    Android.context,
                    arrayOf(Environment.getExternalStorageDirectory().absolutePath),
                    null
                ) { path, uri ->
                    // 扫描完成后的回调
                    "Scanned $path:\nuri=$uri".logD()
                    "Scanned succeed.".logI()
                }

            }

            /**
             * 计算文件的哈希值
             *
             * @param file 要计算哈希值的文件对象
             * @param algorithm 哈希算法名称，默认为 "SHA-256"
             * @return 文件的哈希值字符串
             *
             * 此函数读取指定文件的内容，并使用给定的算法计算文件的哈希值
             * 如果文件不存在或不是文件类型，将抛出 IllegalArgumentException 异常
             */
            fun getFileHash(file: java.io.File, algorithm: String = "SHA-256"): String {
                if (!file.exists() || !file.isFile) {
                    throw IllegalArgumentException("Invalid file path")
                }
                val buffer = ByteArray(1024 * 4) // 4KB 缓冲区
                val digest = MessageDigest.getInstance(algorithm)

                FileInputStream(file).use { inputStream ->
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        digest.update(buffer, 0, bytesRead)
                    }
                }

                return digest.digest().joinToString("") { "%02x".format(it) } // 转换为十六进制字符串
            }

        }
    }

    class Time {
        companion object {
            fun getNowTime(pattern: String = DateFormatPatterns.yyyyMMddHHmmss): String {
                return TimeUtils.getNowString(TimeUtils.getSafeDateFormat(pattern))
            }

            /**
             * 获取当前时间戳
             *
             * @return 返回当前时间戳，单位为毫秒
             */
            fun getCurrentTimestamp(): Long {
                return System.currentTimeMillis()
            }

            /**
             * 获取当前时间戳（以字符串形式返回）
             *
             * @return 返回当前时间戳的字符串形式，单位为毫秒
             */
            fun getCurrentTimestampString(): String {
                return getCurrentTimestamp().toString()
            }
        }
    }

    class Activity {
        companion object {
            /**
             * 重启指定的 Activity
             * 此函数通过结束当前 Activity 并使用相同的 Intent 重新启动它，从而实现重启 Activity 的效果
             * @param activity 要重启的 Activity 实例
             */
            fun restartActivity(activity: android.app.Activity) {
                val intent = activity.intent
                activity.finish() // 结束当前 Activity
                activity.overridePendingTransition(0, 0) // 去除过渡动画
                activity.startActivity(intent) // 重新启动当前 Activity
                activity.overridePendingTransition(0, 0) // 去除过渡动画
            }
        }
    }
}