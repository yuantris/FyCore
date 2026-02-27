@file:Suppress("UNCHECKED_CAST")

package io.core.ui.delegate

import android.app.Activity
import android.os.Bundle
import kotlin.reflect.KProperty

/**
 * 创建Intent Extra委托
 * @param key Intent Extra Key
 * @param default 默认�?
 * @return Intent Extra委托
 */
class IntentExtra<T>(private val key: String, private val default: T? = null) {
    operator fun getValue(thisRef: Activity, property: KProperty<*>): T? {
        return when (default) {
            is String -> thisRef.intent.getStringExtra(key) as? T ?: default
            is Int -> thisRef.intent.getIntExtra(key, default as Int) as? T ?: default
            is Boolean -> thisRef.intent.getBooleanExtra(key, default as Boolean) as? T ?: default
            is Long -> thisRef.intent.getLongExtra(key, default as Long) as? T ?: default
            is Float -> thisRef.intent.getFloatExtra(key, default as Float) as? T ?: default
            is Double -> thisRef.intent.getDoubleExtra(key, default as Double) as? T ?: default
            is Bundle -> thisRef.intent.getBundleExtra(key) as? T ?: default
            is ArrayList<*> -> {
                when {
                    default.isNotEmpty() -> {
                        when (default.firstOrNull()) {
                            is String -> thisRef.intent.getStringArrayListExtra(key) as? T ?: default as T
                            is Int -> thisRef.intent.getIntegerArrayListExtra(key) as? T ?: default as T
                            else -> thisRef.intent.getSerializableExtra(key) as? T ?: default as T
                        }
                    }
                    else -> thisRef.intent.getSerializableExtra(key) as? T ?: default as T
                }
            }
            else -> thisRef.intent.getSerializableExtra(key) as? T ?: default
        }
    }
}
