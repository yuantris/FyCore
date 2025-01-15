package io.core.common.base.component.activity

import android.view.View
import androidx.viewbinding.ViewBinding
import io.core.common.util.ext.ui.inflateBindingWithGeneric

abstract class ReflectBindingActivity<VB : ViewBinding> : BaseActivity() {
    lateinit var binding: VB

    override fun contentViewBind(): View? {
        binding = inflateBindingWithGeneric(layoutInflater)
        return binding.root
    }
}