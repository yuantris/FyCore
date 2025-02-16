package com.core.fy.android

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import com.core.fy.android.databinding.ActivityMainBinding
import com.core.fy.android.databinding.HomeNavigationItemBinding
import com.core.fy.android.interfaces.FragmentPagerAdapter
import com.core.fy.android.main.fragment.HomeFragment
import com.core.fy.android.main.fragment.SetFragment
import com.gyf.immersionbar.ktx.navigationBarHeight
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.base.component.fragment.BaseFragment
import io.core.common.helper.dialogs.showDialog
import io.core.common.helper.rv.ItemViewHolder
import io.core.common.helper.rv.RecyclerAdapter
import io.core.common.util.DiveGestureLine
import io.core.common.util.ext.exitApp
import io.core.common.util.ext.notifyAllDataChanged
import io.core.common.util.ext.ui.onClick
import io.core.common.util.ext.ui.setPaddingBottom
import io.core.common.util.processNavigationBar
import io.core.engine.brv.utils.disableEdgeEffect
import io.core.widget.layout.NestedViewPager

class MainActivity : ReflectBindingActivity<ActivityMainBinding>() {

    private var navigationAdapter: NavigationAdapter? = null
    private var pagerAdapter: FragmentPagerAdapter<BaseFragment<*>>? = null

    override fun initial(savedInstanceState: Bundle?) {
        setTakeOverBackPressed(true)
        super.initial(savedInstanceState)

        navigationAdapter = NavigationAdapter().apply {
            addItem(
                MenuItem(
                    "首页",
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.home_home_selector)
                )
            )
            addItem(
                MenuItem(
                    "我的",
                    ContextCompat.getDrawable(this@MainActivity, R.drawable.home_me_selector)
                )
            )
            binding.rvHomeNavigation.layoutManager =
                GridLayoutManager(this@MainActivity, this.itemCount)
            binding.rvHomeNavigation.adapter = this
            binding.rvHomeNavigation.processNavigationBar()
            binding.rvHomeNavigation.disableEdgeEffect()
        }

        pagerAdapter = FragmentPagerAdapter<BaseFragment<*>>(this).apply {
            addFragment(HomeFragment())
            addFragment(SetFragment())
            binding.vpHomePager.adapter = this
            binding.vpHomePager.addOnPageSelectedListener {
                navigationAdapter?.setSelectedPosition(it)
            }
        }


    }

    private fun switchFragment(fragmentIndex: Int) {
        if (fragmentIndex == -1) {
            return
        }
        when (fragmentIndex) {
            0, 1, 2, 3 -> {
                binding.vpHomePager.currentItem = fragmentIndex
                navigationAdapter?.setSelectedPosition(fragmentIndex)
            }
        }
    }

    override fun onBackPressedCall() {
        showDialog("温馨提示", "是否退出应用？") {
            cancelButton {}
            okButton {
                exitApp()
            }
        }
    }


    inner class NavigationAdapter :
        RecyclerAdapter<MenuItem, HomeNavigationItemBinding>(this@MainActivity) {

        /** 当前选中条目位置 */
        private var selectedPosition: Int = 0

        override fun getViewBinding(parent: ViewGroup): HomeNavigationItemBinding {
            return HomeNavigationItemBinding.inflate(inflater, parent, false)
        }

        override fun registerListener(holder: ItemViewHolder, binding: HomeNavigationItemBinding) {
            holder.itemView.onClick {
                switchFragment(holder.layoutPosition)
            }
        }

        fun setSelectedPosition(position: Int) {
            selectedPosition = position
            notifyAllDataChanged()
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: HomeNavigationItemBinding,
            item: MenuItem,
            payloads: MutableList<Any>
        ) {
            binding.ivHomeNavigationIcon.setImageDrawable(item.getDrawable())
            binding.tvHomeNavigationTitle.text = item.getText()
            binding.ivHomeNavigationIcon.isSelected = (selectedPosition == holder.layoutPosition)
            binding.tvHomeNavigationTitle.isSelected = (selectedPosition == holder.layoutPosition)
        }

    }

    class MenuItem(private val text: String?, private val drawable: Drawable?) {

        fun getText(): String? {
            return text
        }

        fun getDrawable(): Drawable? {
            return drawable
        }
    }

}