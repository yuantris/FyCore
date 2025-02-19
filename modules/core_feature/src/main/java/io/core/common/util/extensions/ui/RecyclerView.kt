package io.core.common.util.extensions.ui

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.view.View
import android.widget.EdgeEffect
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView

@SuppressLint("NotifyDataSetChanged")
fun RecyclerView.Adapter<*>.notifyAllDataChanged() = this.notifyDataSetChanged()

fun RecyclerView.disableEdgeEffect() {
    // 方法一：禁用默认边缘效果
    overScrollMode = View.OVER_SCROLL_NEVER

    // 方法二：自定义 EdgeEffectFactory 彻底禁用（推荐）
    edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
        override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
            return object : EdgeEffect(view.context) {
                // 禁用绘制
                override fun draw(canvas: Canvas?): Boolean = false

                // 禁用拉动效果
                override fun onPull(deltaDistance: Float) {}
                override fun onPull(deltaDistance: Float, displacement: Float) {}
                override fun onRelease() {}
                override fun onAbsorb(velocity: Int) {}
            }
        }
    }
}

fun RecyclerView.setEdgeEffectColor(@ColorInt color: Int) {
    edgeEffectFactory = object : RecyclerView.EdgeEffectFactory() {
        override fun createEdgeEffect(view: RecyclerView, direction: Int): EdgeEffect {
            val edgeEffect = super.createEdgeEffect(view, direction)
            edgeEffect.color = color
            return edgeEffect
        }
    }
}