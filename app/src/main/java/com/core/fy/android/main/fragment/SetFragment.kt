package com.core.fy.android.main.fragment

import android.content.res.AssetManager
import com.core.fy.android.MainActivity
import com.core.fy.android.databinding.FragmentSetBinding
import com.core.fy.android.function.TestPageActivity
import com.core.fy.android.util.showXpConfirm
import com.core.fy.android.util.showXpLoading
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.helper.AppLifecycleTracker
import io.core.common.helper.JsonUltra
import io.core.common.util.extensions.cool.coolThread
import io.core.common.util.extensions.cool.jsonToMap
import io.core.common.util.extensions.cool.logPrint
import io.core.common.util.extensions.cool.timeFormat
import io.core.common.util.extensions.currentTimeMillis
import io.core.common.util.extensions.logD
import io.core.common.util.extensions.logE
import io.core.common.util.extensions.ui.adaptStatusBarToView
import io.core.common.util.extensions.ui.appVersionName
import io.core.common.util.extensions.ui.ctx
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.extensions.ui.onDebouncedClick
import io.core.common.util.extensions.ui.startActivity
import io.core.common.util.extensions.ui.toast
import io.core.common.util.log.LogCat
import io.core.common.util.log.LogPure
import io.core.common.util.tools.FileUtils
import io.core.common.util.tools.ThreadUltra
import io.core.common.util.tools.androidApiVersion
import io.core.common.util.tools.androidVersion
import io.core.common.util.tools.buildMultiLine
import io.core.constant.TimeFormat
import kotlinx.serialization.json.JsonNull.content

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
                ctx.showXpLoading {
                    content = ctx.appVersionName
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
        val json =
            "{\"Ids\":{\"11\":\"212-226\",\"12\":\"227-246\"},\"imageUrlPrefix\":\"https://zycdn.ss.bscstorage.com/wallpaper/\"}"

        val format = JsonUltra.format(json)
        format.logD()

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