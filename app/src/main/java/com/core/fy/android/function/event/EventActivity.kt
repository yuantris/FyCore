package com.core.fy.android.function.event

import com.core.fy.android.R
import com.core.fy.android.databinding.ActivityEventBinding
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.util.ext.cool.observeEvent
import io.core.common.util.ext.cool.postEvent
import io.core.common.util.ext.currentTimeMillis
import io.core.common.util.ext.ui.getCompatColor
import io.core.common.util.ext.ui.onClick
import io.core.common.util.log.logD
import io.core.common.util.ext.ui.toast
import io.core.common.util.tools.DrawableBuilder

class EventActivity : ReflectBindingActivity<ActivityEventBinding>() {

    private val _ratio = "3:1"

    override fun setListener() {
        super.setListener()
        "个数：${binding.root.childCount}".logD()
        binding.apply {
            fEvent.setOnClickListener {
                postEvent(_ratio, 3)
            }

            flowEvent.setOnClickListener {
                postEvent(_ratio, 2)
            }

            ivImg.apply {
                background =
                    DrawableBuilder
                        .setRadius(12f)
                        .setSolidColor(context.getCompatColor(R.color.md_amber_A200))
                        .build()
                onClick {
                    toast(currentTimeMillis.toString())
                }
            }
        }
    }

    override fun observers() {
        super.observers()
        observeEvent<Int>(_ratio) {
            when (it) {
                2 -> binding.ratioLayout.setSizeRatio(2f, 1f)
                3 -> binding.ratioLayout.setSizeRatio(3f, 1f)
            }
        }
    }

}

