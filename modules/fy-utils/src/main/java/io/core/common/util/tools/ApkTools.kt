package io.core.common.util.tools

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import io.core.appCtx

/**
 * APK包名工具类，封装常用APK包名相关操作
 * 包含常见应用包名常量
 */
object ApkTools {

    private val packageManager by lazy { appCtx.packageManager }
    private val installedAppsCache = mutableMapOf<String, PackageInfo?>()

    // 常见社交应用包名
    object Social {
        const val WECHAT = "com.tencent.mm"
        const val QQ = "com.tencent.mobileqq"
        const val WEIBO = "com.sina.weibo"
        const val FACEBOOK = "com.facebook.katana"
        const val TWITTER = "com.twitter.android"
        const val INSTAGRAM = "com.instagram.android"
        const val WHATSAPP = "com.whatsapp"
        const val TIKTOK = "com.zhiliaoapp.musically"
    }

    // 常见浏览器包名
    object Browser {
        const val CHROME = "com.android.chrome"
        const val FIREFOX = "org.mozilla.firefox"
        const val UC = "com.UCMobile"
        const val QQ_BROWSER = "com.tencent.mtt"
        const val OPERA = "com.opera.browser"
        const val EDGE = "com.microsoft.emmx"
    }

    // 常见购物应用包名
    object Shopping {
        const val TAOBAO = "com.taobao.taobao"
        const val JD = "com.jingdong.app.mall"
        const val PINDUODUO = "com.xunmeng.pinduoduo"
        const val AMAZON = "com.amazon.mShop.android.shopping"
        const val EBAY = "com.ebay.mobile"
    }

    // 常见视频应用包名
    object Video {
        const val YOUTUBE = "com.google.android.youtube"
        const val YOUKU = "com.youku.phone"
        const val IQIYI = "com.qiyi.video"
        const val TENCENT_VIDEO = "com.tencent.qqlive"
        const val BILIBILI = "tv.danmaku.bili"
    }

    // 常见音频应用包名
    object Music {
        const val SPOTIFY = "com.spotify.music"
        const val QQ_MUSIC = "com.tencent.qqmusic"
        const val NET_EASE = "com.netease.cloudmusic"
    }

    // 常见地图应用包名
    object Map {
        const val GAODE = "com.autonavi.minimap"
        const val BAIDU = "com.baidu.BaiduMap"
        const val GOOGLE_MAPS = "com.google.android.apps.maps"
    }

    // 常见支付应用包名
    object Payment {
        const val ALIPAY = "com.eg.android.AlipayGphone"
        const val PAYPAL = "com.paypal.android.p2pmobile"
        const val WECHAT_PAY = "com.tencent.mm" // 微信支付与微信相同
    }

    // 常见邮件应用包名
    object Email {
        const val GMAIL = "com.google.android.gm"
        const val OUTLOOK = "com.microsoft.office.outlook"
        const val QQ_MAIL = "com.tencent.androidqqmail"
    }

    /**
     * 检查应用是否已安装
     * @param packageName 要检查的包名
     * @return Boolean 是否已安装
     */
    @JvmStatic
    fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * 获取应用版本名称
     * @param packageName 包名
     * @return String? 版本名称，未安装返回null
     */
    @JvmStatic
    fun getAppVersionName(packageName: String): String? {
        return try {
            val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * 获取应用版本号
     * @param packageName 包名
     * @return Int 版本号，未安装返回-1
     */
    @JvmStatic
    fun getAppVersionCode(packageName: String): Int {
        return try {
            val pInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
            pInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            -1
        }
    }

    /**
     * 打开应用
     * @param packageName 要打开的包名
     * @return Boolean 是否成功打开
     */
    @JvmStatic
    fun openApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appCtx.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 跳转到应用商店
     * @param packageName 包名
     */
    @JvmStatic
    fun openAppInMarket(packageName: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appCtx.startActivity(intent)
        } catch (e: Exception) {
            // 如果没有应用商店，使用网页版
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            appCtx.startActivity(intent)
        }
    }

    /**
     * 跳转到应用详情页
     * @param packageName 包名
     */
    @JvmStatic
    fun openAppDetails(packageName: String) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appCtx.startActivity(intent)
    }

    /**
     * 获取设备上已安装的应用列表
     * @return List<PackageInfo> 已安装的应用列表
     */
    @JvmStatic
    @SuppressLint("QueryPermissionsNeeded")
    fun getInstalledApps(forceRefresh: Boolean = false): List<PackageInfo> {
        if (forceRefresh || installedAppsCache.isEmpty()) {
            installedAppsCache.clear()
            packageManager.getInstalledPackages(0).forEach {
                installedAppsCache[it.packageName] = it
            }
        }
        return installedAppsCache.values.filterNotNull()
    }

    /**
     * 检查是否是系统应用
     * @param packageName 包名
     * @return Boolean 是否是系统应用
     */
    @JvmStatic
    fun isSystemApp(packageName: String): Boolean {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            (pInfo.applicationInfo?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}