package com.core.fy.android.main.fragment

import com.core.fy.android.MainActivity
import com.core.fy.android.databinding.FragmentSetBinding
import com.core.fy.android.function.TestPageActivity
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.util.ShareAir
import io.core.common.util.extensions.cool.coolThread
import io.core.common.util.extensions.cool.timeFormat
import io.core.common.util.extensions.currentTimeMillis
import io.core.common.util.extensions.ui.adaptStatusBarToView
import io.core.common.util.extensions.ui.appVersionName
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.extensions.ui.onDebouncedClick
import io.core.common.util.extensions.ui.startActivity
import io.core.common.util.log.LogCat
import io.core.common.util.tools.ThreadUltra
import io.core.common.util.tools.androidApiVersion
import io.core.common.util.tools.androidVersion
import io.core.common.util.tools.buildMultiLine
import io.core.constant.TimeFormat
import androidx.core.net.toUri
import com.core.fy.android.constants.PreferKey
import io.core.common.util.extensions.logD
import io.core.common.util.extensions.storageManager
import io.core.engine.storage.getWithAnnotation
import io.core.engine.storage.storage

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

        val withAnnotation = storage.getWithAnnotation<Int>(PreferKey.SP_TEST)
        LogCat.d("测试一下工厂默认值 结果为：$withAnnotation")

        with(binding) {
            version.setLeftText("版本")
            version.setRightText(context?.appVersionName)
            version.onClick {
//                ctx.showXpLoading {
//                    content = ctx.appVersionName
//                }
//                showDxNotification {
//                    content = ctx.appVersionName
//                }

                ShareAir.share {
                    text("分享到")
                }
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

    override fun initData() {
        super.initData()


    }

    override fun onFragmentResume(first: Boolean) {
        super.onFragmentResume(first)
        getAttachActivity()?.adaptStatusBarToView(
            rootView = requireActivity().window.decorView,
            targetView = binding.setRoot
        )

        coolThread<Any> {
            background {
                "1"
            }

            success {
                LogCat.d(this)
            }
        }

        ThreadUltra.execute(object : ThreadUltra.Task<Any>() {
            override fun doInBackground(): Any {
                TODO("Not yet implemented")
            }

            override fun onSuccess(result: Any) {
                TODO("Not yet implemented")
            }

        })

    }
}