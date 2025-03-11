package com.core.fy.android.util

import android.content.Context
import com.core.fy.android.R
import com.lxj.xpopup.XPopup
import com.lxj.xpopup.impl.LoadingPopupView
import com.lxj.xpopup.interfaces.OnCancelListener
import com.lxj.xpopup.interfaces.OnConfirmListener
import com.lxj.xpopup.interfaces.OnInputConfirmListener

// DSL 配置类
class XPopupConfig {
    var title: String = "温馨提示"
    var content: String = ""
    var confirmText: String = "确定"
    var cancelText: String = "取消"
    var inputContent: String = ""
    var hint: String = ""
    var onConfirm: (() -> Unit)? = null
    var onInputConfirm: ((String) -> Unit)? = null
    var onCancel: (() -> Unit)? = null
    var isHideCancel: Boolean = false
    var bindLayoutId: Int = 0
}

fun Context.showXpConfirm(block: XPopupConfig.() -> Unit) {
    val config = XPopupConfig().apply(block)
    XPopup.Builder(this)
        .isViewMode(true)
        .isDestroyOnDismiss(true)
        .asConfirm(
            config.title, config.content, config.cancelText, config.confirmText,
            { config.onConfirm?.invoke() },
            { config.onCancel?.invoke() }, config.isHideCancel, config.bindLayoutId
        ).show()
}

fun Context.showXpInputConfirm(block: XPopupConfig.() -> Unit) {
    val config = XPopupConfig().apply(block)
    XPopup.Builder(this)
        .asInputConfirm(
            config.title,
            config.content,
            config.inputContent,
            config.hint,
            { text -> config.onInputConfirm?.invoke(text.orEmpty()) },
            { config.onCancel?.invoke() },
            config.bindLayoutId
        ).show()
}

fun Context.showXpLoading(block: XPopupConfig.() -> Unit) {
    val config = XPopupConfig().apply(block)
    XPopup.Builder(this)
        .isViewMode(true)
        .isDestroyOnDismiss(true)
        .hasShadowBg(false)
        .asLoading(config.content, LoadingPopupView.Style.ProgressBar)
        .show()
}