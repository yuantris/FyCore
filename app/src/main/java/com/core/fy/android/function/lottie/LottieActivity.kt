package com.core.fy.android.function.lottie

import android.os.Bundle
import com.core.fy.android.databinding.ActivityLottieBinding
import io.core.ui.base.component.activity.ReflectBindingActivity
import io.core.utils.tools.TimeTools
import io.core.utils.tools.TimeTools.convertDateFormat

class LottieActivity : ReflectBindingActivity<ActivityLottieBinding>() {
    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)

//        throw NullPointerException("测试异常")

        val to = TimeTools.convertDateFormat("2021-05-05", "yyyy-MM-dd") to "M-d"

        // 使用方式：convertDateFormat("2023-12-25") to "M-d"
        val result = convertDateFormat("2023-12-25") to "M-d"

        binding.lavLottie.apply {
            setAnimation("lottie/chun(2).json")
//            setAnimation("lottie/welcome.json")
        }
    }
}