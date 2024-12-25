package com.core.libraries.base.activity

import android.view.View
import androidx.viewbinding.ViewBinding
import com.core.libraries.base.ext.inflateBindingWithGeneric

abstract class ReflectBindingActivity<VB : ViewBinding> : BaseActivity() {
    lateinit var mBinding: VB

    override fun contentViewBind(): View? {
        mBinding = inflateBindingWithGeneric(layoutInflater)
        return mBinding.root
    }
}