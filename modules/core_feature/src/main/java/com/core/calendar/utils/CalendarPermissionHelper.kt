package com.core.calendar.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * 日历权限管理工具类
 */
object CalendarPermissionHelper {
    
    const val REQUEST_CODE_CALENDAR_PERMISSIONS = 1001
    
    private val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.READ_CALENDAR,
        Manifest.permission.WRITE_CALENDAR
    )
    
    /**
     * 检查是否有读取日历权限
     */
    @JvmStatic
    fun hasReadPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    
    /**
     * 检查是否有写入日历权限
     */
    @JvmStatic
    fun hasWritePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.WRITE_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    
    /**
     * 检查是否有所有必需权限
     */
    @JvmStatic
    fun hasAllPermissions(context: Context): Boolean =
        REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    
    /**
     * 获取缺失的权限列表
     */
    @JvmStatic
    fun getMissingPermissions(context: Context): Array<String> =
        REQUIRED_PERMISSIONS.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    
    /**
     * 请求日历权限
     */
    @JvmStatic
    @JvmOverloads
    fun requestPermissions(
        activity: Activity, 
        requestCode: Int = REQUEST_CODE_CALENDAR_PERMISSIONS
    ) {
        val missingPermissions = getMissingPermissions(activity)
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, missingPermissions, requestCode)
        }
    }
    
    /**
     * 检查权限请求结果
     */
    @JvmStatic
    fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        if (requestCode != REQUEST_CODE_CALENDAR_PERMISSIONS) {
            return false
        }
        
        return grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }
    
    /**
     * 是否应该显示权限说明
     */
    @JvmStatic
    fun shouldShowRequestPermissionRationale(activity: Activity): Boolean =
        REQUIRED_PERMISSIONS.any { permission ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
        }
    
    /**
     * 获取所有必需权限
     */
    @JvmStatic
    fun getRequiredPermissions(): Array<String> = REQUIRED_PERMISSIONS.clone()
}