package io.core.common.base.action

import android.view.View
import androidx.annotation.IdRes

interface ClickAction : View.OnClickListener {

    fun <V : View?> findViewById(@IdRes id: Int): V?

    fun setOnClickListener(@IdRes vararg ids: Int) {
        setOnClickListener(this, *ids)
    }

    fun setOnClickListener(listener: View.OnClickListener?, @IdRes vararg ids: Int) {
        for (id: Int in ids) {
            findViewById<View?>(id)?.setOnClickListener(listener)
        }
    }

    fun setOnClickListener(vararg views: View?) {
        setOnClickListener(this, *views)
    }

    fun setOnClickListener(listener: View.OnClickListener?, vararg views: View?) {
        for (view: View? in views) {
            view?.setOnClickListener(listener)
        }
    }

    override fun onClick(view: View) {
        // 默认不实现，让子类实现
    }
}