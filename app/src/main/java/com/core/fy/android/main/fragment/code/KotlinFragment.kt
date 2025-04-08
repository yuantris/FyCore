package com.core.fy.android.main.fragment.code

import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.GsonUtils
import com.core.fy.android.databinding.FragmentKotlinBinding
import com.core.fy.android.function.TestPageActivity
import com.core.fy.android.help.ProgressNotifier
import com.hjq.permissions.Permission
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.helper.JsonUltra
import io.core.common.util.concurrent.TaskExecutor
import io.core.common.helper.TimeoutCallback
import io.core.common.helper.TimeoutHandler
import io.core.common.helper.jetpack.SingleLiveData
import io.core.common.util.concurrent.Concurrency
import io.core.common.util.extensions.cool.GSON
import io.core.common.util.extensions.cool.PathType
import io.core.common.util.extensions.cool.createMap
import io.core.common.util.extensions.cool.getSettingsPathV2
import io.core.common.util.extensions.cool.joinPath
import io.core.common.util.extensions.cool.launchAsync
import io.core.common.util.extensions.cool.mapBuilder
import io.core.common.util.extensions.cool.requestPermission
import io.core.common.util.extensions.cool.runDelayedMain
import io.core.common.util.extensions.cool.runMain
import io.core.common.util.extensions.logD
import io.core.common.util.extensions.simpleName
import io.core.common.util.extensions.ui.ctx
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.log.LogCat
import io.core.common.util.log.LogPure
import io.core.common.util.log.bury.AppLog
import io.core.common.util.tools.FileTools
import io.core.engine.storage.storage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.collections.List
import kotlin.collections.forEach
import kotlin.collections.listOf
import kotlin.collections.mapOf
import kotlin.collections.set

class KotlinFragment : ReflectBindingFragment<FragmentKotlinBinding, TestPageActivity>() {

    private val _data = SingleLiveData<String>()

    override fun initView() {
        super.initView()

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

        JsonUltra.parse("{\"name\":\"yuantris@qq.com\",\"@aliyun.com\":18}")["@aliyun.com"]?.asString()?.logD()

        val ultra =
            JsonUltra.parse("{\"code\":1,\"message\":\"success\",\"data\":{\"邮政平邮\":\"youzhengbk\",\"申通快递\":\"shentong\",\"圆通快递\":\"yuantong\",\"中通快递\":\"zhongtong\",\"极兔速递\":\"jtexpress\",\"韵达快递\":\"yunda\",\"德邦快递\":\"debangkuaidi\",\"顺丰快递\":\"shunfeng\"}}")

        ultra["data"]?.asMap()?.let { m ->
            m["邮政平邮"]?.logD()
        }
        ultra.getNotNull("data").asString().logD()

        ctx.requestPermission(Permission.MANAGE_EXTERNAL_STORAGE) {
//            LogPure.v {
//                "MANAGE_EXTERNAL_STORAGE permission granted"
//            }
        }

        val timeoutHandler = TimeoutHandler(3000, object : TimeoutCallback {
            override fun onTimeout() {
//                LogPure.e {
//                    "onTimeout"
//                }
            }
        })
        val fixedRate = Concurrency.scheduleAtFixedRate({
//            LogPure.w {
//                "scheduleAtFixedRate"
//            }
            timeoutHandler.onProgress()
        }, 0, 2000, TimeUnit.MILLISECONDS)
        runDelayedMain(16000) {
            fixedRate.cancel(true)
        }


        launchAsync {
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
        runMain {

        }

    }

    override fun initData() {
        super.initData()
        // 注册监听器
        ProgressNotifier.register { id, progress ->
            // LogPure.i { "id:$id\nprogress: $progress%" }
        }

    }


    override fun onFragmentResume(first: Boolean) {
        super.onFragmentResume(first)

        lifecycleScope.launch {
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
        launchAsync {
            TaskExecutor.get().executeConcurrent(
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
}
