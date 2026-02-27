package com.core.fy.android.function.event

import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.core.fy.android.R
import com.core.fy.android.databinding.ActivityEventBinding
import com.gyf.immersionbar.ImmersionBar
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import io.core.ui.base.component.activity.ReflectBindingActivity
import io.core.ui.base.component.dialog.specific.LoadingAir
import io.core.utils.LocationDetail
import io.core.utils.LocationFailure
import io.core.utils.LocationFetcher
import io.core.utils.OnLocationCallback
import io.core.utils.extensions.cool.GSON
import io.core.utils.extensions.cool.observeEvent
import io.core.utils.extensions.cool.postEvent
import io.core.utils.extensions.currentTimeMillis
import io.core.utils.extensions.logD
import io.core.utils.extensions.logE
import io.core.utils.extensions.ui.getCompatColor
import io.core.utils.extensions.ui.onDebouncedClick
import io.core.utils.extensions.ui.toast
import io.core.utils.tools.DrawableBuilder
import io.core.nav.other.LiveDataPro
import io.core.ui.widget.view.StatefulImageViewV2
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        "个数�?{binding.root.childCount}".logD()
        LocationFetcher.DEBUG = true
        binding.apply {

            asyncImg.setOnClickListener {
                lifecycleScope.launch {
//                    asyncImg.syncState(StatefulImageViewV2.ViewState.LOADING)
//                    asyncImg.showLoading()
                    delay(2000L)
                    asyncImg.syncState(StatefulImageViewV2.ViewState.SELECTED)

                    asyncImg.playErrorAnimation()
                }
            }

            stateImg.setOnClickListener {

            }

            effectImg.setOnClickListener {

            }

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
                    XXPermissions.with(this@EventActivity)
                        .permission(
                            Permission.ACCESS_FINE_LOCATION,
                            Permission.ACCESS_COARSE_LOCATION
                        )
                        .request { permissions, allGranted ->
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

