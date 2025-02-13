package com.core.fy.android.main.fragment

import com.core.fy.android.MainActivity
import com.core.fy.android.databinding.FragmentBlankBinding
import com.core.fy.android.databinding.FragmentSetBinding
import com.core.fy.android.function.TestCodePageActivity
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.util.ext.ui.adaptStatusBarToView
import io.core.common.util.ext.ui.appVersionName
import io.core.common.util.ext.ui.onClick
import io.core.common.util.ext.ui.setDebouncedClickListener
import io.core.common.util.ext.ui.startActivity

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/13 9:01
 * @description
 * @author Yuan
 */
class SetFragment : ReflectBindingFragment<FragmentSetBinding, MainActivity>() {

    override fun initView() {
        super.initView()
        with(binding) {
            version.setLeftText("版本")
            version.setRightText(context?.appVersionName)

            testCode.setDebouncedClickListener {
                startActivity<TestCodePageActivity>()
            }
        }
    }

    override fun onFragmentResume(first: Boolean) {
        super.onFragmentResume(first)
        getAttachActivity()?.adaptStatusBarToView(
            rootView = requireActivity().window.decorView,
            targetView = binding.setRoot
        )
    }
}