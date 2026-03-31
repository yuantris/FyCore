package com.core.fy.android.function.brv.fragment

import com.core.fy.android.R
import com.core.fy.android.databinding.FragmentBrvMultitypeBinding
import com.core.fy.android.function.brv.BrvActivity
import com.core.fy.android.function.brv.model.FullSpanModel
import com.core.fy.android.function.brv.model.SimpleModel
import com.drake.brv.annotaion.AnimationType
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.linear
import com.drake.brv.utils.setup
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.util.extensions.ui.toast

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/17 10:06
 * @description
 * @author Yuan
 */
class MultiTypeFragment : ReflectBindingFragment<FragmentBrvMultitypeBinding, BrvActivity>() {

    override fun initView() {
        super.initView()
        binding.rv.linear().setup {
            setAnimation(AnimationType.SCALE)
            addType<SimpleModel>(R.layout.item_simple)
            addType<FullSpanModel>(R.layout.item_full_span)
        }.models = getData()

        // 点击事件
        binding.rv.bindingAdapter.onClick(R.id.item) {
            when (itemViewType) {
                R.layout.item_simple -> getContext()?.toast("类型1")
                else -> getContext()?.toast("类型2")
            }
        }
    }

    private fun getData(): MutableList<Any> {
        return mutableListOf(
            SimpleModel(),
            FullSpanModel(),
            FullSpanModel(),
            SimpleModel(),
            SimpleModel(),
            SimpleModel(),
            SimpleModel(),
            FullSpanModel(),
            FullSpanModel(),
            FullSpanModel(),
            SimpleModel(),
            SimpleModel(),
            SimpleModel()
        )
    }
}