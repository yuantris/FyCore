package io.core.common.util.ext.cool

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2024/12/26 10:31
 * @description
 * @author Yuan
 */

/**
 * Activity 中获取 ViewModel 的扩展属性
 * @param factory 可选的 ViewModelFactory
 */
inline fun <reified VM : ViewModel> ComponentActivity.viewModel(factory: ViewModelProvider.Factory? = null): VM {
    return if (factory == null) {
        ViewModelProvider(this)[VM::class.java]
    } else {
        ViewModelProvider(this, factory)[VM::class.java]
    }
}

/**
 * Fragment 中获取 ViewModel 的扩展属性
 * @param factory 可选的 ViewModelFactory
 */
inline fun <reified VM : ViewModel> Fragment.viewModel(factory: ViewModelProvider.Factory? = null): VM {
    return if (factory == null) {
        ViewModelProvider(this)[VM::class.java]
    } else {
        ViewModelProvider(this, factory)[VM::class.java]
    }
}

/**
 * Fragment 中获取 Activity 作用域 ViewModel 的扩展属性
 * @param factory 可选的 ViewModelFactory
 */
inline fun <reified VM : ViewModel> Fragment.activityViewModel(factory: ViewModelProvider.Factory? = null): VM {
    return if (factory == null) {
        ViewModelProvider(requireActivity())[VM::class.java]
    } else {
        ViewModelProvider(requireActivity(), factory)[VM::class.java]
    }
}
