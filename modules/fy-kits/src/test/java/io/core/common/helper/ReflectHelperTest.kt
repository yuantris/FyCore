package io.core.common.helper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executors

class ReflectHelperTest {
    // 测试初始化方法
    @Test
    fun testClassInitialization() {
        // 测试on方法初始化
        val classHelper = ReflectHelper.on("java.lang.String")
        assertEquals(String::class.java, classHelper.get<Class<*>>())

        // 测试with实例
        val instanceHelper = ReflectHelper.with("test")
        assertEquals("test", instanceHelper.get<String>())
    }

    // 测试链式调用
    @Test
    fun testMethodChaining() {
        val list = ArrayList<String>()
        val result = ReflectHelper.with(list)
            .chainInvoke("add", "element")
            .get<Boolean>()

        assertTrue(result)
        assertEquals(1, list.size)
    }

    // 测试字段访问边界
    @Test
    fun testFieldBoundaryCases() {
        // 测试静态字段
        val systemOut = ReflectHelper.on("java.lang.System")
            .getField("out")

        // 测试实例字段
        val list = ArrayList<String>().apply { add("test") }
        val size = ReflectHelper.with(list)
            .getField("size")

        assertEquals(1, size)
    }

    // 测试异常抛出机制
    @Test(expected = ReflectHelper.ReflectionException::class)
    fun testExceptionHandling() {
        ReflectHelper.on("java.lang.String")
            .getMethod("nonExistingMethod")
    }

}