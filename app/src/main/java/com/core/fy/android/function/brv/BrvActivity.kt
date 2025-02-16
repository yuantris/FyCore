package com.core.fy.android.function.brv

import com.core.fy.android.databinding.ActivityBrvBinding
import io.core.common.base.component.activity.ReflectBindingActivity

class BrvActivity:ReflectBindingActivity<ActivityBrvBinding>() {

    private fun getData(): List<CheckModel> {
        return mutableListOf<CheckModel>().apply {
            for (i in 0..9) add(CheckModel())
        }
    }
}