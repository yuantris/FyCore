package io.core.common.base.component.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextUtils
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.DisplayMetrics
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import com.gyf.immersionbar.ImmersionBar
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import io.core.R
import io.core.appCtx
import io.core.common.base.component.dialog.showPopupWindow
import io.core.common.util.FileSharer
import io.core.common.util.ShareAir
import io.core.common.util.extensions.cool.dp
import io.core.common.util.extensions.cool.getFile
import io.core.common.util.extensions.cool.hasReadWriteStoragePermission
import io.core.common.util.extensions.ui.appVersionCode
import io.core.common.util.extensions.ui.appVersionName
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.extensions.ui.onDebouncedClick
import io.core.constant.CRASH_FOLDER_NAME
import io.core.constant.TimeFormat
import io.core.engine.effect.ViewClickEffect
import io.core.widget.view.SettingBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Date
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.min

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/11 8:39
 * @description
 * @author Yuan
 */
class CrashActivity : BaseActivity() {

    companion object {

        private const val INTENT_KEY_IN_THROWABLE: String = "throwable"
        private const val INTENT_KEY_IN_LOG_FILE_NAME: String = "log_file"

        /** 系统包前缀列表 */
        private val SYSTEM_PACKAGE_PREFIX_LIST: Array<String> = arrayOf(
            "android", "com.android",
            "androidx", "com.google.android", "java", "javax", "dalvik", "kotlin"
        )

        /** 报错代码行数正则表达式 */
        private val CODE_REGEX: Pattern = Pattern.compile("\\(\\w+\\.\\w+:\\d+\\)")

        fun start(application: Application, log: String, throwable: Throwable?) {
            if (throwable == null) {
                return
            }
            Intent(application, CrashActivity::class.java).apply {
                putExtra(INTENT_KEY_IN_THROWABLE, throwable)
                putExtra(INTENT_KEY_IN_LOG_FILE_NAME, log)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.also {
                application.startActivity(it)
            }
        }
    }

    private val titleView: TextView? by lazy { findViewById(R.id.tv_crash_title) }
    private val drawerLayout: DrawerLayout? by lazy { findViewById(R.id.dl_crash_drawer) }
    private val infoView: TextView? by lazy { findViewById(R.id.tv_crash_info) }
    private val messageView: TextView? by lazy { findViewById(R.id.tv_crash_message) }
    private var stackTrace: String? = null
    private var logFiles: List<File>? = null

    @SuppressLint("InflateParams")
    override fun contentViewBind(): View? {
        return LayoutInflater.from(this).inflate(R.layout.activity_core_crash, null)
    }

    override fun initial(savedInstanceState: Bundle?) {
        setTakeOverBackPressed(true)
        super.initial(savedInstanceState)
        // 设置状态栏沉浸
        ImmersionBar.setTitleBar(this, findViewById(R.id.ll_crash_bar))
        ImmersionBar.setTitleBar(this, findViewById(R.id.ll_crash_info))

        initData()
    }

    private fun initData() {
        val throwable: Throwable = getSerializable(INTENT_KEY_IN_THROWABLE) ?: return
        val logFileName = getString(INTENT_KEY_IN_LOG_FILE_NAME)
        logFiles = listOf(
            File(
                appCtx.externalCacheDir?.getFile(CRASH_FOLDER_NAME),
                logFileName ?: "crash.log"
            )
        )
        titleView?.text = throwable.javaClass.simpleName
        val stringWriter = StringWriter()
        val printWriter = PrintWriter(stringWriter)
        throwable.printStackTrace(printWriter)
        throwable.cause?.printStackTrace(printWriter)
        stackTrace = stringWriter.toString()
        val matcher: Matcher = CODE_REGEX.matcher(stackTrace!!)
        val spannable = SpannableStringBuilder(stackTrace)
        if (spannable.isNotEmpty()) {
            while (matcher.find()) {
                // 不包含左括号（
                val start: Int = matcher.start() + "(".length
                // 不包含右括号 ）
                val end: Int = matcher.end() - ")".length

                // 代码信息颜色
                var codeColor: Int = Color.parseColor("#999999")
                val lineIndex: Int = stackTrace!!.lastIndexOf("at ", start)
                if (lineIndex != -1) {
                    val lineData: String = spannable.subSequence(lineIndex, start).toString()
                    if (TextUtils.isEmpty(lineData)) {
                        continue
                    }
                    // 是否高亮代码行数
                    var highlight = true
                    for (packagePrefix: String? in SYSTEM_PACKAGE_PREFIX_LIST) {
                        if (lineData.startsWith("at $packagePrefix")) {
                            highlight = false
                            break
                        }
                    }
                    if (highlight) {
                        codeColor = Color.parseColor("#287BDE")
                    }
                }

                // 设置前景
                spannable.setSpan(
                    ForegroundColorSpan(codeColor),
                    start,
                    end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                // 设置下划线
                spannable.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            messageView?.text = spannable
        }
        val displayMetrics: DisplayMetrics = resources.displayMetrics
        val screenWidth: Int = displayMetrics.widthPixels
        val screenHeight: Int = displayMetrics.heightPixels
        val smallestWidth: Float = min(screenWidth, screenHeight) / displayMetrics.density
        val targetResource: String?
        when {
            displayMetrics.densityDpi > 480 -> {
                targetResource = "xxxhdpi"
            }

            displayMetrics.densityDpi > 320 -> {
                targetResource = "xxhdpi"
            }

            displayMetrics.densityDpi > 240 -> {
                targetResource = "xhdpi"
            }

            displayMetrics.densityDpi > 160 -> {
                targetResource = "hdpi"
            }

            displayMetrics.densityDpi > 120 -> {
                targetResource = "mdpi"
            }

            else -> {
                targetResource = "ldpi"
            }
        }
        val builder: StringBuilder = StringBuilder()
        builder.append("设备品牌：\t").append(Build.BRAND)
            .append("\n设备型号：\t").append(Build.MODEL)

        builder.append("\n屏幕宽高：\t").append(screenWidth).append(" x ").append(screenHeight)
            .append("\n屏幕密度：\t").append(displayMetrics.densityDpi)
            .append("\n密度像素：\t").append(displayMetrics.density)
            .append("\n目标资源：\t").append(targetResource)
            .append("\n最小宽度：\t").append(smallestWidth.toInt())

        builder.append("\n安卓版本：\t").append(Build.VERSION.RELEASE)
            .append("\nAPI 版本：\t").append(Build.VERSION.SDK_INT)
            .append("\nCPU 架构：\t").append(Build.SUPPORTED_ABIS[0])

        builder.append("\n应用版本：\t").append(appVersionName)
            .append("\n版本代码：\t").append(appVersionCode)

        try {
            val dateFormat = TimeFormat.getFormatter("MM-dd HH:mm")
            val packageInfo: PackageInfo =
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            builder.append("\n首次安装：\t")
                .append(dateFormat.format(Date(packageInfo.firstInstallTime)))
                .append("\n最近安装：\t").append(dateFormat.format(Date(packageInfo.lastUpdateTime)))
                .append("\n崩溃时间：\t").append(dateFormat.format(Date()))
            val permissions: MutableList<String> =
                mutableListOf(*packageInfo.requestedPermissions ?: emptyArray())
            if (permissions.contains(Permission.READ_EXTERNAL_STORAGE) ||
                permissions.contains(Permission.WRITE_EXTERNAL_STORAGE)
            ) {
                builder.append("\n存储权限：\t").append(
                    if (this.hasReadWriteStoragePermission()) "已获得" else "未获得"
                )
            }
            if (permissions.contains(Permission.ACCESS_FINE_LOCATION) ||
                permissions.contains(Permission.ACCESS_COARSE_LOCATION)
            ) {
                builder.append("\n定位权限：\t")
                if (XXPermissions.isGranted(
                        this,
                        Permission.ACCESS_FINE_LOCATION,
                        Permission.ACCESS_COARSE_LOCATION
                    )
                ) {
                    builder.append("精确、粗略")
                } else {
                    when {
                        XXPermissions.isGranted(this, Permission.ACCESS_FINE_LOCATION) -> {
                            builder.append("精确")
                        }

                        XXPermissions.isGranted(this, Permission.ACCESS_COARSE_LOCATION) -> {
                            builder.append("粗略")
                        }

                        else -> {
                            builder.append("未获得")
                        }
                    }
                }
            }
            if (permissions.contains(Permission.CAMERA)) {
                builder.append("\n相机权限：\t")
                    .append(
                        if (XXPermissions.isGranted(
                                this,
                                Permission.CAMERA
                            )
                        ) "已获得" else "未获得"
                    )
            }
            if (permissions.contains(Permission.RECORD_AUDIO)) {
                builder.append("\n录音权限：\t").append(
                    if (XXPermissions.isGranted(
                            this,
                            Permission.RECORD_AUDIO
                        )
                    ) "已获得" else "未获得"
                )
            }
            if (permissions.contains(Permission.SYSTEM_ALERT_WINDOW)) {
                builder.append("\n悬浮窗权限：\t").append(
                    if (XXPermissions.isGranted(
                            this,
                            Permission.SYSTEM_ALERT_WINDOW
                        )
                    ) "已获得" else "未获得"
                )
            }
            if (permissions.contains(Permission.REQUEST_INSTALL_PACKAGES)) {
                builder.append("\n安装包权限：\t").append(
                    if (XXPermissions.isGranted(
                            this,
                            Permission.REQUEST_INSTALL_PACKAGES
                        )
                    ) "已获得" else "未获得"
                )
            }
            if (permissions.contains(Manifest.permission.INTERNET)) {
                builder.append("\n当前网络访问：\t")

                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        InetAddress.getByName("www.baidu.com")
                        builder.append("正常")
                    } catch (ignored: UnknownHostException) {
                        builder.append("异常")
                    }
                    lifecycleScope.launch(Dispatchers.Main) {
                        infoView?.text = builder
                    }
                }
            } else {
                infoView?.text = builder
            }
        } catch (e: PackageManager.NameNotFoundException) {
            // CrashReport.postCatchedException(e)
        }
    }

    override fun setListener() {
        val info = findViewById<ImageView>(R.id.iv_crash_info)
        val share = findViewById<ImageView>(R.id.iv_crash_share)
        val restart = findViewById<ImageView>(R.id.iv_crash_restart)
        info.onDebouncedClick {
            drawerLayout?.openDrawer(GravityCompat.START)
        }
        ViewClickEffect.applyScaleToViews(share)
        share.onDebouncedClick {
            // 分享文本
            share.showPopupWindow {
                setLayout(R.layout.popup_crash_log_share)
                setSize(200.dp(), ViewGroup.LayoutParams.WRAP_CONTENT)
                setViewInitializer { _ ->
                    val tvShare = findViewById<SettingBar>(R.id.text)
                    val tvLog = findViewById<SettingBar>(R.id.log)

                    tvShare.onClick {
                        ShareAir.share {
                            text(stackTrace.orEmpty())
                        }
                    }

                    tvLog.onClick {
                        logFiles?.let {
                            lifecycleScope.launch(Dispatchers.IO) {
                                FileSharer.Builder()
                                    .setChooserTitle("分享崩溃日志")
                                    .setFileList(it)
                                    .share(this@CrashActivity)
                            }
                        }
                    }
                }
            }
        }
        restart.onDebouncedClick {
            onBackPressedCall()
        }
    }

    override fun onBackPressedCall() {
        finish()
        // 重启应用
        RestartActivity.restart(this)
    }

    override fun createStatusBarConfig(): ImmersionBar {
        return super.createStatusBarConfig() // 指定导航栏背景颜色
            .navigationBarColor(R.color.white)
    }


}