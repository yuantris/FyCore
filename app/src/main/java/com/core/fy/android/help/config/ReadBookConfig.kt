package com.core.fy.android.help.config

import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import androidx.annotation.Keep
import androidx.core.graphics.drawable.toDrawable
import androidx.core.graphics.toColorInt
import com.core.fy.android.R
import com.core.fy.android.constants.PageAnim
import com.core.fy.android.constants.PreferKey
import io.core.base.appCtx
import io.core.utils.coroutine.Coroutine
import io.core.utils.extensions.cool.GSON
import io.core.utils.extensions.cool.PathType
import io.core.utils.extensions.cool.fromJsonArray
import io.core.utils.extensions.cool.fromJsonObject
import io.core.utils.extensions.cool.getBasePath
import io.core.utils.extensions.cool.getMeanColor
import io.core.utils.extensions.cool.hexString
import io.core.utils.extensions.cool.printOnDebug
import io.core.utils.extensions.cool.resizeAndRecycle
import io.core.utils.extensions.ui.getCompatColor
import io.core.utils.extensions.ui.getPrefBoolean
import io.core.utils.extensions.ui.getPrefInt
import io.core.utils.extensions.ui.putPrefBoolean
import io.core.utils.extensions.ui.putPrefInt
import io.core.utils.log.LogCat
import io.core.utils.tools.BitmapTools
import io.core.utils.tools.FileTools
import java.io.File

/**
 * 阅读界面配置
 */
@Suppress("ConstPropertyName")
@Keep
object ReadBookConfig {
    const val configFileName = "readConfig.json"
    const val shareConfigFileName = "shareReadConfig.json"
    val configFilePath = FileTools.getPath(appCtx.filesDir, configFileName)
    val shareConfigFilePath = FileTools.getPath(appCtx.filesDir, shareConfigFileName)
    val configList: ArrayList<Config> = arrayListOf()
    lateinit var shareConfig: Config
    var durConfig
        get() = getConfig(styleSelect)
        set(value) {
            configList[styleSelect] = value
            if (shareLayout) {
                shareConfig = value
            }
        }

    var isComic: Boolean = false
    var bg: Drawable? = null
    var bgMeanColor: Int = 0
    val textColor: Int get() = durConfig.curTextColor()

    init {
        initConfigs()
        initShareConfig()
    }

    @Synchronized
    fun getConfig(index: Int): Config {
        if (configList.size < 5) {
            resetAll()
        }
        return configList.getOrNull(index) ?: configList[0]
    }

    fun initConfigs() {
        val configFile = File(configFilePath)
        var configs: List<Config>? = null
        if (configFile.exists()) {
            try {
                val json = configFile.readText()
                configs = GSON.fromJsonArray<Config>(json).getOrThrow()
            } catch (e: Exception) {
                LogCat.e(e, msg = "读取排版配置文件出错")
            }
        }
        (configs ?: DefaultData.readConfigs).let {
            configList.clear()
            configList.addAll(it)
        }
    }

    fun initShareConfig() {
        val configFile = File(shareConfigFilePath)
        var c: Config? = null
        if (configFile.exists()) {
            try {
                val json = configFile.readText()
                c = GSON.fromJsonObject<Config>(json).getOrThrow()
            } catch (e: Exception) {
                e.printOnDebug()
            }
        }
        shareConfig = c ?: configList.getOrNull(5) ?: Config()
    }

