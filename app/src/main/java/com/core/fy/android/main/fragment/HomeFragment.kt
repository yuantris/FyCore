package com.core.fy.android.main.fragment

import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.blankj.utilcode.util.VibrateUtils
import com.core.fy.android.MainActivity
import com.core.fy.android.R
import com.core.fy.android.databinding.FragmentHomeBinding
import com.core.fy.android.databinding.ItemTabBinding
import com.core.fy.android.interfaces.FragmentPagerAdapter
import com.core.fy.android.model.Tab
import com.core.fy.android.ui.ConfigDialog
import com.drake.brv.BindingAdapter
import com.drake.brv.utils.grid
import com.drake.brv.utils.setup
import io.core.common.base.component.fragment.BaseFragment
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.util.extensions.ui.getCompatColor
import io.core.common.util.extensions.ui.hide
import io.core.common.util.extensions.ui.notifyAllDataChanged
import io.core.common.util.extensions.ui.show
import io.core.common.util.extensions.ui.showDialogFragment
import io.core.other.ClickSequenceHandler

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/7 17:40
 * @description
 * @author Yuan
 */
class HomeFragment : ReflectBindingFragment<FragmentHomeBinding, MainActivity>() {

    private val list: List<Tab> = listOf(
        Tab("功能"),
        Tab("待开发"),
    )

    // 当前选中的tab
    private var selectIndex = 0
    private var adapter: BindingAdapter? = null


    override fun initView() {
        binding.appBar.setExpanded(false)

        FragmentPagerAdapter<BaseFragment<*>>(this).apply {
            addFragment(StatusFragment())
            addFragment(BlankFragment())
            binding.vpHomePager.adapter = this
        }

        adapter = binding.rvHomeTab
            .grid(2)
            .setup {
                addType<Tab>(R.layout.item_tab)
                onBind {
                    val binding = getBinding<ItemTabBinding>()
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
            }
        adapter?.models = list
    }

    override fun initData() {
        with(binding) {
            vpHomePager.addOnPageChangeListener(object : OnPageChangeListener {
                override fun onPageScrolled(
                    position: Int,
                    positionOffset: Float,
                    positionOffsetPixels: Int
                ) {
                }

                override fun onPageSelected(position: Int) {
                    selectIndex = position
                    adapter?.notifyAllDataChanged()
                }

                override fun onPageScrollStateChanged(state: Int) {}

            })

            ClickSequenceHandler(binding.toolbar) {
                // 立即开始 → 震动50ms → 暂停50ms → 震动50ms
                VibrateUtils.vibrate(longArrayOf(0, 50, 50, 50), -1)
                showDialogFragment<ConfigDialog>()
            }


        }
    }

    override fun onFragmentResume(first: Boolean) {
        super.onFragmentResume(first)
//        getAttachActivity()?.adaptStatusBarToView(
//            rootView = requireActivity().window.decorView,
//            targetView = binding.collTool
//        )
    }

}