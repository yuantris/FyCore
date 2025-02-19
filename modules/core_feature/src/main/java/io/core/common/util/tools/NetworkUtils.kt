package io.core.common.util.tools

import android.annotation.SuppressLint
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import io.core.common.util.extensions.connectivityManager
import io.core.common.util.extensions.cool.isAbsUrl
import io.core.common.util.extensions.cool.isDataUrl
import io.core.common.util.log.AppLog
import io.core.common.util.log.printOnDebug
import java.net.URL
import java.util.BitSet

@Suppress("unused", "MemberVisibilityCanBePrivate")
object NetworkUtils {

    /**
     * 判断是否联网
     */
    @SuppressLint("ObsoleteSdkInt", "MissingPermission")
    @Suppress("DEPRECATION")
    fun isAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < 23) {
            val mWiFiNetworkInfo = connectivityManager.activeNetworkInfo
            if (mWiFiNetworkInfo != null) {
                // WIFI
                return mWiFiNetworkInfo.type == ConnectivityManager.TYPE_WIFI ||
                        // 移动数据
                        mWiFiNetworkInfo.type == ConnectivityManager.TYPE_MOBILE ||
                        // 以太网
                        mWiFiNetworkInfo.type == ConnectivityManager.TYPE_ETHERNET ||
                        // VPN
                        mWiFiNetworkInfo.type == ConnectivityManager.TYPE_VPN
            }
        } else {
            val network = connectivityManager.activeNetwork
            if (network != null) {
                val nc = connectivityManager.getNetworkCapabilities(network)
                if (nc != null) {
                    // WIFI
                    return nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            // 移动数据
                            nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            // 以太网
                            nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                            // VPN
                            nc.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
                }
            }
        }
        return false
    }

    private val notNeedEncoding: BitSet by lazy {
        val bitSet = BitSet(256)
        for (i in 'a'.code..'z'.code) {
            bitSet.set(i)
        }
        for (i in 'A'.code..'Z'.code) {
            bitSet.set(i)
        }
        for (i in '0'.code..'9'.code) {
            bitSet.set(i)
        }
        for (char in "+-_.~$:()!*@&#,[]") {
            bitSet.set(char.code)
        }
        return@lazy bitSet
    }

    /**
     * 支持JAVA的URLEncoder.encode出来的string做判断。 即: 将' '转成'+'
     * 0-9a-zA-Z保留 <br></br>
     * ! * ' ( ) ; : @ & = + $ , / ? # [ ] 保留
     * 其他字符转成%XX的格式，X是16进制的大写字符，范围是[0-9A-F]
     */
    fun hasUrlEncoded(str: String): Boolean {
        var needEncode = false
        var i = 0
        while (i < str.length) {
            val c = str[i]
            if (notNeedEncoding.get(c.code)) {
                i++
                continue
            }
            if (c == '%' && i + 2 < str.length) {
                // 判断是否符合urlEncode规范
                val c1 = str[++i]
                val c2 = str[++i]
                if (isDigit16Char(c1) && isDigit16Char(c2)) {
                    i++
                    continue
                }
            }
            // 其他字符，肯定需要urlEncode
            needEncode = true
            break
        }

        return !needEncode
    }

    /**
     * 判断c是否是16进制的字符
     */
    private fun isDigit16Char(c: Char): Boolean {
        return c in '0'..'9' || c in 'A'..'F' || c in 'a'..'f'
    }

    /**
     * 获取绝对地址
     */
    fun getAbsoluteURL(baseURL: String?, relativePath: String): String {
        if (baseURL.isNullOrEmpty()) return relativePath.trim()
        var absoluteUrl: URL? = null
        try {
            absoluteUrl = URL(baseURL.substringBefore(","))
        } catch (e: Exception) {
            e.printOnDebug()
        }
        return getAbsoluteURL(absoluteUrl, relativePath)
    }

    /**
     * 获取绝对地址
     */
    fun getAbsoluteURL(baseURL: URL?, relativePath: String): String {
        val relativePathTrim = relativePath.trim()
        if (baseURL == null) return relativePathTrim
        if (relativePathTrim.isAbsUrl()) return relativePathTrim
        if (relativePathTrim.isDataUrl()) return relativePathTrim
        if (relativePathTrim.startsWith("javascript")) return ""
        var relativeUrl = relativePathTrim
        try {
            val parseUrl = URL(baseURL, relativePath)
            relativeUrl = parseUrl.toString()
            return relativeUrl
        } catch (e: Exception) {
            AppLog.put("网址拼接出错\n${e.localizedMessage}", e)
        }
        return relativeUrl
    }

    fun getBaseUrl(url: String?): String? {
        url ?: return null
        if (url.startsWith("http://", true)
            || url.startsWith("https://", true)
        ) {
            val index = url.indexOf("/", 9)
            return if (index == -1) {
                url
            } else url.substring(0, index)
        }
        return null
    }

    fun getDomain(url: String): String {
        val baseUrl = getBaseUrl(url) ?: return url
        return kotlin.runCatching {
            URL(baseUrl).host
        }.getOrDefault(baseUrl)
    }


}