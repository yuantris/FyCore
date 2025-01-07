package com.core.libraries.base.dialog

import android.animation.ObjectAnimator
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.core.libraries.R
import com.core.libraries.base.ext.inflateWithGeneric

/**
 * 愿你余生所学，皆是兴趣使然，而非生活所迫。
 * @Author      : szl
 * @Date        : on 2023-12-22.
 * @Description :
 */
abstract class BaseReflectBindingDialog<VB : ViewBinding>(
    context: Context,
    themeResId: Int = 0,
    private var isOutSide: Boolean = true,
    var gravity: Int = Gravity.CENTER
) : Dialog(context, themeResId) {

    lateinit var binding: VB
    private var animator: ObjectAnimator? = null
    private var isReverse = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = inflateWithGeneric(this, layoutInflater)
        setContentView(binding.root)
        // 按空白处不能取消动画
        setCanceledOnTouchOutside(isOutSide)
        // 按返回键不消失
        setCancelable(false)
        window!!.setBackgroundDrawableResource(R.color.transparent)
        window?.setGravity(gravity)
        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        // window?.setWindowAnimations(R.style.dialogWindowAnim)
        initView()
    }


    protected open fun initView() {}
}