package com.core.fy.android.main.fragment

import com.core.fy.android.MainActivity
import com.core.fy.android.databinding.FragmentSetBinding
import com.core.fy.android.function.TestPageActivity
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.util.extensions.cool.currentTimeFormat
import io.core.common.util.extensions.currentTimeMillis
import io.core.common.util.extensions.ui.adaptStatusBarToView
import io.core.common.util.extensions.ui.appVersionName
import io.core.common.util.extensions.ui.onDebouncedClick
import io.core.common.util.extensions.ui.startActivity
import io.core.constant.DateFormat

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

            testCode.onDebouncedClick {
                startActivity<TestPageActivity>()
            }
            crash.onDebouncedClick {
                throw RuntimeException("Crash ${currentTimeMillis.currentTimeFormat(DateFormat.yyyyMMddHHmmssSSS)}")
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