package com.core.fy.android.function

import android.os.Bundle
import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import com.core.fy.android.databinding.ActivityKeyboardBinding
import com.core.libraries.Android
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.base.ext.logD
import com.core.libraries.base.ext.logV
import com.core.libraries.helper.ValidHelper
import com.core.libraries.other.keyboard.KeyboardObserver
import com.core.libraries.util.LogUtils.logD
import com.core.libraries.view.TitleBar

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
    private val observer by lazy { KeyboardObserver.create(this, showDebug = Android.debug) }
    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        observer.watch()

        "ValidHelper start".logV()
        ValidHelper.create().asString()
            .build("1") {
                if (it) {
                    "isFile".logD()
                } else {
                    "isNotFile".logD()
                }
            }
    }

    override fun setListener() {
        super.setListener()
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