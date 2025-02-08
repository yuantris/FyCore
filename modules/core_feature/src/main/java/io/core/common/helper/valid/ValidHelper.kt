package io.core.common.helper.valid

import java.io.File
import java.util.function.Predicate

/**
 * ██████████
 * █▄█████▄█
 * █▼▼▼▼▼
 * █
 * █▲▲▲▲▲
 * ██████████
 * ██ ██
 * 注释的艺术，正在加载……
 * 2024/12/16 17:20
 * @description 通用校验工具类，支持动态添加校验条件
 * @author Yuan
 */
class ValidHelper private constructor() {
    private val TAG = "ValidHelper_"
    private val conditions: MutableList<Predicate<Any>> = mutableListOf()

    /**
     * 添加条件
     */
    fun addCondition(condition: Predicate<Any>): ValidHelper {
        conditions.add(condition)
        return this
    }

    /**
     * 针对 File 类型进行校验
     */
    fun asFile(): ValidHelper {
        addCondition { input ->
            input is File &&
                    input.exists() &&
                    input.isFile &&
                    input.lastModified() != 0L &&
                    input.length() > 0
        }
        return this
    }

    /**
     * 过滤掉路径中包含隐藏文件夹或文件
     */
    fun excludeHiddenFiles(): ValidHelper {
        addCondition { input ->
            if (input is File) {
                !containsHiddenPart(input)
            } else {
                true
            }
        }
        return this
    }

    /**
     * 检查路径中是否包含隐藏文件夹或文件
     */
    private fun containsHiddenPart(file: File): Boolean {
        var currentFile: File? = file
        while (currentFile != null) {
            if (currentFile.name.startsWith(".")) {
                return true
            }
            currentFile = currentFile.parentFile
        }
        return false
    }


    /**
     * 最终执行校验，返回校验结果
     */
    fun build(
        input: Any,
        onResult: ((Boolean) -> Unit)? = null
    ): Boolean {
        val isVerified = conditions.all { it.test(input) }
        onResult?.invoke(isVerified)
        return isVerified
    }

    companion object {
        /**
         * 创建 ValidHelper 实例
         */
        fun create(): ValidHelper {
            return ValidHelper()
        }
    }
}
