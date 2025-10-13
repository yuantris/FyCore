package com.core.fy.android.main.fragment.code

import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.GsonUtils
import com.core.fy.android.databinding.FragmentKotlinBinding
import com.core.fy.android.function.TestPageActivity
import com.core.fy.android.help.ProgressNotifier
import com.hjq.permissions.Permission
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.helper.CallCoordinator
import io.core.common.helper.JsonUltra
import io.core.common.helper.TimeoutCallback
import io.core.common.helper.TimeoutHandler
import io.core.common.helper.coroutine.info.GlobalCoroutine
import io.core.common.helper.coroutine.info.LoopEngine
import io.core.common.helper.jetpack.SingleLiveData
import io.core.common.helper.track.AppTrackV2
import io.core.common.helper.track.FragmentVisibilityDetectorV2
import io.core.common.util.Preferences
import io.core.common.util.Toaster
import io.core.common.util.concurrent.Concurrency
import io.core.common.util.concurrent.TaskExecutor
import io.core.common.util.extensions.cool.GSON
import io.core.common.util.extensions.cool.PathType
import io.core.common.util.extensions.cool.createMap
import io.core.common.util.extensions.cool.getSettingsPathV2
import io.core.common.util.extensions.cool.joinPath
import io.core.common.util.extensions.cool.launch
import io.core.common.util.extensions.cool.mapBuilder
import io.core.common.util.extensions.cool.runDelayedMain
import io.core.common.util.extensions.cool.withMain
import io.core.common.util.extensions.logD
import io.core.common.util.extensions.simpleName
import io.core.common.util.extensions.ui.ctx
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.log.LogCat
import io.core.common.util.log.LogPure
import io.core.common.util.log.bury.AppLog
import io.core.common.util.tools.FileTools
import io.core.engine.storage.storage
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class KotlinFragment : ReflectBindingFragment<FragmentKotlinBinding, TestPageActivity>() {

    private var timeoutHandler: TimeoutHandler? = null
    private val _data = SingleLiveData<String>()
    private var _count = 0

    override fun initView() {
        super.initView()

//        Preferences.getValue(T.TTT,true).logD()

        lifecycleScope.launch {
            val d1 = async {
                CallCoordinator.guardedSingleFlight("KET") {
                    delay(300) // 模拟耗时
                    LogPure.i { "我开始执行" }
                    "OK"
                }
            }
            val d2 = async {
                CallCoordinator.guardedSingleFlight("KET") {
                    delay(300)
                    LogPure.i { "我开始执行" }
                    "OK"
                }
            }
            val d3 = async {
                CallCoordinator.guardedSingleFlight("KET") {
                    delay(300)
                    LogPure.i { "我开始执行" }
                    "OK"
                }
            }
            awaitAll(d1, d2, d3)
        }


        FragmentVisibilityDetectorV2.attachToFragment(this) { isVisible ->

        }

        val fragments = AppTrackV2.getFragmentsByActivity(requireActivity())
        fragments.forEach { LogPure.d("AppTrackV4", "${it::class.simpleName}") }

        _data.observe(this) {
            it.logD()
        }

        binding.tv.onClick {
            _data.setValue("${System.currentTimeMillis()}")
        }

        val createMap = createMap<String, Any>()
        createMap["key"] = "value"
        val builder = mapBuilder<String, Int>()
        builder.put("key", 1)
        val map = builder.build()

        // 构建复杂 JSON
        val jsonString = JsonUltra.build {
            "library" obj {
                "name" with "Central Library"
                "books" array {
                    plusAssign(mapOf("title" to "Kotlin Coroutines", "year" to 2023))
                    plusAssign(mapOf("title" to "Android Development", "year" to 2024))
                }
                "features" with listOf("wifi", "cafe", "24h")
            }
            "author" with "yuan"
        }
        AppLog.debug(simpleName(), "jsonString")
        AppLog.info(simpleName(), jsonString)

        JsonUltra.parse(jsonString).getAllPaths().forEach { LogPure.i { it } }
        AppLog.debug(
            simpleName(),
            JsonUltra.parse(jsonString)["library.features[2]"]?.asString() ?: ""
        )
        AppLog.debug(
            simpleName(),
            JsonUltra.parse(jsonString)["library.books[1].title"]?.asString() ?: ""
        )
        val list: List<String>? =
            JsonUltra.parse(jsonString)["library.features"]?.asList { it.asString() }
        AppLog.error(simpleName(), GSON.toJson(list))


        val parse = JsonUltra.parse("{\"key\": \"{\\\"nested\\\": 1234}\"}")
        parse["key.nested"]?.asInt().logD()

        JsonUltra.parse("{\"name\":\"yuantris@qq.com\",\"@aliyun.com\":18}")["@aliyun.com"]?.asString()
            ?.logD()

        val ultra =
            JsonUltra.parse("{\"code\":1,\"message\":\"success\",\"data\":{\"邮政平邮\":\"youzhengbk\",\"申通快递\":\"shentong\",\"圆通快递\":\"yuantong\",\"中通快递\":\"zhongtong\",\"极兔速递\":\"jtexpress\",\"韵达快递\":\"yunda\",\"德邦快递\":\"debangkuaidi\",\"顺丰快递\":\"shunfeng\"}}")

        ultra["data"]?.asMap()?.let { m ->
            m["邮政平邮"]?.logD()
        }
        ultra.getNotNull("data").asString().logD()

        timeoutHandler = TimeoutHandler(
            timeoutMillis = 3000,
            maxRetries = 3,
            logger = { message -> LogPure.d { "[TimeoutHandler] $message" } },
            timeoutCallback = object : TimeoutCallback {
                override fun onTimeout(reason: TimeoutHandler.TimeoutReason) {
                    when (reason) {
                        TimeoutHandler.TimeoutReason.NORMAL -> LogPure.e { "正常超时" }
                        TimeoutHandler.TimeoutReason.RETRY_LIMIT -> LogPure.e { "达到重试限制" }
                        TimeoutHandler.TimeoutReason.MANUAL_TRIGGER -> LogPure.e { "手动触发" }
                    }
                }
            }
        )
        var fixedRateCount = 0
        val fixedRate = Concurrency.scheduleAtFixedRate({
            fixedRateCount++
            timeoutHandler?.resetTimeout()
        }, 0, 2000, TimeUnit.MILLISECONDS)
        runDelayedMain(16000) {
            fixedRate.cancel(true)
        }


        launch {
            val listFiles =
                FileTools.listFiles(ctx.getSettingsPathV2(PathType.EXTERNAL_CACHE, "mmkv_fy"))
            listFiles.forEach {
                LogPure.i {
                    "file:${it.absolutePath}"
                }
            }
        }

        AppLog.getLogFiles().forEach {
            // LogCat.i(it.readText())
        }

        joinPath("a", "b", "c").logD()
    }

    override fun initData() {
        super.initData()
        // 注册监听器
        ProgressNotifier.register { id, progress ->
            LogPure.i { "id:$id\nprogress: $progress%" }
        }

        val engine = LoopEngine.Builder()
            .interval(1_000)
            .onStart { Toaster.show("LoopEngine start") }
            .onStop { Toaster.show("LoopEngine stop") }
            .task {
                _count++
                withMain {
                    binding.tv.text = "count:$_count"
                }
            }
            .build()

        with(binding) {
            btnStart.onClick {
                engine.start()
            }
            btnPause.onClick {
                engine.pause()
            }
            btnResume.onClick {
                engine.resume()
            }
            btnStop.onClick {
                engine.stop()

                LogPure.e { "isRunning:${engine.isRunning()}" }
            }
        }
    }


    override fun onFragmentResume(first: Boolean) {
        super.onFragmentResume(first)

        GlobalCoroutine.launch {
            val totalSteps = 24
            val taskId = ProgressNotifier.startTask(totalSteps)

            repeat(totalSteps) {
                ProgressNotifier.incrementProgress(taskId)
                delay(1000)
            }

            ProgressNotifier.complete(taskId)
        }

        val tasks = listOf<suspend () -> String>(
            { /* 扫描图片实现 */ "img1" },
            { /* 扫描视频实现 */
                delay(4000)
                "video1"
            },
        )
        launch {
            TaskExecutor.get().execute(
                tasks,
                onComplete = {
                    LogPure.i {
                        "onComplete:${GsonUtils.toJson(it)}"
                    }
                },
                onEachComplete = { result, index ->
                    LogPure.d { "result:$result,index:$index" }
                },
            )
        }

        storage.getAllKeys().forEach {
            LogCat.e(it)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timeoutHandler?.cancel()
    }
}
