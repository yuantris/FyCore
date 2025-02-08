package com.core.fy.android.function.keyboard

import android.os.Bundle
import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import com.core.fy.android.databinding.ActivityKeyboardBinding
import io.core.common.base.component.activity.BaseInputActivity
import io.core.common.util.log.logD
import io.core.common.util.log.logV
import io.core.common.helper.valid.ValidHelper

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2024/12/24 10:57
 * @description
 * @author Yuan
 */
class KeyboardActivity : BaseInputActivity<ActivityKeyboardBinding>() {
    private val TAG by lazy { "KeyboardActivity_" }

    override fun isShowKeyboardDebug(): Boolean {
        return true
    }

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
    }

    override fun setListener() {
        super.setListener()
//        SoftKeyboardGlobal.addSoftKeyboardCallback(object :
//            SoftKeyboardGlobal.SoftKeyboardCallback {
//            override fun onOpen(height: Int) {
//                Log.d(TAG, "onOpen height=$height")
//            }
//
//            override fun onClose() {
//                Log.d(TAG, "onClose")
//            }
//        })
    }

    override fun onKeyboardHeightChanged(height: Int) {
        Log.d(TAG, "onKeyboardHeightChanged height=$height")
        val params = binding.etInput.layoutParams as ConstraintLayout.LayoutParams
        params.bottomMargin = height
        binding.etInput.layoutParams = params
    }
}