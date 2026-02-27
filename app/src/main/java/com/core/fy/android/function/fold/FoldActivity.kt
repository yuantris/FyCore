package com.core.fy.android.function.fold

import android.os.Bundle
import android.view.View
import com.core.fy.android.databinding.ActivityFoldBinding
import com.google.android.material.appbar.AppBarLayout
import io.core.ui.base.component.activity.ReflectBindingActivity
import io.core.ui.delegate.IntentExtra
import io.core.utils.extensions.ui.getExtra
import io.core.utils.log.d
import kotlin.math.abs

class FoldActivity: ReflectBindingActivity<ActivityFoldBinding>() {

    private val param1 by IntentExtra<String>("param1")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        "使用拓展函数获取参数�?{getExtra<String>("param1")}".d()
        "使用委托获取参数�?param1".d()
    }
}
