package io.core.common.util.tools

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.core.appCtx

object PermissionAir {

    /**
     * 检查单个权限是否已授予
     * @param permission 要检查的权限
     * @return true 如果权限已授予，false 否则
     */
    fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            appCtx,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查多个权限是否全部已授予
     * @param permissions 要检查的权限数组
     * @return true 如果所有权限都已授予，false 否则
     */
    fun areAllGranted(vararg permissions: String): Boolean {
        return permissions.all { permission ->
            isGranted(permission)
        }
    }

    /**
     * 请求权限
     * @param activity 发起请求的Activity
     * @param permissions 要请求的权限数组
     * @param requestCode 请求码
     */
    fun requestPermissions(activity: Activity, permissions: Array<String>, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    /**
     * 检查权限请求结果
     * @param grantResults 权限请求结果数组
     * @return true 如果所有请求的权限都被授予，false 否则
     */
    fun verifyPermissions(grantResults: IntArray): Boolean {
        return grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }

    /**
     * 检查是否应该显示权限请求的说明
     * @param activity Activity
     * @param permission 权限
     * @return true 如果需要显示说明，false 否则
     */
    fun shouldShowRequestPermissionRationale(activity: Activity, permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
}