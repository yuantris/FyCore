package com.core.fy.android

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.core.fy.android.databinding.ActivityMainBinding
import com.core.fy.android.databinding.ItemTabBinding
import com.core.fy.android.function.read.model.AudioPlay
import com.core.fy.android.interfaces.FragmentPagerAdapter
import com.core.fy.android.model.Tab
import com.core.fy.android.ui.fragment.BlankFragment
import com.core.fy.android.ui.fragment.HomeFragment
import com.drake.brv.BindingAdapter
import com.drake.brv.utils.grid
import com.drake.brv.utils.setup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.base.fragment.BaseFragment
import io.core.common.helper.dialogs.alert
import io.core.common.util.ext.addCallback
import io.core.common.util.ext.ui.BarColor
import io.core.common.util.ext.ui.hide
import io.core.common.util.ext.ui.show
import io.core.common.util.ext.ui.startService


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

    }

    override fun setListener() {
        binding.vpHomePager.addOnPageChangeListener(this)
        onBackPressedDispatcher.addCallback(this) {
            alert("温馨提示", "是否退出应用？") {
                cancelButton {}
                okButton {
                    finish()
                }
            }
        }
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