package com.core.fy.android.ui.fragment

import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import com.core.fy.android.MainActivity
import com.core.fy.android.R
import com.core.fy.android.databinding.FragmentHomeBinding
import com.core.fy.android.databinding.ItemFunctionBinding
import com.core.fy.android.function.ClickTextActivity
import com.core.fy.android.function.CollapsingBarActivity
import com.core.fy.android.function.CustomToastActivity
import com.core.fy.android.function.DialogActivity
import com.core.fy.android.function.EventActivity
import com.core.fy.android.function.ImgTextActivity
import com.core.fy.android.function.KeyboardActivity
import com.core.fy.android.function.RoomActivity
import com.core.fy.android.function.TTSActivity
import com.core.fy.android.function.VisibilityActivity
import com.core.fy.android.function.read.ReadBookActivity
import com.core.fy.android.interfaces.LeastAnimationStateChangedHandler
import com.core.fy.android.room.VMFactory
import com.core.fy.android.room.entity.Function
import com.core.fy.android.room.repository.FunctionRepository
import com.core.fy.android.viewmodel.FunctionVM
import io.core.common.util.ext.cool.launchAsync
import io.core.common.util.log.logD
import io.core.common.util.ext.ui.onClick
import io.core.common.util.ext.ui.startActivity
import io.core.common.base.fragment.ReflectBindingFragment
import io.core.other.Toast
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
        VMFactory(FunctionRepository.create())
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
                                        functionVM.repository.dao.update(model)
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
                                FunctionVM.Design.云创控件 -> startActivity(ImgTextActivity::class.java)
                                FunctionVM.Design.单文字点击的TextView -> startActivity(ClickTextActivity::class.java)
                                FunctionVM.Design.ROOM -> startActivity(RoomActivity::class.java)
                                FunctionVM.Design.DIALOG -> startActivity(DialogActivity::class.java)
                                FunctionVM.Design.TOAST -> startActivity(CustomToastActivity::class.java)
                                FunctionVM.Design.EVENT -> startActivity(EventActivity::class.java)
                                FunctionVM.Design.COLL_BAR -> startActivity(CollapsingBarActivity::class.java)
                                FunctionVM.Design.VIEW_VISIBILITY -> startActivity(
                                    VisibilityActivity::class.java
                                )
                                FunctionVM.Design.TTS -> startActivity(TTSActivity::class.java)
                                FunctionVM.Design.READ -> startActivity(ReadBookActivity::class.java)
                                else -> {
                                    // do nothing
                                    Toast.Builder(requireContext())
                                        .setMessage("该添加点击事件了")
                                        .create()
                                        .show()
                                }
                            }
                        }
                    }
                }.models = it?.ifEmpty { functionVM.list }
            }
            binding.state.showContent()
        }
    }

    override fun onFragmentResume(first: Boolean) {
        "是否首次调用：$first".logD()
    }
}