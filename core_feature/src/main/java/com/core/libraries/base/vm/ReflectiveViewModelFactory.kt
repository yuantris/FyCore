package com.core.libraries.base.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.lang.reflect.Constructor

/**
 * 自定义 ViewModel 工厂，使用反射和泛型实例化 ViewModel。
 */
class ReflectiveViewModelFactory<T : ViewModel>(
    private val modelClass: Class<T>
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        try {
            // 确保传入的 modelClass 是预期的类型
            if (this.modelClass.isAssignableFrom(modelClass)) {
                // 获取 ViewModel 的无参构造函数
                val constructor: Constructor<T> = modelClass.getDeclaredConstructor()
                return constructor.newInstance()
            } else {
                throw IllegalArgumentException("Unexpected model class: $modelClass")
            }
        } catch (e: Exception) {
            throw IllegalArgumentException("Cannot create an instance of $modelClass", e)
        }
    }
}

/**
 * 扩展函数，便于通过反射实例化 ViewModel。
 */
inline fun <reified T : ViewModel> reflectiveViewModelFactory(): ViewModelProvider.Factory {
    return ReflectiveViewModelFactory(T::class.java)
}
