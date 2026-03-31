package com.core.fy.android.function.brv.fragment

import android.view.View
import androidx.core.view.ViewCompat
import com.core.fy.android.R
import com.core.fy.android.databinding.FragmentBrvMultitypeBinding
import com.core.fy.android.function.brv.BrvActivity
import com.core.fy.android.function.brv.model.HoverHeaderModel
import com.core.fy.android.function.brv.model.SimpleModel
import com.drake.brv.listener.OnHoverAttachListener
import com.drake.brv.utils.linear
import com.drake.brv.utils.setup
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.util.CoreUtil.Companion.toast

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/17 10:27
 * @description
 * @author Yuan
 */
class HoverLinearFragment:ReflectBindingFragment<FragmentBrvMultitypeBinding,BrvActivity>() {

    override fun initView() {
        super.initView()
        binding.rv.linear().setup {
            addType<SimpleModel>(R.layout.item_simple)
            addType<HoverHeaderModel>(R.layout.item_hover_header)
            models = getData()

            // 点击事件
            onClick(R.id.item) {
                when (itemViewType) {
                    R.layout.item_hover_header -> toast("悬停条目")
                    else -> toast("普通条目")
                }
            }

            // 可选项, 粘性监听器
            onHoverAttachListener = object : OnHoverAttachListener {
                override fun attachHover(v: View) {
                    ViewCompat.setElevation(v, 10F) // 悬停时显示阴影
                }

                override fun detachHover(v: View) {
                    ViewCompat.setElevation(v, 0F) // 非悬停时隐藏阴影
                }
            }

        }
    }

    private fun getData(): List<Any> {
        return listOf(
            HoverHeaderModel(),
            SimpleModel(),
            SimpleModel(),
            SimpleModel(),
            HoverHeaderModel(),
            SimpleModel(),
            SimpleModel(),
            SimpleModel(),
            HoverHeaderModel(),
            SimpleModel(),
            SimpleModel(),
            SimpleModel(),
            SimpleModel(),
            SimpleModel(),
            SimpleModel(),
            SimpleModel(),
            SimpleModel(),
            SimpleModel(),
            SimpleModel(),
            SimpleModel()
        )
    }
}