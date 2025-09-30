package com.core.fy.android.function.fold

import android.os.Bundle
import android.view.View
import com.core.fy.android.databinding.ActivityFoldBinding
import com.google.android.material.appbar.AppBarLayout
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.delegate.IntentExtra
import io.core.common.util.extensions.ui.getExtra
import io.core.common.util.log.d
import kotlin.math.abs

class FoldActivity: ReflectBindingActivity<ActivityFoldBinding>() {

    private val param1 by IntentExtra<String>("param1")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        "使用拓展函数获取参数：${getExtra<String>("param1")}".d()
        "使用委托获取参数：$param1".d()
    }
}
