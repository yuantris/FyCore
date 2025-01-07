package com.core.fy.android.function

import android.os.Bundle
import android.view.Gravity
import com.core.fy.android.databinding.ActivityCustomToastBinding
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.base.ext.getActivity
import com.core.libraries.base.ext.logD
import com.core.libraries.other.Toast

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
                Toast.Builder(this@CustomToastActivity)
                    .setMessage("自定义Toast")
                    .setGravity(Gravity.TOP)
                    .create().show()
            }
            showCenter.setOnClickListener {
                Toast.Builder(this@CustomToastActivity)
                    .setMessage("点什么点点什么点点什么点点什么点点什么点点什么点")
                    .setGravity(Gravity.CENTER)
                    .create().show()
            }
            showBottom.setOnClickListener {
                Toast.Builder(this@CustomToastActivity)
                    .setMessage("自定义Toast")
                    .setGravity(Gravity.BOTTOM)
                    .create().show()
            }
            cancel.setOnClickListener {
                // Toast.cancel(this@CustomToastActivity)
            }
        }
    }
}