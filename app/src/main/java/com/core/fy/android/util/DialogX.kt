package com.core.fy.android.util

import android.app.Application
import android.view.View
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.dialogs.MessageDialog
import com.kongzue.dialogx.dialogs.PopNotification
import com.kongzue.dialogx.style.KongzueStyle
import io.core.common.helper.coroutine.info.GlobalScopeManager

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
}

fun Application.initDialogX() {
    GlobalScopeManager.launch {
        DialogX.init(this@initDialogX)
        DialogX.globalStyle = KongzueStyle.style()
    }
}

fun showDxNotification(block: NotificationConfig.() -> Unit): PopNotification {
    val config = NotificationConfig().apply(block)
    return PopNotification.show(config.icon,config.title, config.content)
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