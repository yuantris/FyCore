package com.core.fy.android

import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import com.core.fy.android.databinding.ActivityMainBinding
import com.core.fy.android.databinding.HomeNavigationItemBinding
import com.core.fy.android.interfaces.FragmentPagerAdapter
import com.core.fy.android.main.fragment.HomeFragment
import com.core.fy.android.main.fragment.SetFragment
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.base.component.adapter.BaseRecyclerAdapter
import io.core.common.base.component.adapter.BaseViewHolder
import io.core.common.base.component.adapter.createBindingViewHolder
import io.core.common.base.component.fragment.BaseFragment
import io.core.common.helper.dialogs.showDialog
import io.core.common.helper.rv.ItemViewHolder
import io.core.common.helper.rv.RecyclerAdapter
import io.core.common.util.extensions.exitApp
import io.core.common.util.extensions.ui.disableEdgeEffect
import io.core.common.util.extensions.ui.getCompatDrawable
import io.core.common.util.extensions.ui.notifyAllDataChanged
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.processNavigationBar

class MainActivity : ReflectBindingActivity<ActivityMainBinding>() {

    private var navigationAdapter: NavigationV2Adapter? = null
    private var pagerAdapter: FragmentPagerAdapter<BaseFragment<*>>? = null

    override fun initial(savedInstanceState: Bundle?) {
        setTakeOverBackPressed(true)
        super.initial(savedInstanceState)

        navigationAdapter = NavigationV2Adapter().apply {
            listOf(
                "首页" to R.drawable.home_home_selector,
                "我的" to R.drawable.home_me_selector
            ).forEach { (title, iconRes) ->
                addItem(MenuItem(title, this@MainActivity.getCompatDrawable(iconRes)))
            }
            itemClickListener = BaseRecyclerAdapter.OnItemClickListener { _, position ->
                switchFragment(position)
            }
            with(binding.rvHomeNavigation) {
                layoutManager = GridLayoutManager(this@MainActivity, itemCount)
                adapter = this@apply
                processNavigationBar()
                disableEdgeEffect()
            }
        }

        pagerAdapter = FragmentPagerAdapter<BaseFragment<*>>(this).apply {
            listOf(HomeFragment(), SetFragment()).forEach(::addFragment)
            with(binding.vpHomePager) {
                adapter = this@apply
                addOnPageSelectedListener {
                    navigationAdapter?.setSelectedPosition(it)
                }
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

    inner class NavigationV2Adapter : BaseRecyclerAdapter<MenuItem>() {

        /** 当前选中条目位置 */
        private var selectedPosition: Int = 0

        fun setSelectedPosition(position: Int) {
            selectedPosition = position
            notifyAllDataChanged()
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): BaseViewHolder<MenuItem> =
            createBindingViewHolder(parent, HomeNavigationItemBinding::inflate)
            { item, holder, _ ->
                ivHomeNavigationIcon.setImageDrawable(item.getDrawable())
                tvHomeNavigationTitle.text = item.getText()
                val isSelected = (selectedPosition == holder.layoutPosition)
                ivHomeNavigationIcon.isSelected = isSelected
                tvHomeNavigationTitle.isSelected = isSelected
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