package com.core.fy.android.function

import android.os.Bundle
import com.core.fy.android.R
import com.core.fy.android.databinding.ActivityImgTextBinding
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.base.ext.toast

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/9 11:38
 * @description
 * @author Yuan
 */
class ImgTextActivity : ReflectBindingActivity<ActivityImgTextBinding>() {

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)

        binding.apply {
            civImg.setBackgroundImage(resources.getDrawable(R.drawable.ic_launcher_background))
            civImg.setActiveBackground(resources.getDrawable(R.drawable.splash_1))
            civImg.setOnClickListener {
                toast("点击了")
            }
        }
    }
}