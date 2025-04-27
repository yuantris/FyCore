package com.core.fy.android.main.fragment

import com.core.fy.android.MainActivity
import com.core.fy.android.constants.PreferKey
import com.core.fy.android.databinding.FragmentSetBinding
import com.core.fy.android.function.TestPageActivity
import com.core.fy.android.function.read.ReadBookActivity
import com.core.fy.android.help.HighLightHelper
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.helper.JsonUltra
import io.core.common.helper.net.NetworkMonitor
import io.core.common.helper.net.NetworkState
import io.core.common.helper.net.awaitNetwork
import io.core.common.helper.track.TimeTracker
import io.core.common.helper.valid.ValidGT
import io.core.common.helper.valid.excludeHiddenFiles
import io.core.common.helper.valid.hasExtension
import io.core.common.helper.valid.maxSize
import io.core.common.util.extensions.cool.coolThread
import io.core.common.util.extensions.cool.launchAsync
import io.core.common.util.extensions.cool.timeFormat
import io.core.common.util.extensions.currentTimeMillis
import io.core.common.util.extensions.logD
import io.core.common.util.extensions.logE
import io.core.common.util.extensions.logI
import io.core.common.util.extensions.ui.appVersionName
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.extensions.ui.onDebouncedClick
import io.core.common.util.extensions.ui.postDelayed
import io.core.common.util.extensions.ui.startActivity
import io.core.common.util.log.LogCat
import io.core.common.util.log.LogPure
import io.core.common.util.tools.ThreadUltra
import io.core.common.util.tools.androidApiVersion
import io.core.common.util.tools.androidVersion
import io.core.common.util.tools.buildMultiLine
import io.core.constant.DeviceOS
import io.core.constant.FileSize
import io.core.constant.FileSize.TimeUnitStyle.English
import io.core.constant.FileSize.TimeUnitStyle.UnitCase
import io.core.constant.TimeFormat
import io.core.engine.storage.getWithAnnotation
import io.core.engine.storage.put
import io.core.engine.storage.storage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.function.Predicate

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
        val monitor = NetworkMonitor.get()
        launchAsync {
            monitor.networkState.collect { state ->
                when (state) {
                    is NetworkState.Connected -> {
                        if (state.isWifi) {
                            // WiFi连接处理
                            "WiFi连接".logI()
                        } else if (state.isCellular) {
                            // 移动数据连接处理
                            "移动数据连接".logI()
                        }
                    }
                    NetworkState.Disconnected -> {
                        // 断开连接处理
                        "断开连接".logI()
                        loadNetworkData()
                    }
                }
            }
        }

        val fileResult = ValidGT.forFile()
            .excludeHiddenFiles()
            .hasExtension(".jpg", ".png")
            .maxSize(1024 * 1024 * 5)
            .build(File("avatar.jpg"))

        val complexCondition = Predicate<File> { it.name.startsWith("temp") }
            .and { it.length() > 1024 }

        ValidGT.forFile()
            .addCondition(complexCondition, "文件名必须以temp开头且大于1KB")

        val ageValidator = ValidGT.create<Int>()
            .addCondition({ it in 18..60 }, "年龄必须在18-60岁之间")
            .build(11)

        JsonUltra.parse("{\"a\":1}").use {
            it["a"]?.asString().logD()
        }

        val size = 37230L
        val duration = 3723000L
        FileSize.formatDuration(
            duration,
            compact = true,
            unitStyle = English.Long(UnitCase.CAPITAL)
        ).logE()
        FileSize.format(size).logI()

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
//                showDxCustom {
//                    layoutResId = R.layout.layout_skeleton
//                    onBindView = { _, _ ->
//
//                    }
//                }

//                AppLauncher.launchAppByPackage("com.taobao.taobao")
//                val file = File("/sdcard/Documents/error.txt")
//                AppLauncher.launchByUri(file.getUri().toString())
//                AppLauncher.launchSpecificActivity(
//                    "com.taobao.taobao",
//                    "com.taobao.browser.BrowserActivity",
//                    "https://item.taobao.com/item.htm?id=820919984621".toUri()
//                ) {
//                    putBoolean("is_refund_order_url", false)
//                    putBoolean("alloweWebViewHistoryBack", true)
//                }

                printTimeStats()

            }

            systemInfo.text = buildMultiLine {
                append("Android $androidVersion")
                append("Api $androidApiVersion")
                appendDivider(12, "🉐")
                append("${DeviceOS.brand}")
                append(DeviceOS.romInfo.verDesc)
                append("VerName: ${DeviceOS.romInfo.verName}")
                append("VerCode: ${DeviceOS.romInfo.verCode}")

            }

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

    suspend fun loadNetworkData() {
        val hasNetwork = awaitNetwork { it.isConnected }
        if (hasNetwork) {
            // 执行网络请求
            "执行网络请求".logE()
        }
    }

    // 获取统计数据
    private fun printTimeStats() {
        val stats = TimeTracker.getStats(ReadBookActivity::class.java)
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        LogPure.d {
            """
            |页面停留统计:
            |总时长: ${stats.totalDuration / 1000}秒
            |今日: ${stats.todayDuration / 1000}秒
            |本周: ${stats.weekDuration / 1000}秒
            |本月: ${stats.monthDuration / 1000}秒
            |首次访问: ${formatter.format(Date(stats.firstVisitTime))}
            |最后访问: ${formatter.format(Date(stats.lastVisitTime))}
            |访问次数: ${stats.visitCount}
        """.trimMargin()
        }
    }

    override fun initData() {
        super.initData()

        val annotation = storage.getWithAnnotation<Boolean>(PreferKey.SET_HIGHLIGHT)
        if (annotation) {
            binding.crash.postDelayed(1000) {
                storage.put(PreferKey.SET_HIGHLIGHT, false)
                HighLightHelper.showQuickFolderGuide(
                    requireActivity(),
                    binding.version,
                    binding.crash
                )
            }
        }

    }

    override fun onFragmentResume(first: Boolean) {
        super.onFragmentResume(first)
//        getAttachActivity()?.adaptStatusBarToView(
//            rootView = requireActivity().window.decorView,
//            targetView = binding.setRoot
//        )

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