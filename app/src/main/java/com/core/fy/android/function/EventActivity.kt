package com.core.fy.android.function

import com.core.fy.android.databinding.ActivityEventBinding
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.base.event.Event
import com.core.libraries.base.event.FEvent
import com.core.libraries.base.event.FlowEventBus
import com.core.libraries.base.event.flowOf
import com.core.libraries.base.ext.launchSync
import com.core.libraries.base.ext.logD
import com.core.libraries.base.ext.toast
import kotlinx.coroutines.flow.collectLatest

class EventActivity : ReflectBindingActivity<ActivityEventBinding>() {

    override fun setListener() {
        super.setListener()
        binding.apply {
            fEvent.setOnClickListener {
                //FEvent.post(ParentEvent.ChildEvent(), ParentEvent::class.java)
                //FEvent.post(Event.ShowInit("234"))
                launchSync {
                    FEvent.emit(Event.Created("567"))
                }
            }
            flowEvent.setOnClickListener {
                FlowEventBus.post(Event.Created("123"))
            }

            ivImg.setOnLongClickListener {

                false
            }
        }
    }

    override fun observers() {
        super.observers()
        launchSync {
            FEvent.flowOf<Event.Created<String>>().collectLatest { event ->
                "flowOf ParentEvent -> $event".logD()
            }
//            FEvent.flowOf<ParentEvent.ChildEvent>().collect { event ->
//                "flowOf ParentEvent -> $event".logD()
//            }
        }
        FlowEventBus.observe<Event.Created<String>>(this) {
            "flowOf showInit -> ${it.data}".logD()
            toast(it.data)
        }
    }

    private data class SampleEvent(
        val name: String = "Tome",
    )

    sealed interface ParentEvent {
        data class ChildEvent(val name: String = "child") : ParentEvent
    }
}

