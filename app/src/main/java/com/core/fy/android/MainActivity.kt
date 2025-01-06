package com.core.fy.android

import android.os.Bundle
import androidx.activity.viewModels
import androidx.databinding.DataBindingUtil.getBinding
import androidx.recyclerview.widget.ItemTouchHelper
import com.blankj.utilcode.util.DeviceUtils.getModel
import com.core.fy.android.databinding.ActivityMainBinding
import com.core.fy.android.databinding.ItemFunctionBinding
import com.core.fy.android.function.CollapsingBarActivity
import com.core.fy.android.function.CustomToastActivity
import com.core.fy.android.function.DialogActivity
import com.core.fy.android.function.EventActivity
import com.core.fy.android.function.KeyboardActivity
import com.core.fy.android.function.RoomActivity
import com.core.fy.android.function.VisibilityActivity
import com.core.fy.android.room.VMFactory
import com.core.fy.android.room.entity.Function
import com.core.fy.android.room.repository.FunctionRepository
import com.core.fy.android.room.repository.UserRepository
import com.core.fy.android.viewmodel.FunctionVM
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.base.event.Event
import com.core.libraries.base.event.FEvent
import com.core.libraries.base.event.FlowEventBus
import com.core.libraries.base.event.flowOf
import com.core.libraries.base.ext.BarColor
import com.core.libraries.base.ext.launchAsync
import com.core.libraries.base.ext.launchSync
import com.core.libraries.base.ext.logD
import com.core.libraries.base.ext.logI
import com.core.libraries.base.ext.onClick
import com.core.libraries.base.ext.startActivity
import com.core.libraries.base.ext.toast
import com.core.libraries.base.ext.withMainContext
import com.drake.brv.BindingAdapter
import com.drake.brv.listener.DefaultItemTouchCallback
import com.drake.brv.utils.divider
import com.drake.brv.utils.grid
import com.drake.brv.utils.models
import com.drake.brv.utils.setup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

class MainActivity : ReflectBindingActivity<ActivityMainBinding>() {

    enum class Design(val function: String) {
        KEYBOARD("键盘"),
        ROOM("room"),
        DIALOG("dialog"),
        TOAST("toast"),
        VIEW_VISIBILITY("viewVisibility"),
        EVENT("event"),
        COLL_BAR("collBar"),
    }

    private val functionVM by viewModels<FunctionVM> {
        VMFactory(FunctionRepository.singletonCreate())
    }


    override fun getStatusBarColor(): BarColor {
        return BarColor.WHITE
    }

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        launchAsync {
            functionVM.getAllList().await()?.let {} ?: run {
                functionVM.repository.dao.insertAll(functionVM.list)
            }

            val functions = functionVM.getAllList().await()

            if ((functions?.size ?: 0) <= functionVM.list.size) {
                functionVM.list.forEachIndexed { index, item ->
                    val existItem = functionVM.getFunctionWithDesign(item.design).await()
                    existItem?.let {
                        it.position = index
                        "详情更新：${it.design} ${it.position}".logD()
                        functionVM.repository.update(it)
                    } ?: run {
                        functionVM.repository.insert(item)
                    }

                }
            } else {
                // 找出在 functions 中但不在 list 中的元素
                val onlyInFunctions =
                    functions?.filter { it.design !in functionVM.list.map { bean -> bean.design } }
                onlyInFunctions?.forEach {
                    functionVM.repository.delete(it)
                }
            }

            withContext(Dispatchers.Main) {
                mBinding.rv.apply {
                    grid(3).divider {
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
                            binding.item.text = data.design
                            binding.item.onClick {
                                when (data.design) {
                                    Design.KEYBOARD.function -> startActivity(KeyboardActivity::class.java)
                                    Design.ROOM.function -> startActivity(RoomActivity::class.java)
                                    Design.DIALOG.function -> startActivity(DialogActivity::class.java)
                                    Design.TOAST.function -> startActivity(CustomToastActivity::class.java)
                                    Design.VIEW_VISIBILITY.function -> startActivity(
                                        VisibilityActivity::class.java
                                    )

                                    Design.EVENT.function -> startActivity(EventActivity::class.java)
                                    Design.COLL_BAR.function -> startActivity(CollapsingBarActivity::class.java)
                                }
                            }
                        }
                    }.models = if (functions.isNullOrEmpty()) functionVM.list else functions
                }
            }
        }


    }

    override fun observers() {
        FlowEventBus.observe<Event.Created<String>>(this) {
            "mainactivity showInit -> ${it.data}".logD()
            toast(it.data)
        }

        launchSync {
            FEvent.flowOf<Event.Created<String>>().collectLatest { event ->
                "flowOf mainactivity -> $event".logD()
            }
        }
    }

}