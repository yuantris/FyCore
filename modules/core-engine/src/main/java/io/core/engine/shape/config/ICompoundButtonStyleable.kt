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
 * 2025/1/21 10:49
 * @description
 * @author Yuan
 */
interface ICompoundButtonStyleable {

    fun getButtonDrawableStyleable(): Int

    fun getButtonPressedDrawableStyleable(): Int

    fun getButtonCheckedDrawableStyleable(): Int

    fun getButtonDisabledDrawableStyleable(): Int

    fun getButtonFocusedDrawableStyleable(): Int

    fun getButtonSelectedDrawableStyleable(): Int
}