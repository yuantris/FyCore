package io.core.common.helper

import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

class ReflectHelper(private val clazz: Class<*>) {

    companion object {
        // 快速入口
        @JvmStatic
        @JvmOverloads
        fun on(className: String, classLoader: ClassLoader? = null): ReflectHelper {
            val loader = classLoader ?: Thread.currentThread().contextClassLoader
            return try {
                ReflectHelper(Class.forName(className, true, loader))
            } catch (e: ClassNotFoundException) {
                throw ReflectionException("Class $className not found", e)
            }
        }

        @JvmStatic
        fun with(clazz: Class<*>): ReflectHelper = ReflectHelper(clazz)

        @JvmStatic
        fun with(kClass: KClass<*>): ReflectHelper = ReflectHelper(kClass.java)

        @JvmStatic
        fun with(any: Any): ReflectHelper {
            return ReflectHelper(any.javaClass).apply {
                this.instance = any
            }
        }

    }

    // 实例对象（用于实例方法调用）
    private var instance: Any? = null
    // 使用线程安全的缓存
    private val methodCache = ConcurrentHashMap<String, Method>()
    private val fieldCache = ConcurrentHashMap<String, Field>()

    // 创建实例
    fun newInstance(vararg args: Any?): ReflectHelper {
        try {
            val parameterTypes = args.map { arg ->
                if (arg == null) Any::class.java else getEffectiveClass(arg)
            }.toTypedArray()
            val constructor = findConstructor(clazz, parameterTypes)
            this.instance = constructor.newInstance(*args)
            return this
        } catch (e: Exception) {
            throw ReflectionException("Failed to create instance of ${clazz.name}", e)
        }
    }

    // 获取方法（支持精确查找）
    fun getMethod(methodName: String, vararg parameterTypes: Class<*>): Method {
        val cacheKey = "$methodName:${parameterTypes.joinToString(",") { it.name }}"

        return methodCache.computeIfAbsent(cacheKey) {
            try {
                findMethodInHierarchy(clazz, methodName, parameterTypes)
                    ?: throw NoSuchMethodException("Method $methodName not found")
            } catch (e: NoSuchMethodException) {
                throw ReflectionException("Method $methodName not found", e)
            }
        }
    }

    // 调用方法
    fun invokeMethod(method: Method, vararg args: Any?): Any? {
        return try {
            method.invoke(if (Modifier.isStatic(method.modifiers)) null else instance, *args)
        } catch (e: InvocationTargetException) {
            throw ReflectionException("Method invocation failed", e.targetException)
        } catch (e: Exception) {
            throw ReflectionException("Failed to invoke method", e)
        }
    }

    // 链式调用方法（支持实例方法/静态方法）
    fun chainInvoke(methodName: String, vararg args: Any?): ReflectHelper {
        try {
            val parameterTypes = args.map { arg ->
                if (arg == null) Any::class.java else getEffectiveClass(arg)
            }.toTypedArray()
            val method = getMethod(methodName, *parameterTypes)
            val isStatic = Modifier.isStatic(method.modifiers)
            val targetInstance = if (isStatic) null else instance

            if (!isStatic && targetInstance == null) {
                throw ReflectionException("Cannot invoke instance method '$methodName' on null instance")
            }

            val result = method.invoke(targetInstance, *args)
            return if (result == null || result == Unit) this else with(result)
        } catch (e: Exception) {
            throw ReflectionException("Failed to invoke method $methodName", e)
        }
    }

    // 获取字段值
    fun getField(fieldName: String): Any? {
        val field = findFieldInHierarchy(fieldName)
        return try {
            if (Modifier.isStatic(field.modifiers)) field.get(null) else field.get(instance)
        } catch (e: Exception) {
            throw ReflectionException("Failed to get field $fieldName", e)
        }
    }

    // 设置字段值
    fun setField(fieldName: String, value: Any?): ReflectHelper {
        val field = findFieldInHierarchy(fieldName)
        try {
            if (Modifier.isStatic(field.modifiers)) {
                field.set(null, value)
            } else {
                field.set(instance, value)
            }
            return this
        } catch (e: Exception) {
            throw ReflectionException("Failed to set field $fieldName", e)
        }
    }
    // 获取结果对象
    @Suppress("UNCHECKED_CAST")
    fun <T> get(): T = (instance ?: clazz) as T

