package io.core.ui.base.component.activity

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
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
import io.core.R
import io.core.base.appCtx
import io.core.ui.base.component.dialog.showPopupWindow
import io.core.utils.extensions.cool.dp
import io.core.utils.extensions.cool.getFile
import io.core.utils.extensions.cool.hasReadWriteStoragePermission
import io.core.utils.extensions.cool.putBoolean
import io.core.utils.extensions.ui.ScreenOrientation
import io.core.utils.extensions.ui.appVersionCode
import io.core.utils.extensions.ui.appVersionName
import io.core.utils.extensions.ui.onClick
import io.core.utils.extensions.ui.onDebouncedClick
import io.core.utils.share.ShareAir
import io.core.utils.tools.PermissionAir
import io.core.utils.tools.UriTools
import io.core.constant.CRASH_FOLDER_NAME
import io.core.constant.TimePatterns
import io.core.engine.effect.ViewClickEffect
import io.core.other.CrashHandler
import io.core.ui.widget.view.SettingBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Date
import java.util.regex.Pattern
import kotlin.math.min

/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/3/10 16:45
 * @description
 * @author Yuan
 */
class CrashSameProcessActivity : BaseActivity() {

    private val logFile: File? by lazy {
        intent.getStringExtra(INTENT_KEY_LOG_PATH)?.let(::File) ?: appCtx.externalCacheDir?.getFile(
            CRASH_FOLDER_NAME
        )?.listFiles()?.maxByOrNull { it.lastModified() }
    }

    private val sp = appCtx.getSharedPreferences(CrashHandler.CRASH_FILE_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val INTENT_KEY_LOG_PATH = "log_path"
        const val OPEN_PAGE = "open_this_crash_same_process_page"

        /** 报错代码行数正则表达�?*/
        private val CODE_REGEX: Pattern = Pattern.compile("\\(\\w+\\.\\w+:\\d+\\)")

        fun start(context: Context, logPath: String? = null) {
            Intent(context, CrashSameProcessActivity::class.java).apply {
                logPath?.let { putExtra(INTENT_KEY_LOG_PATH, it) }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }.also(context::startActivity)
        }
    }

    private val titleView: TextView? by lazy { findViewById(R.id.tv_crash_title) }
    private val drawerLayout: DrawerLayout? by lazy { findViewById(R.id.dl_crash_drawer) }
    private val infoView: TextView? by lazy { findViewById(R.id.tv_crash_info) }
    private val messageView: TextView? by lazy { findViewById(R.id.tv_crash_message) }
    private var stackTrace: String? = null

    override fun contentViewBind(): View? {
        return LayoutInflater.from(this).inflate(R.layout.activity_core_crash, null)
    }

    override fun initial(savedInstanceState: Bundle?) {
        setTakeOverBackPressed(true)
        super.initial(savedInstanceState)
        // 设置状态栏沉浸
        ImmersionBar.setTitleBar(this, findViewById(R.id.ll_crash_bar))
        ImmersionBar.setTitleBar(this, findViewById(R.id.ll_crash_info))
        ImmersionBar.with(this).statusBarDarkFont(true).init()
        initData()
    }

