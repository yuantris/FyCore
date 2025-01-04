package com.core.libraries.base.activity

import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import androidx.viewbinding.ViewBinding
import com.blankj.utilcode.util.KeyboardUtils
import com.core.libraries.base.ext.inflateBindingWithGeneric
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
 * 2025/1/4 11:54
 * @description
 * @author Yuan
 */
abstract class BaseInputActivity<VB : ViewBinding> : BaseActivity(), KeyboardObserver.Callback {
    private val observer by lazy {
        KeyboardObserver.create(
            this,
            showDebug = isShowKeyboardDebug()
        )
    }
    lateinit var mBinding: VB

    override fun contentViewBind(): View? {
        mBinding = inflateBindingWithGeneric(layoutInflater)
        return mBinding.root
    }

    open fun isShowKeyboardDebug(): Boolean {
        return false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        observer.addCallback(this)
        observer.watch()


    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (isShouldHideKeyboard(v, ev)) {
                KeyboardUtils.hideSoftInput(this)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun isShouldHideKeyboard(v: View?, event: MotionEvent): Boolean {
        if ((v is EditText)) {
            val l = intArrayOf(0, 0)
            v.getLocationOnScreen(l)
            val left = l[0]
            val top = l[1]
            val bottom = top + v.getHeight()
            val right = left + v.getWidth()
            return !(event.rawX > left && event.rawX < right && event.rawY > top && event.rawY < bottom)
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        observer.unwatch()
    }
}