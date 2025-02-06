package com.core.fy.android

import android.os.Bundle
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.blankj.utilcode.util.AppUtils.exitApp
import com.core.fy.android.databinding.ActivityMainBinding
import com.core.fy.android.databinding.ItemTabBinding
import com.core.fy.android.interfaces.FragmentPagerAdapter
import com.core.fy.android.model.Tab
import com.core.fy.android.main.fragment.BlankFragment
import com.core.fy.android.main.fragment.HomeFragment
import com.core.fy.android.ui.ConfigDialog
import com.core.fy.android.util.ClickSequenceHandler
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.base.component.fragment.BaseFragment
import io.core.common.helper.dialogs.alert
import io.core.common.util.ext.exitApp
import io.core.common.util.ext.notifyAllDataChanged
import io.core.common.util.ext.ui.BarColor
import io.core.common.util.ext.ui.getCompatColor
import io.core.common.util.ext.ui.hide
import io.core.common.util.ext.ui.onClick
import io.core.common.util.ext.ui.show
import io.core.common.util.ext.ui.showDialogFragment
import io.core.common.util.ext.ui.toast
import io.core.common.util.log.LogCat
import io.core.common.util.tools.TimeUtils
import io.core.engine.brv.BindingAdapter
import io.core.engine.brv.utils.grid
import io.core.engine.brv.utils.setup

class MainActivity : ReflectBindingActivity<ActivityMainBinding>() {

    private val list: List<Tab> = listOf(
        Tab("功能"),
        Tab("待开发"),
    )

    // 当前选中的tab
    private var selectIndex = 0
    private var adapter: BindingAdapter? = null

    override fun initial(savedInstanceState: Bundle?) {
        setTakeOverBackPressed(true)
        super.initial(savedInstanceState)

        binding.appBar.setExpanded(false)

        FragmentPagerAdapter<BaseFragment<*>>(this).apply {
            addFragment(HomeFragment())
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

    override fun onResume() {
        super.onResume()
    }

    override fun setListener() {
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
                showDialogFragment<ConfigDialog>()
            }
        }
    }

    override fun onBackPressedCall() {
        alert("温馨提示", "是否退出应用？") {
            cancelButton {}
            okButton {
                exitApp()
            }
        }
    }

    override fun getStatusBarColor(): BarColor {
        return BarColor.WHITE
    }

}