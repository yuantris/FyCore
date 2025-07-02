package com.core.fy.android.function.event

import android.os.Bundle
import com.core.fy.android.R
import com.core.fy.android.databinding.ActivityEventBinding
import com.gyf.immersionbar.ImmersionBar
import com.hjq.permissions.Permission
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.base.component.dialog.specific.LoadingAir
import io.core.common.helper.LocationDetail
import io.core.common.helper.LocationFailure
import io.core.common.helper.LocationFetcher
import io.core.common.helper.OnLocationCallback
import io.core.common.util.extensions.cool.GSON
import io.core.common.util.extensions.cool.observeEvent
import io.core.common.util.extensions.cool.postEvent
import io.core.common.util.extensions.cool.requestPermission
import io.core.common.util.extensions.currentTimeMillis
import io.core.common.util.extensions.logD
import io.core.common.util.extensions.logE
import io.core.common.util.extensions.ui.getCompatColor
import io.core.common.util.extensions.ui.onDebouncedClick
import io.core.common.util.extensions.ui.toast
import io.core.common.util.tools.DrawableBuilder
import io.core.other.LiveDataPro

class EventActivity : ReflectBindingActivity<ActivityEventBinding>() {

    private val _ratio = "3:1"

    override fun initial(savedInstanceState: Bundle?) {
        ImmersionBar.setTitleBar(this, binding.titleBar)
        super.initial(savedInstanceState)
        setSupportActionBar(binding.titleBar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_arrow_back)
        }
    }

    override fun setListener() {
        super.setListener()
        "个数：${binding.root.childCount}".logD()
        LocationFetcher.DEBUG = true
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
                onDebouncedClick {
                    // toast(currentTimeMillis.toString())
//                    LiveDataPro.postEvent("123", "")'
                    requestPermission(
                        Permission.ACCESS_FINE_LOCATION,
                        Permission.ACCESS_COARSE_LOCATION
                    ) {
                        LoadingAir.show("正在加载位置信息...")
                        LocationFetcher.startPeriodicLocationUpdates(callback = object :
                            OnLocationCallback {
                            override fun onLocationRetrieved(detail: LocationDetail) {
                                LoadingAir.closeWith {
                                    GSON.toJson(detail).logD()
                                }

                            }



                            override fun onLocationFailed(failure: LocationFailure) {
                                LoadingAir.closeWith {
                                    GSON.toJson(failure).logE()
                                }
                            }
                        })
                    }
                }
            }
        }
    }

    override fun observers() {
        super.observers()
        val description = "这是一个宽高比 %s 的FrameLayout"
        observeEvent<Int>(_ratio) {
            when (it) {
                2 -> {
                    binding.ratioLayout.setSizeRatio(2f, 1f)
                    binding.description.text = description.format("2:1")
                }

                3 -> {
                    binding.ratioLayout.setSizeRatio(3f, 1f)
                    binding.description.text = description.format("3:1")
                }
            }
        }

        LiveDataPro.on("123", String::class.java)
            .with(this) { toast(currentTimeMillis.toString()) }
    }

    override fun onDestroy() {
        super.onDestroy()
        LocationFetcher.stopRealtimeLocationTracking()
        LocationFetcher.stopPeriodicLocationUpdates()
    }
}

