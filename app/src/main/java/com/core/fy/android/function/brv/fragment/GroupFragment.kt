package com.core.fy.android.function.brv.fragment

import com.core.fy.android.R
import com.core.fy.android.databinding.FragmentBrvGroupdragBinding
import com.core.fy.android.databinding.ItemGroup1Binding
import com.core.fy.android.databinding.ItemGroup2Binding
import com.core.fy.android.function.brv.BrvActivity
import com.core.fy.android.function.brv.model.Group1Model
import com.core.fy.android.function.brv.model.Group2Model
import com.core.fy.android.function.brv.model.Group3Model
import com.core.fy.android.function.brv.model.GroupDrag1Model
import com.core.fy.android.function.brv.model.GroupDrag2Model
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.util.CoreUtil.Companion.toast
import io.core.engine.brv.item.ItemExpand
import io.core.engine.brv.utils.linear
import io.core.engine.brv.utils.setup

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/17 10:43
 * @description
 * @author Yuan
 */
class GroupFragment : ReflectBindingFragment<FragmentBrvGroupdragBinding, BrvActivity>() {

    override fun initView() {
        binding.rv.linear().setup {

            // 任何条目都需要添加类型到BindingAdapter中
            addType<Group1Model>(R.layout.item_group_1)
            addType<Group2Model>(R.layout.item_group_2)
            addType<Group3Model>(R.layout.item_group_3)

            onBind {
                when (val model = getModel<Any>()) {
                    is Group1Model -> {
                        val group1Binding = getBinding<ItemGroup1Binding>()
                        group1Binding.iv.setImageResource(model.expandIcon)
                    }

                    is Group2Model -> {
                        val group2Binding = getBinding<ItemGroup2Binding>()
                        group2Binding.iv.setImageResource(model.expandIcon)
                    }
                }
            }

            R.id.item.onFastClick {
                when (itemViewType) {
                    // 点击展开或折叠
                    R.layout.item_group_2, R.layout.item_group_1 -> {

                        val changeCount =
                            if (getModel<ItemExpand>().itemExpand) "折叠 ${expandOrCollapse()} 条" else "展开 ${expandOrCollapse()} 条"

                        toast(changeCount)
                    }
                    // 点击删除嵌套分组
                    R.layout.item_group_3 -> {
                        val model = getModel<Group3Model>()
                        val parentPosition = findParentPosition()
                        if (parentPosition != -1) {
                            (getModel<ItemExpand>(parentPosition).getItemSublist() as MutableList).remove(
                                model
                            )
                            mutable.removeAt(layoutPosition)
                            notifyItemRemoved(layoutPosition)
                        }
                    }
                }
            }

        }.models = getData()
    }

    private fun getData(): MutableList<Group1Model> {
        return mutableListOf<Group1Model>().apply {
            for (i in 0..4) {

                // 第二个分组存在嵌套分组
                if (i == 0) {
                    val nestedGroupModel = Group1Model().apply {
                        sublist = MutableList(3) { Group2Model() }
                    }
                    add(nestedGroupModel)
                    continue
                }

                add(Group1Model())
            }
        }
    }
}