    private fun findFieldInHierarchy(fieldName: String): Field {
        return fieldCache.getOrPut(fieldName) {
            var currentClass: Class<*>? = clazz
            while (currentClass != null) {
                try {
                    return currentClass.getDeclaredField(fieldName).apply { isAccessible = true }
                } catch (e: NoSuchFieldException) {
                    currentClass = currentClass.superclass
                }
            }
            throw ReflectionException("Field $fieldName not found in class hierarchy")
        }
    }

    private fun findMethodInHierarchy(clazz: Class<*>, methodName: String, parameterTypes: Array<out Class<*>>): Method? {
        var currentClass: Class<*>? = clazz
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredMethod(methodName, *parameterTypes).apply { isAccessible = true }
            } catch (e: NoSuchMethodException) {
                currentClass.declaredMethods.firstOrNull { method ->
                    method.name == methodName && parametersCompatible(method.parameterTypes, parameterTypes)
                }?.let { return it.apply { isAccessible = true } }
                currentClass = currentClass.superclass
            }
        }
        return null
    }

    // region 私有辅助方法
    private fun findConstructor(clazz: Class<*>, parameterTypes: Array<Class<*>>): Constructor<*> {
        return clazz.declaredConstructors.firstOrNull { constructor ->
            parametersMatch(constructor.parameterTypes, parameterTypes)
        }?.apply { isAccessible = true }
            ?: throw ReflectionException("No matching constructor found for ${clazz.name}")
    }

    private fun parametersMatch(methodParams: Array<Class<*>>, inputParams: Array<Class<*>>): Boolean {
        if (methodParams.size != inputParams.size) return false
        return methodParams.indices.all { i ->
            methodParams[i].isAssignableFrom(inputParams[i]) ||
                    isPrimitiveWrapperMatch(methodParams[i], inputParams[i])
        }
    }

    // 检查参数兼容性
    private fun parametersCompatible(methodParams: Array<Class<*>>, inputParams: Array<out Class<*>>): Boolean {
        if (methodParams.size != inputParams.size) return false
        return methodParams.indices.all { i ->
            methodParams[i].isAssignableFrom(inputParams[i]) ||
                    isPrimitiveWrapperMatch(methodParams[i], inputParams[i]) ||
                    (methodParams[i] == Object::class.java && !inputParams[i].isPrimitive)
        }
    }

    private fun isPrimitiveWrapperMatch(class1: Class<*>, class2: Class<*>): Boolean {
        return (class1.isPrimitive && getWrapperClass(class1) == class2) ||
                (class2.isPrimitive && getWrapperClass(class2) == class1)
    }

    private fun getEffectiveClass(obj: Any?): Class<*> = when (obj) {
        null -> Any::class.java
        is Int -> Int::class.javaPrimitiveType!!
        is Long -> Long::class.javaPrimitiveType!!
        is Boolean -> Boolean::class.javaPrimitiveType!!
        is Byte -> Byte::class.javaPrimitiveType!!
        is Char -> Char::class.javaPrimitiveType!!
        is Short -> Short::class.javaPrimitiveType!!
        is Float -> Float::class.javaPrimitiveType!!
        is Double -> Double::class.javaPrimitiveType!!
        else -> obj.javaClass
    }


    private fun getWrapperClass(primitiveClass: Class<*>): Class<*> = when (primitiveClass) {
        Int::class.javaPrimitiveType -> Int::class.java
        Long::class.javaPrimitiveType -> Long::class.java
        Boolean::class.javaPrimitiveType -> Boolean::class.java
        Byte::class.javaPrimitiveType -> Byte::class.java
        Char::class.javaPrimitiveType -> Char::class.java
        Short::class.javaPrimitiveType -> Short::class.java
        Float::class.javaPrimitiveType -> Float::class.java
        Double::class.javaPrimitiveType -> Double::class.java
        else -> primitiveClass
    }

    internal class ReflectionException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
}