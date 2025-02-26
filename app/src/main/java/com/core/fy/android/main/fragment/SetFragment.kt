package com.core.fy.android.main.fragment

import com.core.fy.android.MainActivity
import com.core.fy.android.databinding.FragmentSetBinding
import com.core.fy.android.function.TestPageActivity
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.util.extensions.cool.logPrint
import io.core.common.util.extensions.cool.timeFormat
import io.core.common.util.extensions.currentTimeMillis
import io.core.common.util.extensions.ui.adaptStatusBarToView
import io.core.common.util.extensions.ui.appVersionName
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.extensions.ui.onDebouncedClick
import io.core.common.util.extensions.ui.startActivity
import io.core.common.util.tools.ThreadUltra
import io.core.common.util.tools.androidApiVersion
import io.core.common.util.tools.androidVersion
import io.core.common.util.tools.buildMultiLine
import io.core.constant.TimeFormat

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
            version.onClick {

            }

            val info = buildMultiLine {
                append("Android $androidVersion")
                append("Api $androidApiVersion")
            }
            systemInfo.text = info

            testCode.onDebouncedClick {
                startActivity<TestPageActivity>()
            }
            crash.onDebouncedClick {
                require(false) {
                    "Crash ${currentTimeMillis.timeFormat(TimeFormat.LOG_TIMESTAMP)}"
                }
            }
        }
    }

    override fun onFragmentResume(first: Boolean) {
        super.onFragmentResume(first)
        getAttachActivity()?.adaptStatusBarToView(
            rootView = requireActivity().window.decorView,
            targetView = binding.setRoot
        )


        ThreadUltra.execute(object : ThreadUltra.Task<Any>() {
            override fun doInBackground(): Any {
                TODO("Not yet implemented")
            }

            override fun onSuccess(result: Any) {
                TODO("Not yet implemented")
            }

            override fun onFail(errorType: ThreadUltra.ErrorType, ex: Throwable) {
                ex.logPrint()
            }

        })
    }
}