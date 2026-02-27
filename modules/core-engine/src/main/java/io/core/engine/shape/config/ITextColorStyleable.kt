package io.core.engine.shape.config

/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/1/21 10:51
 * @description
 * @author Yuan
 */
interface ITextColorStyleable {
    fun getTextColorStyleable(): Int

    fun getTextPressedColorStyleable(): Int

    fun getTextCheckedColorStyleable(): Int {
        return 0
    }

    fun getTextDisabledColorStyleable(): Int

    fun getTextFocusedColorStyleable(): Int

    fun getTextSelectedColorStyleable(): Int

    fun getTextStartColorStyleable(): Int

    fun getTextCenterColorStyleable(): Int

    fun getTextEndColorStyleable(): Int

    fun getTextGradientOrientationStyleable(): Int

    fun getTextStrokeColorStyleable(): Int

    fun getTextStrokeSizeStyleable(): Int
}