package io.core.ui.delegate

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * 创建接口的智能代理委�?
 * @param T 目标接口类型
 * @return 实现该接口的动态代理对�?
 */
inline fun <reified T> createSmartDelegate(): T {
    val interfaceClass = T::class.java
    return Proxy.newProxyInstance(
        interfaceClass.classLoader,
        arrayOf(interfaceClass),
        InterfaceDelegateHandler
    ) as T
}

/**
 * 严格模式代理（强制实现所有方法）
 */
inline fun <reified T> createStrictDelegate(): T {
    val handler = InvocationHandler { _, method, _ ->
        throw NotImplementedError("必须实现 ${method.declaringClass.simpleName}.${method.name}")
    }
    return Proxy.newProxyInstance(
        T::class.java.classLoader,
        arrayOf(T::class.java),
        handler
    ) as T
}

/**
 * 接口代理的智能处理程�?
 */
object InterfaceDelegateHandler : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<Any>?): Any? {
        return when (val returnType = method.returnType) {
            Void.TYPE -> Unit // 兼容Java void返回类型
            else -> createTypeSafeDefault(returnType)
        }
    }

    /**
     * 根据类型创建安全默认�?
     */
    private fun createTypeSafeDefault(type: Class<*>): Any? {
        return when {
            type.isPrimitive -> handlePrimitiveType(type)
            type.isEnum -> type.enumConstants.firstOrNull()
            type.isInterface -> createInterfaceProxy(type)
            else -> null
        }
    }

    /**
     * 处理原始数据类型
     */
    private fun handlePrimitiveType(type: Class<*>): Any {
        return when (type.name) {
            "boolean" -> false
            "char" -> '\u0000'
            "byte" -> 0.toByte()
            "short" -> 0.toShort()
            "int" -> 0
            "long" -> 0L
            "float" -> 0.0f
            "double" -> 0.0
            else -> throw IllegalArgumentException("Unsupported primitive type: ${type.name}")
        }
    }

    /**
     * 为接口类型创建递归代理
     */
    private fun createInterfaceProxy(interfaceType: Class<*>): Any {
        return Proxy.newProxyInstance(
            interfaceType.classLoader,
            arrayOf(interfaceType),
            InterfaceDelegateHandler
        )
    }
}
