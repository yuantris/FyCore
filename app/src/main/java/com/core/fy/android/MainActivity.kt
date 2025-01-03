package com.core.fy.android

import android.os.Bundle
import androidx.lifecycle.Lifecycle
import com.core.fy.android.databinding.ActivityMainBinding
import com.core.fy.android.function.CustomToastActivity
import com.core.fy.android.function.DialogActivity
import com.core.fy.android.function.EventActivity
import com.core.fy.android.function.KeyboardActivity
import com.core.fy.android.function.RoomActivity
import com.core.fy.android.function.VisibilityActivity
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.base.event.Event
import com.core.libraries.base.event.FEvent
import com.core.libraries.base.event.FlowEventBus
import com.core.libraries.base.event.flowOf
import com.core.libraries.base.ext.launchSafe
import com.core.libraries.base.ext.launchSafeAsync
import com.core.libraries.base.ext.logD
import com.core.libraries.base.ext.logI
import com.core.libraries.base.ext.startActivity
import com.core.libraries.base.ext.toast
import com.core.libraries.util.CoreUtil
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ReflectBindingActivity<ActivityMainBinding>() {
    private val TAG by lazy { "MainActivity_" }

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        val filename = CoreUtil.File.generateNameNoExtension("mp3")
        //ToastUtil.show("filename: $filename")
        launchSafeAsync {
            toast("filename: $filename")
        }
    }

    override fun setListener() {
        super.setListener()
        mBinding.apply {
            keyboard.setOnClickListener {
                startActivity(KeyboardActivity::class.java)
            }
            room.setOnClickListener {
                startActivity(RoomActivity::class.java)
            }
            dialog.setOnClickListener {
                startActivity(DialogActivity::class.java)
            }
            toast.setOnClickListener {
                startActivity(CustomToastActivity::class.java)
            }
            viewVisibility.setOnClickListener {
                startActivity(VisibilityActivity::class.java)
            }
            event.setOnClickListener {
                startActivity(EventActivity::class.java)
            }
        }
    }

    override fun onResume() {
        super.onResume()


    }

    override fun observers() {
        FlowEventBus.observe<Event.Created<String>>(this) {
            "mainactivity showInit -> ${it.data}".logD()
            toast(it.data)
        }

        launchSafe {
            FEvent.flowOf<Event.Created<String>>().collectLatest { event ->
                "flowOf mainactivity -> $event".logD()
            }
        }
    }

}