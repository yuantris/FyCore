package com.core.fy.android.function.brv

import android.os.Bundle
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil.getBinding
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.DeviceUtils.getModel
import com.core.fy.android.R
import com.core.fy.android.databinding.ActivityBrvBinding
import com.core.fy.android.databinding.ItemTabBinding
import com.core.fy.android.function.brv.fragment.CheckModeFragment
import com.core.fy.android.function.brv.fragment.GroupDragFragment
import com.core.fy.android.function.brv.fragment.GroupFragment
import com.core.fy.android.function.brv.fragment.HoverLinearFragment
import com.core.fy.android.function.brv.fragment.MultiTypeFragment
import com.core.fy.android.interfaces.FragmentPagerAdapter
import com.core.fy.android.model.Tab
import com.drake.brv.utils.bindingAdapter
import com.drake.brv.utils.linear
import com.drake.brv.utils.setup
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.base.component.fragment.BaseFragment
import io.core.common.util.extensions.ui.notifyAllDataChanged
import io.core.common.util.extensions.ui.disableEdgeEffect
import io.core.common.util.extensions.ui.getCompatColor
import io.core.common.util.extensions.ui.hide
import io.core.common.util.extensions.ui.screenWidthPx
import io.core.common.util.extensions.ui.show

class BrvActivity : ReflectBindingActivity<ActivityBrvBinding>() {

    private val list: List<Tab> = listOf(
        Tab("选择模式"),
        Tab("多类型"),
        Tab("拖拽分组"),
        Tab("分组"),
        Tab("悬停"),
    )

    // 当前选中的tab
    private var selectIndex = 0

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        FragmentPagerAdapter<BaseFragment<*>>(this).apply {
            addFragment(CheckModeFragment())
            addFragment(HoverLinearFragment())
            addFragment(GroupDragFragment())
            addFragment(GroupFragment())
            addFragment(MultiTypeFragment())

            binding.vpHomePager.adapter = this
        }

        binding.rv.disableEdgeEffect()
        binding.rv.linear(RecyclerView.HORIZONTAL).setup {
            addType<Tab>(R.layout.item_tab)
            onBind {
                val binding = getBinding<ItemTabBinding>()
                binding.itemRoot.layoutParams =
                    ViewGroup.LayoutParams(screenWidthPx / 4, ViewGroup.LayoutParams.WRAP_CONTENT)

                val data = getModel<Tab>()
                binding.tvTabDesignTitle.text = data.type
                if (selectIndex == modelPosition) {
                    binding.tvTabDesignTitle.setTextColor(
                        context.getCompatColor(R.color.common_accent_color)
                    )
                    binding.vTabDesignLine.show()
                } else {
                    binding.tvTabDesignTitle.setTextColor(
                        context.getCompatColor(R.color.black25)
                    )
                    binding.vTabDesignLine.hide()
                }
            }

            onClick(R.id.item_root) {
                val index = modelPosition
                binding.vpHomePager.setCurrentItem(index, true)
            }
        }.models = list

        binding.vpHomePager.addOnPageSelectedListener {
            selectIndex = it
            binding.rv.smoothScrollToPosition(it)
            binding.rv.bindingAdapter.notifyAllDataChanged()
        }
    }

}