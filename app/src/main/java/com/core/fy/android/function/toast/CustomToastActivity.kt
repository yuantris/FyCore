package com.core.fy.android.function.toast

import android.os.Bundle
import android.view.Gravity
import com.core.fy.android.databinding.ActivityCustomToastBinding
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.util.ext.ui.toast
import io.core.common.base.component.dialog.CustomToast

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
                CustomToast.Builder(this@CustomToastActivity)
                    .setMessage("自定义Toast")
                    .setGravity(Gravity.TOP)
                    .build()
                    .show()
            }
            showCenter.setOnClickListener {
                CustomToast.Builder(this@CustomToastActivity)
                    .setMessage("点什么点点什么点点什么点点什么点点什么点点什么点")
                    .setGravity(Gravity.CENTER)
                    .build().show()
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