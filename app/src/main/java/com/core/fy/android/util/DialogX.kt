package com.core.fy.android.util

import android.app.Application
import android.content.Context
import android.view.View
import com.kongzue.dialogx.DialogX
import com.kongzue.dialogx.dialogs.MessageDialog
import com.kongzue.dialogx.dialogs.PopNotification
import com.kongzue.dialogx.style.KongzueStyle
import io.core.common.util.extensions.ui.appName


fun Application.initDialogX() {
    DialogX.init(this)
    DialogX.globalStyle = KongzueStyle.style()
}

fun Context.showDxNotification(message: String): PopNotification {
    return PopNotification.show("${appName}通知", message)
}

fun Context.showDxMessage(
    message: String,
    title: String = "温馨提示",
    onCancel: () -> Unit = {},
    onOk: (dialog: MessageDialog, v: View) -> Unit = { _, _ -> }
): MessageDialog {
    return MessageDialog.show(title, message)
        .setOkButton("确定") { dialog, v ->
            onOk.invoke(dialog, v)
            false
        }
        .setCancelButton("取消") { _, _ ->
            onCancel.invoke()
            false
        }
}