    fun upBg(width: Int, height: Int) {
        val drawable = durConfig.curBgDrawable(width, height)
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            bgMeanColor = drawable.bitmap.getMeanColor()
        } else if (drawable is ColorDrawable) {
            bgMeanColor = drawable.color
        }
        val tmp = bg
        bg = drawable
        (tmp as? BitmapDrawable)?.bitmap?.recycle()
    }

    fun save() {
        Coroutine.async {
            synchronized(this) {
                GSON.toJson(configList).let {
                    FileTools.delete(configFilePath)
                    FileTools.createFileIfNotExist(configFilePath).writeText(it)
                }
                GSON.toJson(shareConfig).let {
                    FileTools.delete(shareConfigFilePath)
                    FileTools.createFileIfNotExist(shareConfigFilePath).writeText(it)
                }
            }
        }
    }

    fun getAllPicBgStr(): ArrayList<String> {
        val list = arrayListOf<String>()
        configList.forEach {
            if (it.bgType == 2) {
                list.add(it.bgStr)
            }
            if (it.bgTypeNight == 2) {
                list.add(it.bgStrNight)
            }
            if (it.bgTypeEInk == 2) {
                list.add(it.bgStrEInk)
            }
        }
        return list
    }

    fun deleteDur(): Boolean {
        if (configList.size > 5) {
            val removeIndex = styleSelect
            configList.removeAt(removeIndex)
            if (removeIndex <= readStyleSelect) {
                readStyleSelect -= 1
            }
            if (removeIndex <= comicStyleSelect) {
                comicStyleSelect -= 1
            }
            return true
        }
        return false
    }

    private fun resetAll() {
        DefaultData.readConfigs.let {
            configList.clear()
            configList.addAll(it)
            save()
        }
    }

    //配置写入读取
    var readBodyToLh = appCtx.getPrefBoolean(PreferKey.readBodyToLh, true)
    var autoReadSpeed = appCtx.getPrefInt(PreferKey.autoReadSpeed, 10)
        set(value) {
            field = value
            appCtx.putPrefInt(PreferKey.autoReadSpeed, value)
        }
    var styleSelect: Int
        get() = if (isComic) comicStyleSelect else readStyleSelect
        set(value) {
            if (isComic) {
                comicStyleSelect = value
            } else {
                readStyleSelect = value
            }
        }
    var readStyleSelect = appCtx.getPrefInt(PreferKey.readStyleSelect)
        set(value) {
            field = value
            if (appCtx.getPrefInt(PreferKey.readStyleSelect) != value) {
                appCtx.putPrefInt(PreferKey.readStyleSelect, value)
            }
        }
    var comicStyleSelect = appCtx.getPrefInt(PreferKey.comicStyleSelect, readStyleSelect)
        set(value) {
            field = value
            if (appCtx.getPrefInt(PreferKey.comicStyleSelect) != value) {
                appCtx.putPrefInt(PreferKey.comicStyleSelect, value)
            }
        }
    var shareLayout = appCtx.getPrefBoolean(PreferKey.shareLayout)
        set(value) {
            field = value
            if (appCtx.getPrefBoolean(PreferKey.shareLayout) != value) {
                appCtx.putPrefBoolean(PreferKey.shareLayout, value)
            }
        }

    /**
     * 两端对齐
     */
    val textFullJustify get() = appCtx.getPrefBoolean(PreferKey.textFullJustify, true)

    /**
     * 底部对齐
     */
    val textBottomJustify get() = appCtx.getPrefBoolean(PreferKey.textBottomJustify, true)
    var hideStatusBar = appCtx.getPrefBoolean(PreferKey.hideStatusBar)
    var hideNavigationBar = appCtx.getPrefBoolean(PreferKey.hideNavigationBar)
    var useZhLayout = appCtx.getPrefBoolean(PreferKey.useZhLayout)

    val config get() = if (shareLayout) shareConfig else durConfig

    var bgAlpha: Int
        get() = config.bgAlpha
        set(value) {
            config.bgAlpha = value
        }

    var pageAnim: Int
        get() = config.curPageAnim()
        set(@PageAnim.Anim value) {
            config.setCurPageAnim(value)
        }

    var textFont: String
        get() = config.textFont
        set(value) {
            config.textFont = value
        }

    var textBold: Int
        get() = config.textBold
        set(value) {
            config.textBold = value
        }

    var textSize: Int
        get() = config.textSize
        set(value) {
            config.textSize = value
        }

    var letterSpacing: Float
        get() = config.letterSpacing
        set(value) {
            config.letterSpacing = value
        }

    var lineSpacingExtra: Int
        get() = config.lineSpacingExtra
        set(value) {
            config.lineSpacingExtra = value
        }

    var paragraphSpacing: Int
        get() = config.paragraphSpacing
        set(value) {
            config.paragraphSpacing = value
        }

    /**
     * 标题位置 0:居左 1:居中 2:隐藏
     */
    var titleMode: Int
        get() = config.titleMode
        set(value) {
            config.titleMode = value
        }
    var titleSize: Int
        get() = config.titleSize
        set(value) {
            config.titleSize = value
        }

    /**
     * 是否标题居中
     */
    val isMiddleTitle get() = titleMode == 1

    var titleTopSpacing: Int
        get() = config.titleTopSpacing
        set(value) {
            config.titleTopSpacing = value
        }

    var titleBottomSpacing: Int
        get() = config.titleBottomSpacing
        set(value) {
            config.titleBottomSpacing = value
        }

    var paragraphIndent: String
        get() = config.paragraphIndent
        set(value) {
            config.paragraphIndent = value
        }

    var underline: Boolean
        get() = config.underline
        set(value) {
            config.underline = value
        }

    var paddingBottom: Int
        get() = config.paddingBottom
        set(value) {
            config.paddingBottom = value
        }

    var paddingLeft: Int
        get() = config.paddingLeft
        set(value) {
            config.paddingLeft = value
        }

    var paddingRight: Int
        get() = config.paddingRight
        set(value) {
            config.paddingRight = value
        }

    var paddingTop: Int
        get() = config.paddingTop
        set(value) {
            config.paddingTop = value
        }

    var headerPaddingBottom: Int
        get() = config.headerPaddingBottom
        set(value) {
            config.headerPaddingBottom = value
        }

    var headerPaddingLeft: Int
        get() = config.headerPaddingLeft
        set(value) {
            config.headerPaddingLeft = value
        }

    var headerPaddingRight: Int
        get() = config.headerPaddingRight
        set(value) {
            config.headerPaddingRight = value
        }

    var headerPaddingTop: Int
        get() = config.headerPaddingTop
        set(value) {
            config.headerPaddingTop = value
        }

    var footerPaddingBottom: Int
        get() = config.footerPaddingBottom
        set(value) {
            config.footerPaddingBottom = value
        }

    var footerPaddingLeft: Int
        get() = config.footerPaddingLeft
        set(value) {
            config.footerPaddingLeft = value
        }

    var footerPaddingRight: Int
        get() = config.footerPaddingRight
        set(value) {
            config.footerPaddingRight = value
        }

    var footerPaddingTop: Int
        get() = config.footerPaddingTop
        set(value) {
            config.footerPaddingTop = value
        }

    var showHeaderLine: Boolean
        get() = config.showHeaderLine
        set(value) {
            config.showHeaderLine = value
        }

    var showFooterLine: Boolean
        get() = config.showFooterLine
        set(value) {
            config.showFooterLine = value
        }

    fun getExportConfig(): Config {
        val exportConfig = durConfig.copy()
        if (shareLayout) {
            exportConfig.textFont = shareConfig.textFont
            exportConfig.textBold = shareConfig.textBold
            exportConfig.textSize = shareConfig.textSize
            exportConfig.letterSpacing = shareConfig.letterSpacing
            exportConfig.lineSpacingExtra = shareConfig.lineSpacingExtra
            exportConfig.paragraphSpacing = shareConfig.paragraphSpacing
            exportConfig.titleMode = shareConfig.titleMode
            exportConfig.titleSize = shareConfig.titleSize
            exportConfig.titleTopSpacing = shareConfig.titleTopSpacing
            exportConfig.titleBottomSpacing = shareConfig.titleBottomSpacing
            exportConfig.paddingBottom = shareConfig.paddingBottom
            exportConfig.paddingLeft = shareConfig.paddingLeft
            exportConfig.paddingRight = shareConfig.paddingRight
            exportConfig.paddingTop = shareConfig.paddingTop
            exportConfig.headerPaddingBottom = shareConfig.headerPaddingBottom
            exportConfig.headerPaddingLeft = shareConfig.headerPaddingLeft
            exportConfig.headerPaddingRight = shareConfig.headerPaddingRight
            exportConfig.headerPaddingTop = shareConfig.headerPaddingTop
            exportConfig.footerPaddingBottom = shareConfig.footerPaddingBottom
            exportConfig.footerPaddingLeft = shareConfig.footerPaddingLeft
            exportConfig.footerPaddingRight = shareConfig.footerPaddingRight
            exportConfig.footerPaddingTop = shareConfig.footerPaddingTop
            exportConfig.showHeaderLine = shareConfig.showHeaderLine
            exportConfig.showFooterLine = shareConfig.showFooterLine
            exportConfig.tipHeaderLeft = shareConfig.tipHeaderLeft
            exportConfig.tipHeaderMiddle = shareConfig.tipHeaderMiddle
            exportConfig.tipHeaderRight = shareConfig.tipHeaderRight
            exportConfig.tipFooterLeft = shareConfig.tipFooterLeft
            exportConfig.tipFooterMiddle = shareConfig.tipFooterMiddle
            exportConfig.tipFooterRight = shareConfig.tipFooterRight
            exportConfig.tipColor = shareConfig.tipColor
            exportConfig.headerMode = shareConfig.headerMode
            exportConfig.footerMode = shareConfig.footerMode
        }
        return exportConfig
    }

    @Keep
    data class Config(
        var name: String = "",
        var bgStr: String = "#EEEEEE",//白天背景
        var bgStrNight: String = "#000000",//夜间背景
        var bgStrEInk: String = "#FFFFFF",//EInk背景
        var bgAlpha: Int = 100,//背景透明�?
        var bgType: Int = 0,//白天背景类型 0:颜色, 1:assets图片, 2其它图片
        var bgTypeNight: Int = 0,//夜间背景类型
        var bgTypeEInk: Int = 0,//EInk背景类型
        private var darkStatusIcon: Boolean = true,//白天是否暗色状态栏
        private var darkStatusIconNight: Boolean = false,//晚上是否暗色状态栏
        private var darkStatusIconEInk: Boolean = true,
        private var textColor: String = "#3E3D3B",//白天文字颜色
        private var textColorNight: String = "#ADADAD",//夜间文字颜色
        private var textColorEInk: String = "#000000",
        private var pageAnim: Int = 0,//翻页动画
        private var pageAnimEInk: Int = 4,
        var textFont: String = "",//字体
        var textBold: Int = 0,//是否粗体�?0:正常, 1:粗体, 2:细体
        var textSize: Int = 20,//文字大小
        var letterSpacing: Float = 0.1f,//字间�?
        var lineSpacingExtra: Int = 12,//行间�?
        var paragraphSpacing: Int = 2,//段距
        var titleMode: Int = 0,//标题位置 0:居左 1:居中 2:隐藏
        var titleSize: Int = 0,
        var titleTopSpacing: Int = 0,
        var titleBottomSpacing: Int = 0,
        var paragraphIndent: String = "　　",//段落缩进
        var underline: Boolean = false, //下划�?
        var paddingBottom: Int = 6,
        var paddingLeft: Int = 16,
        var paddingRight: Int = 16,
        var paddingTop: Int = 6,
        var headerPaddingBottom: Int = 0,
        var headerPaddingLeft: Int = 16,
        var headerPaddingRight: Int = 16,
        var headerPaddingTop: Int = 0,
        var footerPaddingBottom: Int = 6,
        var footerPaddingLeft: Int = 16,
        var footerPaddingRight: Int = 16,
        var footerPaddingTop: Int = 6,
        var showHeaderLine: Boolean = false,
        var showFooterLine: Boolean = true,
        var tipHeaderLeft: Int = ReadTipConfig.time,
        var tipHeaderMiddle: Int = ReadTipConfig.none,
        var tipHeaderRight: Int = ReadTipConfig.battery,
        var tipFooterLeft: Int = ReadTipConfig.chapterTitle,
        var tipFooterMiddle: Int = ReadTipConfig.none,
        var tipFooterRight: Int = ReadTipConfig.pageAndTotal,
        var tipColor: Int = 0,
        var tipDividerColor: Int = -1,
        var headerMode: Int = 0,
        var footerMode: Int = 0
    ) {

        @Transient
        private var textColorIntEInk = -1

        @Transient
        private var textColorIntNight = -1

        @Transient
        private var textColorInt = -1

        @Transient
        private var initColorInt = false

        private fun initColorInt() {
            textColorIntEInk = textColorEInk.toColorInt()
            textColorIntNight = textColorNight.toColorInt()
            textColorInt = textColor.toColorInt()
            initColorInt = true
        }

        fun setCurTextColor(color: Int) {
            when {
                else -> {
                    textColor = "#${color.hexString}"
                    textColorInt = color
                }
            }
        }

        fun curTextColor(): Int {
            if (!initColorInt) {
                initColorInt()
            }
            return when {
                else -> textColorInt
            }
        }

        fun setCurStatusIconDark(isDark: Boolean) {
            when {
                else -> darkStatusIcon = isDark
            }
        }

        fun curStatusIconDark(): Boolean {
            return when {
                else -> darkStatusIcon
            }
        }

        fun setCurPageAnim(@PageAnim.Anim anim: Int) {
            when {
                else -> pageAnim = anim
            }
        }

        fun curPageAnim(): Int {
            return when {
                else -> pageAnim
            }
        }

        fun setCurBg(bgType: Int, bg: String) {
            when {
                else -> {
                    this.bgType = bgType
                    bgStr = bg
                }
            }
        }

        fun curBgStr(): String {
            return when {
                else -> bgStr
            }
        }

        fun curBgType(): Int {
            return when {
                else -> bgType
            }
        }

        fun curBgDrawable(width: Int, height: Int): Drawable {
            if (width == 0 || height == 0) {
                return appCtx.getCompatColor(R.color.background).toDrawable()
            }
            var bgDrawable: Drawable? = null
            val resources = appCtx.resources
            try {
                bgDrawable = when (curBgType()) {
                    0 -> curBgStr().toColorInt().toDrawable()
                    1 -> {
                        val path = "bg" + File.separator + curBgStr()
                        val bitmap = BitmapTools.decodeAssetsBitmap(path, width, height)
                        bitmap?.resizeAndRecycle(width, height)?.toDrawable(resources)
                    }

                    else -> {
                        val path = curBgStr().let {
                            if (it.contains(File.separator)) it
                            else FileTools.getPath(
                                appCtx.getBasePath(PathType.EXTERNAL_FILES),
                                "bg",
                                curBgStr()
                            )
                        }
                        val bitmap = BitmapTools.decodeBitmap(path, width, height)
                        bitmap?.resizeAndRecycle(width, height)?.toDrawable(resources)
                    }
                }
            } catch (e: OutOfMemoryError) {
                e.printOnDebug()
            } catch (e: Exception) {
                e.printOnDebug()
            }
            return bgDrawable ?: appCtx.getCompatColor(R.color.background).toDrawable()
        }
    }
}