package com.core.fy.android.util

import android.app.Application
import android.view.View
import com.core.fy.android.R
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.dialogs.CustomDialog
import com.kongzue.dialogx.dialogs.MessageDialog
import com.kongzue.dialogx.dialogs.PopNotification
import com.kongzue.dialogx.interfaces.DialogLifecycleCallback
import com.kongzue.dialogx.interfaces.OnBindView
import com.kongzue.dialogx.style.KongzueStyle

// DSL 配置类
class DialogXConfig {
    var title: String = "温馨提示"
    var content: String = ""
    var confirmText: String = "确定"
    var cancelText: String = "取消"
    var inputContent: String = ""
    var hint: String = ""
    var onConfirm: (() -> Unit)? = null
    var onCancel: (() -> Unit)? = null
    var onInputConfirm: ((String) -> Unit)? = null
    var onMessageConfirm: ((MessageDialog, View) -> Unit)? = null
    var onMessageCancel: ((MessageDialog, View) -> Unit)? = null
}

class NotificationConfig {
    var title: String = "温馨提示"
    var content: String = ""
    var icon: Int = 0
    var onDismiss: () -> Unit = {}
    var onShow: () -> Unit = {}
}

class CustomDialogConfig {
    var layoutResId: Int = 0 // 默认布局
    var onBindView: (CustomDialog, View) -> Unit = { _, _ -> } // 绑定视图的回调
}


fun Application.initDialogX() {
    GlobalCoroutine.launch {
        DialogX.init(this@initDialogX)
        DialogX.globalStyle = KongzueStyle.style()
    }
}

fun showDxNotification(block: NotificationConfig.() -> Unit): PopNotification {
    val config = NotificationConfig().apply(block)
    return PopNotification.show(config.icon, config.title, config.content)
        .setDialogLifecycleCallback(object : DialogLifecycleCallback<PopNotification>() {
            override fun onDismiss(dialog: PopNotification?) {
                super.onDismiss(dialog)
                config.onDismiss.invoke()
            }

            override fun onShow(dialog: PopNotification?) {
                super.onShow(dialog)
                config.onShow.invoke()
            }
        })
}

fun showDxMessage(block: DialogXConfig.() -> Unit): MessageDialog {
    val config = DialogXConfig().apply(block)
    return MessageDialog.show(config.title, config.content)
        .setOkButton(config.confirmText) { dialog, v ->
            config.onMessageConfirm?.invoke(dialog, v)
            false
        }
        .setCancelButton(config.cancelText) { dialog, v ->
            config.onMessageCancel?.invoke(dialog, v)
            false
        }
}

fun showDxCustom(block: CustomDialogConfig.() -> Unit): CustomDialog {
    val config = CustomDialogConfig().apply(block)
    return CustomDialog.show(object : OnBindView<CustomDialog>(config.layoutResId) {
        override fun onBind(dialog: CustomDialog, v: View) {
            config.onBindView.invoke(dialog, v)
        }
    })
}