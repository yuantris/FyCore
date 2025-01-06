package com.core.fy.android

import android.os.Bundle
import androidx.activity.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
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
import com.core.libraries.base.ext.launchSync
import com.core.libraries.base.ext.logD
import com.core.libraries.base.ext.logI
import com.core.libraries.base.ext.onClick
import com.core.libraries.base.ext.startActivity
import com.core.libraries.base.ext.toast
import com.drake.brv.BindingAdapter
import com.drake.brv.listener.DefaultItemTouchCallback
import com.drake.brv.utils.divider
import com.drake.brv.utils.grid
import com.drake.brv.utils.setup
import kotlinx.coroutines.flow.collectLatest

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

    private val list by lazy {
        listOf(
            Function(0, Design.KEYBOARD.function),
            Function(1, Design.ROOM.function),
            Function(2, Design.DIALOG.function),
            Function(3, Design.TOAST.function),
            Function(4, Design.VIEW_VISIBILITY.function),
            Function(5, Design.EVENT.function),
            Function(6, Design.COLL_BAR.function),
        )
    }

    override fun getStatusBarColor(): BarColor {
        return BarColor.WHITE
    }

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        launchSync {
            functionVM.repository.getAllList()?.let {
                if (it.isEmpty()){
                    functionVM.repository.dao.insertAll(list)
                }
            }
        }
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
                        super.onDrag(source, target)
                        ("source: ${source.getModel<Function>().design},position: ${source.modelPosition} " +
                                "\ntarget: ${target.getModel<Function>().design},position: ${target.modelPosition}").logI()
                        models?.iterator()?.let {
                            while (it.hasNext()) {
                                val model = it.next()
                                if (model is Function) {
                                    val temp = model.design
                                    temp.logD()
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
                            Design.VIEW_VISIBILITY.function -> startActivity(VisibilityActivity::class.java)
                            Design.EVENT.function -> startActivity(EventActivity::class.java)
                            Design.COLL_BAR.function -> startActivity(CollapsingBarActivity::class.java)
                        }
                    }
                }
            }.models = list
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