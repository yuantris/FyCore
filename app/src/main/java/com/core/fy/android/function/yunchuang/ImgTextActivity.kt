package com.core.fy.android.function.yunchuang

import android.os.Bundle
import com.core.fy.android.R
import com.core.fy.android.databinding.ActivityImgTextBinding
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.util.ext.ifNotNull
import io.core.common.util.ext.ui.getCompatDrawable
import io.core.common.util.ext.ui.toast

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

        getString("title").ifNotNull {
            binding.titleBar.setTitle(it)
        }

        binding.apply {
            getCompatDrawable(R.drawable.ic_launcher_background)?.let {
                civImg.setBackgroundImage(it)
            }
            getCompatDrawable(R.drawable.splash_1).ifNotNull {
                civImg.setActiveBackground(it)
            }
            civImg.setOnClickListener {
                toast("点击了")
            }
        }
    }
}