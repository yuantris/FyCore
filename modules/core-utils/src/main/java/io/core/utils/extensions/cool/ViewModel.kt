package io.core.utils.extensions.cool

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider


/**
 * Activity 中获�?ViewModel 的扩展属�?
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
 * Fragment 中获�?ViewModel 的扩展属�?
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
 * Fragment 中获�?Activity 作用�?ViewModel 的扩展属�?
 * @param factory 可选的 ViewModelFactory
 */
inline fun <reified VM : ViewModel> Fragment.activityViewModel(factory: ViewModelProvider.Factory? = null): VM {
    return if (factory == null) {
        ViewModelProvider(requireActivity())[VM::class.java]
    } else {
        ViewModelProvider(requireActivity(), factory)[VM::class.java]
    }
}
