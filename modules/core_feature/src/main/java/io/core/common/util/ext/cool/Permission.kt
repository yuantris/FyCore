package io.core.common.util.ext.cool

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.XXPermissions
import io.core.common.util.ext.ui.ctx

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/6 9:51
 * @description
 * @author Yuan
 */

fun Activity.isGranted(vararg permissions: String): Boolean {
    return XXPermissions.isGranted(this, *permissions)
}

fun Fragment.isGranted(vararg permissions: String): Boolean {
    return XXPermissions.isGranted(requireContext(), *permissions)
}

fun Context.isGranted(vararg permissions: String): Boolean {
    return XXPermissions.isGranted(this, *permissions)
}

fun Context.requestPermission(
    vararg permissions: String,
    onDenied: (MutableList<String>, Boolean) -> Unit = { _, _ ->/* 默认空实现 */ },
    onGranted: (Boolean) -> Unit
) {
    this.ctx?.let {
        XXPermissions.with(it)
            .permission(*permissions)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    onGranted.invoke(allGranted)
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    onDenied.invoke(permissions, doNotAskAgain)
                }
            })
    }

}