    private fun initData() {
        lifecycleScope.launch(Dispatchers.IO) {
            val content = logFile?.readText() ?: return@launch
            val substringAfter = content.substringAfter(CrashHandler.DIVIDER).substringAfter("\n")
            val spannable = buildSpannableContent(substringAfter)
            withContext(Dispatchers.Main) {
                titleView?.text = logFile?.name ?: "未知"
                messageView?.text = spannable
                stackTrace = spannable.toString()
                sp.putBoolean(logFile?.name ?: OPEN_PAGE, true)
            }
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
            val dateFormat = TimePatterns.getFormatter("MM-dd HH:mm")
            val packageInfo: PackageInfo =
                packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            builder.append("\n首次安装：\t")
                .append(dateFormat.format(Date(packageInfo.firstInstallTime)))
                .append("\n最近安装：\t").append(dateFormat.format(Date(packageInfo.lastUpdateTime)))
                .append("\n崩溃时间：\t").append(dateFormat.format(Date()))
            val permissions: MutableList<String> =
                mutableListOf(*packageInfo.requestedPermissions ?: emptyArray())
            if (permissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE) ||
                permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            ) {
                builder.append("\n存储权限：\t").append(
                    if (this.hasReadWriteStoragePermission()) "已获�? else "未获�?
                )
            }
            if (permissions.contains(Manifest.permission.ACCESS_FINE_LOCATION) ||
                permissions.contains(Manifest.permission.ACCESS_COARSE_LOCATION)
            ) {
                builder.append("\n定位权限：\t")
                if (PermissionAir.areAllGranted(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                ) {
                    builder.append("精确、粗�?)
                } else {
                    when {
                        PermissionAir.isGranted(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                            builder.append("精确")
                        }

                        PermissionAir.isGranted(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                            builder.append("粗略")
                        }

                        else -> {
                            builder.append("未获�?)
                        }
                    }
                }
            }
            if (permissions.contains(Manifest.permission.CAMERA)) {
                builder.append("\n相机权限：\t")
                    .append(
                        if (PermissionAir.isGranted(
                                Manifest.permission.CAMERA
                            )
                        ) "已获�? else "未获�?
                    )
            }
            if (permissions.contains(Manifest.permission.RECORD_AUDIO)) {
                builder.append("\n录音权限：\t").append(
                    if (PermissionAir.isGranted(
                            Manifest.permission.RECORD_AUDIO
                        )
                    ) "已获�? else "未获�?
                )
            }
            if (permissions.contains(Manifest.permission.SYSTEM_ALERT_WINDOW)) {
                builder.append("\n悬浮窗权限：\t").append(
                    if (PermissionAir.isGranted(
                            Manifest.permission.SYSTEM_ALERT_WINDOW
                        )
                    ) "已获�? else "未获�?
                )
            }
            if (permissions.contains(Manifest.permission.REQUEST_INSTALL_PACKAGES)) {
                builder.append("\n安装包权限：\t").append(
                    if (PermissionAir.isGranted(
                            Manifest.permission.REQUEST_INSTALL_PACKAGES
                        )
                    ) "已获�? else "未获�?
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

    private fun buildSpannableContent(content: String): SpannableStringBuilder {
        val spannable = SpannableStringBuilder(content)

        // 高亮错误类型 (示例：java.lang.NullPointerException)
        val errorRegex = Pattern.compile("[A-Za-z]+\\.?[A-Za-z]+Exception")
        applySpanForMatches(errorRegex, spannable, "#FF3B30")

        // 高亮代码行号 (示例：at com.example.TestActivity.onCreate(TestActivity.kt:12))
        applySpanForMatches(CODE_REGEX, spannable, "#287BDE")

        // 高亮关键参数 (示例：MODEL = Pixel 3)
        val paramRegex = Pattern.compile("^[A-Z_]+\\s=")
        applySpanForMatches(paramRegex, spannable, "#34C759")

        return spannable
    }

    private fun applySpanForMatches(
        pattern: Pattern,
        spannable: SpannableStringBuilder,
        color: String
    ) {
        val matcher = pattern.matcher(spannable)
        while (matcher.find()) {
            spannable.setSpan(
                ForegroundColorSpan(Color.parseColor(color)),
                matcher.start(),
                matcher.end(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
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
                setSize(200.dp, ViewGroup.LayoutParams.WRAP_CONTENT)
                setViewInitializer { _ ->
                    val tvShare = findViewById<SettingBar>(R.id.text)
                    val tvLog = findViewById<SettingBar>(R.id.log)

                    tvShare.onClick {
                        ShareAir.share {
                            text(stackTrace ?: "")
                        }
                    }

                    tvLog.onClick {
                        logFile?.let {
                            lifecycleScope.launch(Dispatchers.IO) {
                                ShareAir.share {
                                    file(UriTools.file2Uri(it))
                                }
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
        return super.createStatusBarConfig() // 指定导航栏背景颜�?
            .navigationBarColor(R.color.white)
    }

    override fun getLockOrientation(): ScreenOrientation {
        return ScreenOrientation.HORIZONTAL
    }

}