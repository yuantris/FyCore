package com.core.fy.android.function.brv.fragment

import androidx.databinding.DataBindingUtil.getBinding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.DeviceUtils.getModel
import com.core.fy.android.R
import com.core.fy.android.databinding.FragmentBrvGroupdragBinding
import com.core.fy.android.databinding.ItemGroup1Binding
import com.core.fy.android.function.brv.BrvActivity
import com.core.fy.android.function.brv.model.GroupDrag1Model
import com.core.fy.android.function.brv.model.GroupDrag2Model
import com.drake.brv.BindingAdapter
import com.drake.brv.item.ItemExpand
import com.drake.brv.listener.DefaultItemTouchCallback
import com.drake.brv.utils.linear
import com.drake.brv.utils.setup
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.util.CoreUtil.Companion.toast

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/17 10:36
 * @description
 * @author Yuan
 */
class GroupDragFragment:ReflectBindingFragment<FragmentBrvGroupdragBinding,BrvActivity>() {

    override fun initView() {
        binding.rv.linear().setup {
            addType<GroupDrag1Model>(R.layout.item_group_1)
            addType<GroupDrag2Model>(R.layout.item_group_3)

            onBind {
                when (itemViewType) {
                    R.layout.item_group_1 -> {
                        val item = getModel<GroupDrag1Model>()
                        val group1Binding = getBinding<ItemGroup1Binding>()
                        group1Binding.iv.setImageResource(item.expandIcon)
                    }
                }
            }

            // 自定义部分实现
            itemTouchHelper = ItemTouchHelper(object : DefaultItemTouchCallback() {

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val vh = viewHolder as BindingAdapter.BindingViewHolder
                    vh.collapse() // 侧滑删除分组前先折叠子列表
                    super.onSwiped(viewHolder, direction)

                    // 如果侧滑删除的是子列表, 要删除对应分组的getItemSublist, 避免刷新再次被加载出来
                    // getItemSublist必须发挥可变集合, 否则无法删除
                    (vh.findParentViewHolder()?.getModelOrNull<ItemExpand>()?.getItemSublist() as? MutableList)?.remove(vh.getModelOrNull())
                }

                override fun onMove(
                    recyclerView: RecyclerView,
                    source: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    // 拖拽分组前先折叠子列表
                    (source as BindingAdapter.BindingViewHolder).collapse()
                    (target as BindingAdapter.BindingViewHolder).collapse()
                    return super.onMove(recyclerView, source, target)
                }
            })

            R.id.item.onFastClick {
                when (itemViewType) {
                    R.layout.item_group_2, R.layout.item_group_1 -> {

                        val changeCount =
                            if (getModel<ItemExpand>().itemExpand) "折叠 ${expandOrCollapse()} 条" else "展开 ${expandOrCollapse()} 条"

                        toast(changeCount)
                    }
                }
            }

        }.models = getData()
    }

    private fun getData(): MutableList<GroupDrag1Model> {
        return MutableList(4) { GroupDrag1Model() }
    }
}