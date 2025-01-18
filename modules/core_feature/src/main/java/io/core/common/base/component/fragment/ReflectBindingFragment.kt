package io.core.common.base.component.fragment

import android.view.View
import androidx.viewbinding.ViewBinding
import io.core.common.base.component.activity.BaseActivity
import io.core.common.util.ext.ui.inflateBindingWithGeneric

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/7 17:09
 * @description
 * @author Yuan
 */
abstract class ReflectBindingFragment<VB : ViewBinding, A : BaseActivity> : BaseFragment<A>() {

    private var _binding: VB? = null
    val binding: VB get() = _binding!!

    override fun contentViewBind(): View? {
        _binding = inflateBindingWithGeneric(layoutInflater, getFragmentContainer(), false)
        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}