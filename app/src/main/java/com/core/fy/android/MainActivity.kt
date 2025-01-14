package com.core.fy.android

import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.core.fy.android.databinding.ActivityMainBinding
import com.core.fy.android.databinding.ItemTabBinding
import com.core.fy.android.interfaces.FragmentPagerAdapter
import com.core.fy.android.model.Tab
import com.core.fy.android.ui.fragment.BlankFragment
import com.core.fy.android.ui.fragment.HomeFragment
import io.core.common.base.component.activity.CrashActivity
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.util.ext.ui.BarColor
import io.core.common.util.ext.ui.hide
import io.core.common.util.ext.ui.show
import io.core.common.base.fragment.BaseFragment
import io.core.common.helper.LifecycleHelp
import io.core.common.util.log.logD
import com.drake.brv.BindingAdapter
import com.drake.brv.utils.grid
import com.drake.brv.utils.setup

class MainActivity : ReflectBindingActivity<ActivityMainBinding>(), OnPageChangeListener {

    private val list: List<Tab> = listOf(
        Tab("功能"),
        Tab("待开发"),
    )

    // 当前选中的tab
    private var selectIndex = 0
    private var adapter: BindingAdapter? = null

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)

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
                            ContextCompat.getColor(context, R.color.common_accent_color)
                        )
                        binding.vTabDesignLine.show()
                    } else {
                        binding.tvTabDesignTitle.setTextColor(
                            ContextCompat.getColor(context, R.color.black25)
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
        val existActivity = LifecycleHelp.isExistActivity(CrashActivity::class.java)
        "Crash 是否销毁 $existActivity".logD()
    }

    override fun setListener() {
        binding.vpHomePager.addOnPageChangeListener(this)
    }
    override fun getStatusBarColor(): BarColor {
        return BarColor.WHITE
    }

    override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

    override fun onPageSelected(position: Int) {
        selectIndex = position
        adapter?.notifyDataSetChanged()
    }

    override fun onPageScrollStateChanged(state: Int) {}

}