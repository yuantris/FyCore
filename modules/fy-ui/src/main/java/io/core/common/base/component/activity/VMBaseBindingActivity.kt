package io.core.common.base.component.activity

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.viewbinding.ViewBinding


abstract class VMBaseBindingActivity<VB : ViewBinding, VM : ViewModel> :
    BaseActivity() {

    protected abstract val binding: VB
    protected abstract val vm: VM

    override fun contentViewBind(): View {
        return binding.root
    }
}