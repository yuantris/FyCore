package com.core.fy.android

import android.os.Bundle
import android.view.ViewGroup
import androidx.databinding.adapters.ViewBindingAdapter.setPadding
import com.blankj.utilcode.util.BarUtils
import com.core.fy.android.databinding.ActivityMainBinding
import com.core.fy.android.databinding.ItemTabBinding
import com.core.fy.android.interfaces.FragmentPagerAdapter
import com.core.fy.android.model.Tab
import com.core.fy.android.ui.HomeFragment
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.base.ext.BarColor
import com.core.libraries.base.fragment.BaseFragment
import com.drake.brv.utils.grid
import com.drake.brv.utils.setup

class MainActivity : ReflectBindingActivity<ActivityMainBinding>() {

    private val list: List<Tab> = listOf(
        Tab("功能"),
    )

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
//        binding.toolbar.apply {
//            layoutParams = ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                BarUtils.getStatusBarHeight() + BarUtils.getActionBarHeight()
//            )
//        }
        val pagerAdapter = FragmentPagerAdapter<BaseFragment<*>>(this).apply {
            addFragment(HomeFragment())
            binding.vpHomePager.adapter = this
        }

        binding.rvHomeTab.grid(2).setup {
            addType<Tab>(R.layout.item_tab)
            onBind {
                val binding = getBinding<ItemTabBinding>()
                val data = getModel<Tab>()
                binding.tvTabDesignTitle.text = data.type
            }
        }.models = list
    }

    override fun observers() {
    }

    override fun getStatusBarColor(): BarColor {
        return BarColor.WHITE
    }

}