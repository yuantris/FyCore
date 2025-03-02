package io.core.common.helper

import java.util.function.Consumer

import java.util.*
import java.util.function.Function

/**
 * 为 Java 设计的 Try-Catch 封装工具类
 *
 * 特点：
 * 1. 支持带返回值和不带返回值的操作
 * 2. 支持自定义默认值、异常转换、错误回调
 * 3. 兼容 Java 1.8+ 的 Lambda 语法
 * 4. 支持 checked/unchecked 异常统一处理
 */
object TryCatchTurbo {

    /* ========================== 带返回值的操作 ========================== */

    /**
     * 执行带返回值的操作，返回 Optional 包装
     * @param supplier 可能抛出异常的操作
     * @return Optional 包装的结果，异常时返回 Optional.empty()
     */
    @JvmStatic
    fun <T> safeGet(supplier: SupplierWithException<T>) =
        try {
            Optional.ofNullable(supplier.get())
        } catch (e: Exception) {
            Optional.empty()
        }

    /**
     * 执行带返回值的操作，提供默认值
     * @param supplier 可能抛出异常的操作
     * @param defaultValue 异常时返回的默认值
     */
    @JvmStatic
    fun <T> safeGet(supplier: SupplierWithException<T>, defaultValue: T) =
        try {
            supplier.get()
        } catch (e: Exception) {
            defaultValue
        }

    /**
     * 执行带返回值的操作，支持异常转换
     * @param supplier 可能抛出异常的操作
     * @param exceptionWrapper 异常转换函数
     */
    @JvmStatic
    fun <T> safeGetOrThrow(
        supplier: SupplierWithException<T>,
        exceptionWrapper: Function<Exception, RuntimeException>
    ) = try {
        supplier.get()
    } catch (e: Exception) {
        throw exceptionWrapper.apply(e)
    }

    /* ========================== 不带返回值的操作 ========================== */

    /**
     * 执行不带返回值的操作
     * @param runnable 可能抛出异常的操作
     */
    @JvmStatic
    fun safeRun(runnable: RunnableWithException) {
        try {
            runnable.run()
        } catch (e: Exception) {
            // 默认忽略异常
        }
    }

    /**
     * 执行不带返回值的操作，带异常回调
     * @param runnable 可能抛出异常的操作
     * @param onError 异常处理回调
     */
    @JvmStatic
    fun safeRun(
        runnable: RunnableWithException,
        onError: Consumer<Exception>
    ) = try {
        runnable.run()
    } catch (e: Exception) {
        onError.accept(e)
    }

    /**
     * 带资源清理的 Try-Catch
     */
    @JvmStatic
    fun <T : AutoCloseable, R> useResource(
        resource: T,
        action: Function<T, R>
    ) = try {
        action.apply(resource)
    } catch (e: Exception) {
        throw e
    } finally {
        try {
            resource.close()
        } catch (closeEx: Exception) {
            // 记录关闭异常
        }
    }
}

/* ========================== 函数式接口定义 ========================== */

/**
 * 带异常的 Runnable
 */
fun interface RunnableWithException {
    @Throws(Exception::class)
    fun run()
}

/**
 * 带异常的 Supplier
 */
fun interface SupplierWithException<T> {
    @Throws(Exception::class)
    fun get(): T
}