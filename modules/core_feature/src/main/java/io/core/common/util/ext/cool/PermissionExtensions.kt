package io.core.common.util.ext.cool

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.Fragment
import com.hjq.permissions.XXPermissions

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

fun AppCompatActivity.isGranted(vararg permissions: String): Boolean {
    return XXPermissions.isGranted(this, *permissions)
}

fun Fragment.isGranted(vararg permissions: String): Boolean {
    return XXPermissions.isGranted(requireContext(), *permissions)
}

fun Context.isGranted(vararg permissions: String): Boolean {
    return XXPermissions.isGranted(this, *permissions)
}

