package com.core.fy.android.function.brv.fragment

import android.view.View
import com.core.fy.android.R
import com.core.fy.android.databinding.FragmentBrvCheckmodeBinding
import com.core.fy.android.function.brv.BrvActivity
import com.core.fy.android.function.brv.model.CheckModel
import io.core.ui.base.component.fragment.ReflectBindingFragment
import io.core.utils.processNavigationBar
import io.core.engine.brv.BindingAdapter
import io.core.engine.brv.utils.bindingAdapter
import io.core.engine.brv.utils.linear
import io.core.engine.brv.utils.setup

/**
# ██████████
# █▄█████▄�?
# █▼▼▼▼▼
# �?
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载…�?
 * 2025/2/17 9:32
 * @description
 * @author Yuan
 */
class CheckModeFragment : ReflectBindingFragment<FragmentBrvCheckmodeBinding, BrvActivity>() {

    override fun initView() {
        binding.llMenu.processNavigationBar()
        binding.rv.linear().setup {
            addType<CheckModel>(R.layout.item_check_mode)

            // 长按列表进入编辑模式
            onLongClick(R.id.item) {
                if (!toggleMode) {
                    toggle()
                    setChecked(layoutPosition, true)
                }
            }

            // 点击列表触发选中
            onFastClick(R.id.cb, R.id.item) {
                // 如果当前未处于选择模式�?点击无效
                if (!toggleMode && it == R.id.item) {
                    return@onFastClick
                }
                var checked = getModel<CheckModel>().checked
                if (it == R.id.item) checked = !checked
                setChecked(layoutPosition, checked)
            }

            // 监听列表选中
            onChecked { position, isChecked, isAllChecked ->
                val model = getModel<CheckModel>(position)
                model.checked = isChecked
                model.notifyChange()

                // 刷新已选择计数�?
                binding.tvCheckedCount.text = "已选择 ${checkedCount}/${modelCount}"
            }

            // 监听切换模式
            onToggle { position, toggleMode, _ ->
                // 刷新列表显示选择按钮
                val model = getModel<CheckModel>(position)
                model.visibility = toggleMode
                model.notifyChange()
                changeListEditable(this)
            }
        }.models = getData()

        initEditMode()
    }

    /**
     * 初始化编辑模式视�?
     */
    private fun initEditMode() {
        val adapter = binding.rv.bindingAdapter

        // 单选模式切�?
        binding.tvSingleMode.setOnClickListener {
            adapter.singleMode = !adapter.singleMode

            // 单选模式不应该支持全�?
            binding.tvAllChecked.isEnabled = !adapter.singleMode
        }

        // 反�?
        binding.tvReverseChecked.setOnClickListener {
            adapter.checkedReverse()
        }

        // 全�?
        binding.tvAllChecked.setOnClickListener {
            adapter.checkedAll()
        }

        // 取消选择
        binding.tvCancelChecked.setOnClickListener {
            adapter.checkedAll(false)
        }

        // 切换选择模式
        binding.tvManage.setOnClickListener {
            adapter.toggle()
            // binding.rv.bindingAdapter.setChecked(0, true) // 一开始就选中第一�?
        }
    }

    /** 改变编辑状�?*/
    private fun changeListEditable(adapter: BindingAdapter) {
        val toggleMode = adapter.toggleMode
        val checkedCount = adapter.checkedCount
        // 管理按钮
        binding.tvManage.text = if (toggleMode) "取消" else "管理"

        // 显示和隐藏编辑菜�?
        binding.llMenu.visibility = if (toggleMode) View.VISIBLE else View.GONE

        // 显示/隐藏计数�?
        binding.tvCheckedCount.visibility = if (toggleMode) View.VISIBLE else View.GONE
        binding.tvCheckedCount.text = "已选择 ${checkedCount}/${adapter.modelCount}"

        // 如果取消管理模式则取消全部已选择
        if (!toggleMode) adapter.checkedAll(false)
    }

    private fun getData(): List<CheckModel> {
        return List(20) { CheckModel() }
    }
}