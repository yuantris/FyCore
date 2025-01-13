package com.core.libraries.common.helper

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * ViewModel 快速实例化帮助类
 */
object ViewModelHelper {

    /**
     * 在 Activity 中创建 ViewModel
     * @param factory 可选的自定义 ViewModelFactory
     */
    inline fun <reified VM : ViewModel> ComponentActivity.getViewModel(factory: ViewModelProvider.Factory? = null): VM {
        return if (factory == null) {
            ViewModelProvider(this)[VM::class.java]
        } else {
            ViewModelProvider(this, factory)[VM::class.java]
        }
    }

    /**
     * 在 Fragment 中创建 ViewModel
     * @param factory 可选的自定义 ViewModelFactory
     */
    inline fun <reified VM : ViewModel> Fragment.getViewModel(factory: ViewModelProvider.Factory? = null): VM {
        return if (factory == null) {
            ViewModelProvider(this)[VM::class.java]
        } else {
            ViewModelProvider(this, factory)[VM::class.java]
        }
    }

    /**
     * 在 Fragment 中创建 Activity 作用域的 ViewModel
     * @param factory 可选的自定义 ViewModelFactory
     */
    inline fun <reified VM : ViewModel> Fragment.getActivityViewModel(factory: ViewModelProvider.Factory? = null): VM {
        return if (factory == null) {
            ViewModelProvider(requireActivity())[VM::class.java]
        } else {
            ViewModelProvider(requireActivity(), factory)[VM::class.java]
        }
    }
}
