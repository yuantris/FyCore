package com.core.fy.android.function.brv.fragment

import com.core.fy.android.R
import com.core.fy.android.databinding.FragmentBrvMultitypeBinding
import com.core.fy.android.function.brv.BrvActivity
import com.core.fy.android.function.brv.model.FullSpanModel
import com.core.fy.android.function.brv.model.SimpleModel
import io.core.ui.base.component.fragment.ReflectBindingFragment
import io.core.utils.extensions.ui.toast
import io.core.engine.brv.annotaion.AnimationType
import io.core.engine.brv.utils.bindingAdapter
import io.core.engine.brv.utils.linear
import io.core.engine.brv.utils.setup

/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
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