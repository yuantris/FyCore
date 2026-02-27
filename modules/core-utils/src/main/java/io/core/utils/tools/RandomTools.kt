package io.core.utils.tools

import java.security.SecureRandom
import kotlin.random.Random

/**
 * ██�? ██╗███████╗██╗   ██�?   ┌──────────�?
 * ╚██╗██╔╝██╔════╝╚██╗ ██╔╝    �?加载进度 │▰▰▰▰▰▰▰▰◯ 87%
 *  ╚███╔╝ █████╗   ╚████╔�?    └──────────�?
 *  ██╔██╗ ██╔══╝    ╚██╔�?     ╱╲▲△△△△△△△�?
 * ██╔╝ ██╗██╗        ██�?      �?▏正在渲染配置矩�?..
 * ╚═�? ╚═╝╚═╝        ╚═�?      ╲╱▼▽▽▽▽▽▽▽�?
 * 注释的艺术，正在生成......
 * 最后编译阶�?████████░░░░ 65% (�?F12 解锁彩蛋)
 * ---------------------------------------------
 *
 * @Author [Yuan]
 * 2025/5/16 11:24
 */

object RandomTools {
    private val secureRandom = SecureRandom()

    /**
     * 生成指定长度的随机字节数�?
     * @param size 要生成的字节数组长度
     * @return 随机字节数组
     */
    @JvmStatic
    fun nextBytes(size: Int): ByteArray {
        return ByteArray(size).apply {
            secureRandom.nextBytes(this)
        }
    }

    /**
     * 生成指定范围内的随机整数
     * @param from 最小�?包含)
     * @param until 最大�?包含)
     * @param exclude 可选参数，如果随机值等于此值则重新生成
     * @throws IllegalArgumentException 当exclude值在范围外时抛出
     */
    @JvmStatic
    @JvmOverloads
    @Throws(IllegalArgumentException::class)
    fun nextInt(from: Int, until: Int, exclude: Int? = null): Int {
        require(until >= from) { "until must be greater than or equal to from" }
        exclude?.let {
            require(it in from..until) {
                "exclude value must be within range [$from, $until]"
            }
        }

        while (true) {
            val randomValue = Random.nextInt(from, until + 1)
            if (randomValue != exclude) return randomValue
        }
    }

    /**
     * 生成随机布尔�?
     * @return 随机true或false
     */
    @JvmStatic
    fun nextBoolean(): Boolean = Random.nextBoolean()

    /**
     * 生成随机字符�?
     * @param length 字符串长�?
     * @param chars 可选字符集(默认包含字母和数�?
     * @return 随机字符�?
     */
    @JvmStatic
    fun nextString(
        length: Int,
        chars: CharArray = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray()
    ): String {
        return buildString(length) {
            repeat(length) {
                append(chars[nextInt(0, chars.size)])
            }
        }
    }

    /**
     * 生成随机UUID字符�?
     * @return 标准UUID格式字符�?
     */
    @JvmStatic
    fun nextUUID(): String = java.util.UUID.randomUUID().toString()
}