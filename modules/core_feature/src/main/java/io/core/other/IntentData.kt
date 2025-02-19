package io.core.other

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import io.core.common.util.extensions.currentTimeMillis

object IntentData {

    private val bigData: MutableMap<String, Any> = mutableMapOf()

    @Synchronized
    fun put(key: String, data: Any?): String {
        data?.let {
            bigData[key] = data
        }
        return key
    }

    @Synchronized
    fun put(data: Any?): String {
        val key = currentTimeMillis.toString()
        data?.let {
            bigData[key] = data
        }
        return key
    }

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T> get(key: String?): T? {
        if (key == null) return null
        val data = bigData[key]
        bigData.remove(key)
        return data as? T
    }

    // 基础启动 Activity
    fun startActivity(context: Context, clazz: Class<*>, extras: Bundle? = null) {
        val intent = Intent(context, clazz).apply {
            extras?.let { putExtras(it) }
        }
        context.startActivity(intent)
    }

    // 启动 Activity 带回调（适配新版 Activity Result API）
    fun startActivityForResult(
        activity: Activity,
        clazz: Class<*>,
        requestCode: Int,
        extras: Bundle? = null
    ) {
        val intent = Intent(activity, clazz).apply {
            extras?.let { putExtras(it) }
        }
        activity.startActivityForResult(intent, requestCode)
    }

    @Suppress("DEPRECATION")
    fun goAndFinishAlpha(
        context: Context,
        clazz: Class<*>,
        enterAnim: Int = 0,
        exitAnim: Int = 0
    ) {
        startActivity(context, clazz)
        if (context is Activity) context.finish()
        if (context is Activity) context.overridePendingTransition(enterAnim, exitAnim)
    }
}