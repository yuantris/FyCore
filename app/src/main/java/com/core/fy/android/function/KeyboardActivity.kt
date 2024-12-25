package com.core.fy.android.function

import android.os.Bundle
import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import com.core.fy.android.databinding.ActivityKeyboardBinding
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.other.keyboard.KeyboardObserver

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
class KeyboardActivity : ReflectBindingActivity<ActivityKeyboardBinding>() {
    private val TAG by lazy { "KeyboardActivity_" }
    private val observer by lazy { KeyboardObserver.create(this, true) }
    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        observer.watch()
    }

    override fun setListener() {
        super.setListener()
        mBinding.apply {
            titleBar.setLeftButton(null) {
                finish()
            }
        }
        observer.addCallback(object : KeyboardObserver.Callback {
            override fun onKeyboardHeightChanged(height: Int) {
                Log.d(TAG, "onKeyboardHeightChanged height=$height")
                val params = mBinding.etInput.layoutParams as ConstraintLayout.LayoutParams
                params.bottomMargin = height
                mBinding.etInput.layoutParams = params
                // observer.unwatch()
            }
        })
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
}