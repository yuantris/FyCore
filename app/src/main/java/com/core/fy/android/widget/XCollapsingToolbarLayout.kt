package com.core.fy.android.widget

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.appbar.CollapsingToolbarLayout

class XCollapsingToolbarLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    CollapsingToolbarLayout(context, attrs, defStyleAttr) {

    /** 渐变监听 */
    private var listener: OnScrimsListener? = null

    /** 当前渐变状�?*/
    private var scrimsShownStatus: Boolean = false

    override fun setScrimsShown(shown: Boolean, animate: Boolean) {
        super.setScrimsShown(shown, true)
        // 判断渐变状态是否改变了
        if (scrimsShownStatus == shown) {
            return
        }
        // 如果是就记录并且回调监听�?
        scrimsShownStatus = shown
        listener?.onScrimsStateChange(this, scrimsShownStatus)
    }

    /**
     * 获取当前的渐变状�?
     */
    fun isScrimsShown(): Boolean {
        return scrimsShownStatus
    }

    /**
     * 设置CollapsingToolbarLayout渐变监听
     */
    fun setOnScrimsListener(listener: OnScrimsListener?) {
        this.listener = listener
    }

    /**
     * CollapsingToolbarLayout渐变监听�?
     */
    interface OnScrimsListener {

        /**
         * 渐变状态变�?
         *
         * @param shown         渐变开�?
         */
        fun onScrimsStateChange(layout: XCollapsingToolbarLayout?, shown: Boolean)
    }
}