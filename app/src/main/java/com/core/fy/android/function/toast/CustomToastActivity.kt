package com.core.fy.android.function.toast

import android.os.Bundle
import android.view.Gravity
import com.core.fy.android.databinding.ActivityCustomToastBinding
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.base.component.custom.ToastGT
import io.core.common.util.extensions.ui.toast
import com.core.fy.android.ui.CustomToast

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2024/12/27 11:09
 * @description
 * @author Yuan
 */
class CustomToastActivity : ReflectBindingActivity<ActivityCustomToastBinding>() {

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)

    }

    override fun setListener() {
        super.setListener()
        binding.apply {
            showTop.setOnClickListener {
                ToastGT.Builder(this@CustomToastActivity)
                    .setMessage("自定义消息")
                    .setDuration(3000)
                    .setAppearance(RedToastAppearance())
                    .setAnimation(SlideAnimationStrategy())
                    .setPosition(TopPositionStrategy())
                    .show()
            }
            showCenter.setOnClickListener {


                ToastGT.showWithQueue(this@CustomToastActivity, "第一条消息")
                ToastGT.showWithQueue(this@CustomToastActivity, "第二条消息")
                ToastGT.showWithQueue(this@CustomToastActivity, "第三条消息")
            }
            showBottom.setOnClickListener {
                toast("自定义Toast")
            }
            cancel.setOnClickListener {
                // Toast.cancel(this@CustomToastActivity)
            }
        }
    }
}