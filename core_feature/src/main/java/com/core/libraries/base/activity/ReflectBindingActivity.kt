package com.core.libraries.base.activity

import android.view.View
import androidx.viewbinding.ViewBinding
import com.core.libraries.base.ext.inflateBindingWithGeneric

abstract class ReflectBindingActivity<VB : ViewBinding> : BaseActivity() {
    lateinit var binding: VB

    override fun contentViewBind(): View? {
        binding = inflateBindingWithGeneric(layoutInflater)
        return binding.root
    }
}