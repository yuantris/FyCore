package io.core.common.base.component.dialog

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.viewbinding.ViewBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.core.ui.R
import io.core.common.util.extensions.cool.dpToPx
import io.core.common.util.extensions.ui.inflateWithGeneric

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/17 10:20
 * @description
 * @author Yuan
 */
abstract class BaseBottomSheetDialog<VB : ViewBinding> : BottomSheetDialogFragment() {

    lateinit var binding: VB
    private var dialogRootLayout: CoordinatorLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.BottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // 初始化 CoordinatorLayout 并作为根视图
        dialogRootLayout = CoordinatorLayout(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        // 绑定布局并将其添加到 CoordinatorLayout
        binding = inflateWithGeneric(this, inflater)
        dialogRootLayout?.addView(
            binding.root,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return dialogRootLayout
    }

    override fun onStart() {
        super.onStart()
        // 设置背景为透明
        dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // 初始化配置和视图
        dialogRootLayout?.let { initConfig(Builder(it)) }
        initView()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 清理资源，避免内存泄漏
        dialogRootLayout = null
    }

    /**
     * 子类可以重写此方法进行额外的配置
     */
    protected open fun initConfig(builder: Builder) {}

    /**
     * 子类可以重写此方法初始化视图
     */
    protected open fun initView() {}

    /**
     * Builder 类，用于灵活配置 CoordinatorLayout
     */
    class Builder(private val coordinatorLayout: CoordinatorLayout) {

        fun setBackgroundColor(@ColorInt colorRes: Int): Builder = apply {
            coordinatorLayout.setBackgroundColor(colorRes)
        }

        fun setBackground(drawable: Drawable?): Builder = apply {
            coordinatorLayout.background = drawable
        }

        fun setPadding(left: Int, top: Int, right: Int, bottom: Int): Builder = apply {
            coordinatorLayout.setPadding(
                left.dpToPx(),
                top.dpToPx(),
                right.dpToPx(),
                bottom.dpToPx()
            )
        }

        fun root(): CoordinatorLayout = coordinatorLayout
    }
}
