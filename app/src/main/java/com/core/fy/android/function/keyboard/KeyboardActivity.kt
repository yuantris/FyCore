package com.core.fy.android.function.keyboard

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.constraintlayout.widget.ConstraintLayout
import com.core.fy.android.databinding.ActivityKeyboardBinding
import io.core.common.base.component.activity.BaseInputActivity
import io.core.common.helper.StatusBarManager
import io.core.common.helper.track.AppTrackV2
import io.core.common.util.DiveGestureLine
import io.core.common.util.extensions.cool.launchAsync
import io.core.common.util.extensions.ui.onDebouncedClick
import io.core.common.util.log.d
import io.core.common.util.tools.KeyboardTools
import kotlinx.coroutines.delay

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
    private var isStatusBarLight = true

    override fun isShowKeyboardDebug(): Boolean {
        return true
    }

    override fun initial(savedInstanceState: Bundle?) {
        xiaomiAdapt = {
            DiveGestureLine.adaptXiaomi(window, Color.parseColor("#f4f4f4"))
        }
        super.initial(savedInstanceState)
    }

    override fun setListener() {
        super.setListener()
        binding.apply {
            show.onDebouncedClick {
                KeyboardTools.showSoftInput()
            }

            hide.onDebouncedClick {
                KeyboardTools.hideSoftInput(window)
            }
            fragment.onDebouncedClick {
                AppTrackV2.getTopFragment()?.javaClass?.simpleName?.d() ?: run { "null".d() }
            }
            statusBar.onDebouncedClick {
                isStatusBarLight = !isStatusBarLight
                val light = StatusBarManager.with(this@KeyboardActivity).isStatusBarLight()
                StatusBarManager.with(this@KeyboardActivity)
                    .updateStatusBarManually(!light)
            }
        }
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