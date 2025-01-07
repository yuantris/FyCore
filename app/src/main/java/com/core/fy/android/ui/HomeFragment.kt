package com.core.fy.android.ui

import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import com.core.fy.android.MainActivity
import com.core.fy.android.R
import com.core.fy.android.databinding.FragmentHomeBinding
import com.core.fy.android.databinding.ItemFunctionBinding
import com.core.fy.android.function.CollapsingBarActivity
import com.core.fy.android.function.CustomToastActivity
import com.core.fy.android.function.DialogActivity
import com.core.fy.android.function.EventActivity
import com.core.fy.android.function.KeyboardActivity
import com.core.fy.android.function.RoomActivity
import com.core.fy.android.function.VisibilityActivity
import com.core.fy.android.interfaces.LeastAnimationStateChangedHandler
import com.core.fy.android.room.VMFactory
import com.core.fy.android.room.entity.Function
import com.core.fy.android.room.repository.FunctionRepository
import com.core.fy.android.viewmodel.FunctionVM
import com.core.libraries.base.ext.BarColor
import com.core.libraries.base.ext.launchAsync
import com.core.libraries.base.ext.onClick
import com.core.libraries.base.ext.startActivity
import com.core.libraries.base.fragment.HomeBindingFragment
import com.core.libraries.base.fragment.ReflectBindingFragment
import com.drake.brv.BindingAdapter
import com.drake.brv.listener.DefaultItemTouchCallback
import com.drake.brv.utils.divider
import com.drake.brv.utils.grid
import com.drake.brv.utils.setup

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

    private val functionVM by viewModels<FunctionVM> {
        VMFactory(FunctionRepository.singletonCreate())
    }


    override fun initView() {
        binding.state.stateChangedHandler = LeastAnimationStateChangedHandler()
        binding.state.onRefresh {
            functionVM.initRvData()
        }.showLoading()

    }

    override fun initData() {
        functionVM.data.observe(this) {
            binding.rv.apply {
                grid(2).divider {
                    setDrawable(R.drawable.divider_horizontal)
                    startVisible = false
                    endVisible = false
                }.setup {
                    addType<Function>(R.layout.item_function)
                    itemTouchHelper = ItemTouchHelper(object : DefaultItemTouchCallback() {
                        override fun onDrag(
                            source: BindingAdapter.BindingViewHolder,
                            target: BindingAdapter.BindingViewHolder
                        ) {
                            launchAsync {
                                models?.forEachIndexed { index, model ->
                                    if (model is Function) {
                                        model.position = index
                                        // 更新位置信息
                                        functionVM.repository.update(model)
                                    }
                                }
                            }
                        }
                    })
                    onBind {
                        val binding = getBinding<ItemFunctionBinding>()
                        val data = getModel<Function>()
                        binding.item.text = data.design.function
                        binding.item.onClick {
                            when (data.design) {
                                FunctionVM.Design.KEYBOARD -> startActivity(KeyboardActivity::class.java)
                                FunctionVM.Design.ROOM -> startActivity(RoomActivity::class.java)
                                FunctionVM.Design.DIALOG -> startActivity(DialogActivity::class.java)
                                FunctionVM.Design.TOAST -> startActivity(CustomToastActivity::class.java)
                                FunctionVM.Design.EVENT -> startActivity(EventActivity::class.java)
                                FunctionVM.Design.COLL_BAR -> startActivity(CollapsingBarActivity::class.java)
                                FunctionVM.Design.VIEW_VISIBILITY -> startActivity(
                                    VisibilityActivity::class.java
                                )
                            }
                        }
                    }
                }.models = it?.ifEmpty { functionVM.list }
            }
            binding.state.showContent()
        }
    }
}