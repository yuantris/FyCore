package com.core.fy.android.function.lottie

import android.os.Bundle
import com.core.fy.android.databinding.ActivityLottieBinding
import io.core.common.base.component.activity.ReflectBindingActivity

class LottieActivity : ReflectBindingActivity<ActivityLottieBinding>() {
    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)

        throw NullPointerException("测试异常")
        binding.lavLottie.apply {
            setAnimation("lottie/chun(2).json")
//            setAnimation("lottie/welcome.json")
        }
    }
}