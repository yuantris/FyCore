package io.core.utils.extensions.cool

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import io.core.utils.extensions.ui.ctx
import io.core.utils.tools.OSAir.higherThan
import io.core.utils.tools.PermissionAir
import io.core.utils.tools.isAndroid11Plus
import io.core.utils.constant.ANDROID_10
import io.core.utils.constant.ANDROID_13
import io.core.utils.constant.ANDROID_6

/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/2/6 9:51
 * @description
 * @author Yuan
 */

fun Context.hasReadStoragePermission(): Boolean {
    // 1. 先检查是否拥有管理所有文件的权限
    if (hasManageExternalStorage()) return true

    // 2. 分版本检查读权限
    return when {
        higherThan(ANDROID_13) -> {
            // Android 13+ 需要检查媒体权�?
            hasPermission(Manifest.permission.READ_MEDIA_VIDEO) ||
                    hasPermission(Manifest.permission.READ_MEDIA_IMAGES) ||
                    hasPermission(Manifest.permission.READ_MEDIA_AUDIO)
        }

        higherThan(ANDROID_6) -> {
            // Android 6.0~12
            hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        else -> {
            // Android 5.0~5.1 检查清单声�?
            checkManifestPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}

fun Context.hasWriteStoragePermission(): Boolean {
    // 1. 先检查是否拥有管理所有文件的权限
    if (hasManageExternalStorage()) return true

    // 2. 分版本检查写权限
    return when {
        higherThan(ANDROID_10) -> {
            // Android 10+ 使用Scoped Storage，默认允许应用私有目录写�?
            // true
            // 优先判断是否有外置存储的权限
            hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        higherThan(ANDROID_6) -> {
            hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        else -> {
            checkManifestPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}

fun Context.hasReadWriteStoragePermission(): Boolean {
    return hasReadStoragePermission() && hasWriteStoragePermission()
}

fun hasManageExternalStorage(): Boolean {
    return if (isAndroid11Plus) {
        Environment.isExternalStorageManager()
    } else {
        false
    }
}

// 通用运行时权限检�?
private fun Context.hasPermission(permission: String): Boolean {
    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

// 检查清单是否声明权限（仅用于API <23�?
private fun Context.checkManifestPermission(permission: String): Boolean {
    return try {
        val info = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
        info.requestedPermissions?.any { it == permission } ?: false
    } catch (e: Exception) {
        false
    }
}

