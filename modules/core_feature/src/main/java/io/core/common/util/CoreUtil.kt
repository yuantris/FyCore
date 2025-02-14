package io.core.common.util

import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.os.Looper
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import io.core.common.helper.tryCatch
import io.core.appCtx
import io.core.common.util.ext.logE
import io.core.common.util.ext.ui.getActivity
import io.core.common.util.ext.verify
import io.core.common.util.log.LogCat
import io.core.common.util.log.logD
import io.core.common.util.log.logE
import io.core.common.util.log.logI
import io.core.common.util.tools.TimeUtils
import io.core.constant.FileType
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

    companion object {
        fun toast(text: String) {
            ToastUtil.show(text)
        }

        fun isMainThread(): Boolean {
            return Looper.myLooper() == Looper.getMainLooper()
        }
    }

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
                return "${format.uppercase()}_${TimeUtils.getNowString("yyyyMMdd_HHmmss")}.$format"
            }

            /**
             * 根据指定格式生成无后缀文件名
             * 文件名的格式由用户指定的格式字符串决定，时间戳部分使用"yyyyMMdd_HHmmss"格式
             *
             * @param prefix 指定的文件名前缀字符串
             * @return 返回生成的无后缀文件名
             */
            fun generateNameNoExtension(prefix: String): String {
                return "${prefix.uppercase()}_${TimeUtils.getNowString("yyyyMMdd_HHmmss")}"
            }

            /**
             * 刷新整个媒体库
             * 该函数通过扫描全部路径来更新系统媒体库，以便媒体文件能够被系统识别和索引
             */
            fun refreshMediaLibrary() {
                MediaScannerConnection.scanFile(
                    appCtx,
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

            /**
             * 打开这个文件
             */
            fun openFile(file: java.io.File) {
                val intent = Intent()

                intent.setAction(Intent.ACTION_VIEW)
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addCategory(Intent.CATEGORY_DEFAULT)

                //文件的类型
                val fileName = file.name
                val type = FileType.MATCH_ARRAY
                    .firstOrNull { fileName.endsWith(it[0], ignoreCase = true) }
                    ?.get(1) ?: ""

                tryCatch(
                    tryBlock = {
                        // 直接跳过权限
                        val builder = VmPolicy.Builder()
                        StrictMode.setVmPolicy(builder.build())
                        val fileURI = Uri.fromFile(file)

                        //设置intent的data和Type属性
                        intent.setDataAndType(fileURI, type)
                        appCtx.packageManager.resolveActivity(
                            intent,
                            PackageManager.MATCH_DEFAULT_ONLY
                        ).verify(
                            ifNull = {
                                toast("无法打开该格式文件")
                            },
                            ifNotNull = {
                                appCtx.startActivity(intent)
                                appCtx.getActivity()?.overridePendingTransition(0, 0)
                            })
                    },
                    catchBlock = {
                        toast("无法打开该格式文件")
                        LogCat.e(it)
                    }
                )
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
            @Suppress("DEPRECATION")
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