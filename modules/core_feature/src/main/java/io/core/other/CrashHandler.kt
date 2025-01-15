package io.core.other

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Debug
import android.os.Environment
import android.os.Process
import android.webkit.WebSettings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.core.Android
import io.core.common.base.component.activity.CrashActivity
import io.core.common.base.component.activity.RestartActivity
import io.core.common.exception.NoStackTraceException
import io.core.common.util.FileDoc
import io.core.common.util.ext.appCtx
import io.core.common.util.ext.cool.createFolderReplace
import io.core.common.util.ext.cool.getFile
import io.core.common.util.ext.ui.externalCache
import io.core.common.util.tools.FileUtils
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

class CrashHandler private constructor(private val application: Application) :
    Thread.UncaughtExceptionHandler {

    companion object {

        /** Crash 文件名 */
        private const val CRASH_FILE_NAME: String = "crash_file"

        /** Crash 时间记录 */
        private const val KEY_CRASH_TIME: String = "key_crash_time"

        /**
         * 注册 Crash 监听
         */
        fun register(application: Application) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(application))
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
                map["packageName"] = appCtx.packageName
            }
            map
        }

        /**
         * 格式化时间
         */
        @SuppressLint("SimpleDateFormat")
        private val format = SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")

        /**
         * 保存错误信息到文件中
         */
        fun saveCrashInfo2File(ex: Throwable) {
            val sb = StringBuilder()
            for ((key, value) in paramsMap) {
                sb.append(key).append("=").append(value).append("\n")
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
            sb.append(result)
            val crashLog = sb.toString()
            val timestamp = System.currentTimeMillis()
            val time = format.format(Date())
            val fileName = "crash-$time-$timestamp.log"
            kotlin.runCatching {
                appCtx.externalCacheDir?.let { rootFile ->
                    val exceedTimeMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                    rootFile.getFile("crash").listFiles()?.forEach {
                        if (it.lastModified() < exceedTimeMillis) {
                            it.delete()
                        }
                    }
                    FileUtils.createFileIfNotExist(rootFile, "crash", fileName)
                        .writeText(crashLog)

                    // 写入外置存储
                    if (ContextCompat.checkSelfPermission(
                            appCtx,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                        )
                        == PackageManager.PERMISSION_GRANTED
                    ) {
                        FileUtils.createFileIfNotExist(
                            Environment.getExternalStorageDirectory(),
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).name,
                            fileName
                        ).writeText(crashLog)
                    }
                }
            }
        }

        /**
         * 进行堆转储
         */
        fun doHeapDump(manually: Boolean = false) {
            val heapDir = appCtx
                .externalCache
                .getFile("heapDump")
            heapDir.createFolderReplace()
            val fileName = if (manually) {
                "heap-dump-manually-${System.currentTimeMillis()}.hprof"
            } else {
                "heap-dump-${System.currentTimeMillis()}.hprof"
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
        val currentCrashTime: Long = System.currentTimeMillis()
        val lastCrashTime: Long = sharedPreferences.getLong(KEY_CRASH_TIME, 0)
        // 记录当前崩溃的时间，以便下次崩溃时进行比对
        sharedPreferences.edit().putLong(KEY_CRASH_TIME, currentCrashTime).commit()

        // 保存崩溃信息
        saveCrashInfo2File(throwable)
        if ((throwable is OutOfMemoryError || throwable.cause is OutOfMemoryError)) {
            doHeapDump()
        }

        // 致命异常标记：如果上次崩溃的时间距离当前崩溃小于 5 分钟，那么判定为致命异常
        val deadlyCrash: Boolean = currentCrashTime - lastCrashTime < 1000 * 60 * 5
        if (Android.debug) {
            CrashActivity.start(application, throwable)
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