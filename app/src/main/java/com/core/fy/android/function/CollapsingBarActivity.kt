package com.core.fy.android.function

import android.os.Bundle
import com.core.fy.android.databinding.ActivityCollapsingBarBinding
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.helper.StatusBarManager

class CollapsingBarActivity : ReflectBindingActivity<ActivityCollapsingBarBinding>() {
    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)

        binding.rootView.setOnCollapseStateChangeListener {
            StatusBarManager.with(this@CollapsingBarActivity)
                .updateStatusBarManually(it)
        }
    }
}