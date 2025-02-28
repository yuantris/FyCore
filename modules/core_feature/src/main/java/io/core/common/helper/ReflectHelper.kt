package io.core.common.helper

import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

class ReflectHelper(private val clazz: Class<*>) {
    companion object {
        // 快速入口
        fun on(className: String, classLoader: ClassLoader? = null): ReflectHelper {
            val loader = classLoader ?: Thread.currentThread().contextClassLoader
            return ReflectHelper(Class.forName(className, true, loader))
        }

        fun with(clazz: Class<*>): ReflectHelper = ReflectHelper(clazz)
        fun with(any: Any): ReflectHelper = ReflectHelper(any.javaClass)
    }

    // 实例对象（用于实例方法调用）
    private var instance: Any? = null

    // 创建实例
    fun newInstance(vararg args: Any): ReflectHelper {
        val parameterTypes = args.map { it.javaClass }.toTypedArray()
        val constructor = clazz.getDeclaredConstructor(*parameterTypes).apply {
            isAccessible = true
        }
        this.instance = constructor.newInstance(*args)
        return this
    }

    // 获取方法
    fun getMethod(methodName: String, vararg parameterTypes: Class<*>): Method {
        return try {
            clazz.getMethod(methodName, *parameterTypes)
        } catch (e: NoSuchMethodException) {
            clazz.getDeclaredMethod(methodName, *parameterTypes)
        }.apply { isAccessible = true }
    }

    // 调用方法
    fun invokeMethod(method: Method, vararg args: Any): Any? {
        return method.invoke(instance, *args)
    }

    // 链式调用方法（支持实例方法/静态方法）
    fun chainInvoke(methodName: String, vararg args: Any): ReflectHelper {
        val parameterTypes = args.map { it.javaClass }.toTypedArray()
        val method = getMethod(methodName, *parameterTypes)
        // 判断是否为静态方法
        val targetInstance = if (Modifier.isStatic(method.modifiers)) null else instance
        val result = method.invoke(targetInstance, *args)
        return if (result != null && result != Unit) {
            with(result)
        } else {
            this
        }
    }

    // 获取字段值
    fun getField(fieldName: String): Any? {
        val field = findField(fieldName)
        return if (Modifier.isStatic(field.modifiers)) {
            field.get(null) // 静态字段强制用 null 实例
        } else {
            field.get(instance)
        }
    }

    // 设置字段值
    fun setField(fieldName: String, value: Any): ReflectHelper {
        findField(fieldName).set(instance, value)
        return this
    }

    private fun findField(fieldName: String): Field {
        return clazz.getDeclaredField(fieldName).apply {
            isAccessible = true
        }
    }

    // 获取结果对象
    fun <T> get(): T = instance as T
}
