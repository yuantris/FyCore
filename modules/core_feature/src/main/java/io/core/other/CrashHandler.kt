package io.core.other

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Debug
import android.os.Process
import android.webkit.WebSettings
import androidx.lifecycle.LifecycleCoroutineScope
import io.core.Android
import io.core.appCtx
import io.core.common.CoreConfig
import io.core.common.base.component.activity.CrashActivity
import io.core.common.base.component.activity.CrashSameProcessActivity
import io.core.common.base.component.activity.RestartActivity
import io.core.common.base.interfaces.OnNextStep
import io.core.common.helper.track.AppLifecycleTracker
import io.core.common.util.extensions.cool.PathType
import io.core.common.util.extensions.cool.createFolderReplace
import io.core.common.util.extensions.cool.documentsDir
import io.core.common.util.extensions.cool.getBasePath
import io.core.common.util.extensions.cool.getFile
import io.core.common.util.extensions.cool.hasWriteStoragePermission
import io.core.common.util.extensions.cool.ifNext
import io.core.common.util.extensions.cool.timeFormat
import io.core.common.util.extensions.currentTimeMillis
import io.core.common.util.tools.FileTools
import io.core.constant.CRASH_FOLDER_NAME
import io.core.constant.TimeFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.system.exitProcess

class CrashHandler private constructor(private val application: Application) :
    Thread.UncaughtExceptionHandler {

    companion object {
        const val DIVIDER: String = "--------------------------"

        /** Crash 文件名 */
        const val CRASH_FILE_NAME: String = "crash_file"

        /** Crash 时间记录 */
        const val KEY_CRASH_TIME: String = "key_crash_time"

        /**
         * 注册 Crash 监听
         */
        fun register(application: Application) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(application))
        }

        @JvmStatic
        @JvmOverloads
        fun checkLatestCrash(scope: LifecycleCoroutineScope, action: OnNextStep? = null) {
            if (!CoreConfig.Crash.allowMultiProcess && Android.debug) {
                val millis = System.currentTimeMillis()
                val preferences = appCtx.getSharedPreferences(CRASH_FILE_NAME, Context.MODE_PRIVATE)
                val lastCrashTimeMillis = preferences.getLong(KEY_CRASH_TIME, millis)

                scope.launch(Dispatchers.IO) {
                    // 增加缓存有效期判断（10秒内）
                    val validTimeWindow = TimeUnit.MILLISECONDS.toMillis(10_000)

                    val file = appCtx.externalCacheDir?.getFile(CRASH_FOLDER_NAME)
                        ?.listFiles()
                        ?.filter {
                            it.isFile &&
                                    it.name.startsWith("crash-") &&
                                    it.length() > 0 // 确保非空文件
                        }
                        ?.maxByOrNull { it.lastModified() }

                    // 新增条件：文件必须存在且符合命名规范
                    if (file == null || !file.exists()) {
                        withContext(Dispatchers.Main) { action?.invoke() }
                        return@launch
                    }

                    if (file.lastModified() < millis - validTimeWindow ||
                        abs(lastCrashTimeMillis - file.lastModified()) > validTimeWindow) {
                        withContext(Dispatchers.Main) { action?.invoke() }
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        val openPageCurrent = preferences.getBoolean(file.name, false)
                        // 如果上次崩溃时间距今不超过10秒且当前崩溃页面未打开过，则启动崩溃报告活动
                        if (lastCrashTimeMillis > millis - validTimeWindow && !openPageCurrent) {
                            CrashSameProcessActivity.start(appCtx)
                        } else {
                            action?.invoke()
                        }
                    }
                }
            } else {
                action?.invoke()
            }
        }

        /**
         * 存储异常和参数信息
         */
        private val paramsMap by lazy {
            val map = HashMap<String, String>()
            kotlin.runCatching {
                //获取系统信息
                map["MANUFACTURER"] = Build.MANUFACTURER
                map["BRAND"] = Build.BRAND
                map["MODEL"] = Build.MODEL
                map["SDK_INT"] = Build.VERSION.SDK_INT.toString()
                map["RELEASE"] = Build.VERSION.RELEASE
                map["WebViewUserAgent"] = try {
                    WebSettings.getDefaultUserAgent(appCtx)
                } catch (e: Throwable) {
                    e.toString()
                }
                map["PACKAGE_NAME"] = appCtx.packageName
                map["CURRENT_ACTIVITY"] =
                    AppLifecycleTracker.getTopActivity()?.javaClass?.name ?: "none"
            }
            map
        }

        /**
         * 保存错误信息到文件中
         */
        fun saveCrashInfo2File(timestamp: Long, ex: Throwable): String {
            val sb = StringBuilder()
            for ((key, value) in paramsMap) {
                sb.append(key).append(" = ").append(value).append("\n")
            }

            val writer = StringWriter()
            val printWriter = PrintWriter(writer)
            ex.printStackTrace(printWriter)
            var cause: Throwable? = ex.cause
            while (cause != null) {
                cause.printStackTrace(printWriter)
                cause = cause.cause
            }
            printWriter.close()
            val result = writer.toString()
            sb.append("\n").append(DIVIDER).append("\n").append(result)
            val crashLog = sb.toString()
            val fileName = "crash-${timestamp.timeFormat(TimeFormat.LOG_TIMESTAMP_LINE)}.log"
            val fileNameExternal =
                "crash-${timestamp.timeFormat(TimeFormat.FILE_SAFE_TIMESTAMP)}.log"
            kotlin.runCatching {
                appCtx.externalCacheDir?.let { rootFile ->
                    val exceedTimeMillis = currentTimeMillis - TimeUnit.DAYS.toMillis(7)
                    rootFile.getFile(CRASH_FOLDER_NAME).listFiles()?.forEach {
                        if (it.lastModified() < exceedTimeMillis) {
                            it.delete()
                        }
                    }

                    FileTools.createFileIfNotExist(rootFile, CRASH_FOLDER_NAME, fileName)
                        .writeText(crashLog)

                    // 写入外置存储
                    if (appCtx.hasWriteStoragePermission()) {
                        val folder = FileTools.createFolderIfNotExist(
                            documentsDir,
                            "Crash"
                        )

                        FileTools.createFileIfNotExist(
                            documentsDir,
                            folder.name,
                            fileNameExternal
                        ).writeText(crashLog)
                    }

                }
            }
            return fileName
        }

        /**
         * 进行堆转储
         */
        fun doHeapDump(manually: Boolean = false) {
            val heapDir = appCtx.getBasePath(PathType.EXTERNAL_CACHE)
                .getFile("heapDump")
            heapDir.createFolderReplace()
            val fileName = if (manually) {
                "heap-dump-manually-${currentTimeMillis}.hprof"
            } else {
                "heap-dump-${currentTimeMillis}.hprof"
            }
            val heapFile = heapDir.getFile(fileName)
            val heapDumpName = heapFile.absolutePath
            Debug.dumpHprofData(heapDumpName)
        }
    }

    private val nextHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    init {
        if ((javaClass.name == nextHandler?.javaClass?.name)) {
            // 请不要重复注册 Crash 监听
            throw IllegalStateException("are you ok?")
        }
    }

    @Suppress("ApplySharedPref")
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val sharedPreferences: SharedPreferences = application.getSharedPreferences(
            CRASH_FILE_NAME, Context.MODE_PRIVATE
        )
        val currentCrashTime: Long = currentTimeMillis
        val lastCrashTime: Long = sharedPreferences.getLong(KEY_CRASH_TIME, 0)
        // 记录当前崩溃的时间，以便下次崩溃时进行比对
        sharedPreferences.edit().putLong(KEY_CRASH_TIME, currentCrashTime).commit()

        // 保存崩溃信息
        val fileName = saveCrashInfo2File(currentTimeMillis, throwable)
        if ((throwable is OutOfMemoryError || throwable.cause is OutOfMemoryError)) {
            doHeapDump()
        }

        // 致命异常标记：如果上次崩溃的时间距离当前崩溃小于 5 分钟，那么判定为致命异常
        val deadlyCrash: Boolean = currentCrashTime - lastCrashTime < 1000 * 60 * 5
        if (Android.debug) {
            runCatching {
                CoreConfig.Crash.allowMultiProcess.ifNext {
                    ifTrue = {
                        CrashActivity.start(application, fileName, throwable)
                    }
                    ifFalse = {
                        RestartActivity.start(application)
                    }
                }
            }.onFailure {
                RestartActivity.start(application)
            }

        } else {
            if (!deadlyCrash) {
                // 如果不是致命的异常就自动重启应用
                RestartActivity.start(application)
            }
        }

        // 不去触发系统的崩溃处理（com.android.internal.os.RuntimeInit$KillApplicationHandler）
        if (nextHandler != null && !nextHandler.javaClass.name
                .startsWith("com.android.internal.os")
        ) {
            nextHandler.uncaughtException(thread, throwable)
        }

        // 杀死进程（这个事应该是系统干的，但是它会多弹出一个崩溃对话框，所以需要我们自己手动杀死进程）
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }
}