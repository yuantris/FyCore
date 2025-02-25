package com.core.fy.android.main.fragment.code

import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.GsonUtils
import com.core.fy.android.databinding.FragmentKotlinBinding
import com.core.fy.android.function.TestPageActivity
import com.core.fy.android.help.ProgressNotifier
import com.hjq.permissions.Permission
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.helper.JsonUltra
import io.core.common.helper.TaskExecutor
import io.core.common.helper.TimeoutCallback
import io.core.common.helper.TimeoutHandler
import io.core.common.util.extensions.cool.GSON
import io.core.common.util.extensions.cool.createMap
import io.core.common.util.extensions.cool.launchAsync
import io.core.common.util.extensions.cool.mapBuilder
import io.core.common.util.extensions.cool.postDelayUI
import io.core.common.util.extensions.cool.requestPermission
import io.core.common.util.extensions.logD
import io.core.common.util.extensions.logE
import io.core.common.util.extensions.ui.ctx
import io.core.common.util.log.LogPure
import io.core.common.util.tools.AsyncUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit


class KotlinFragment : ReflectBindingFragment<FragmentKotlinBinding, TestPageActivity>() {

    override fun initView() {
        super.initView()

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
        jsonString.logE()

        JsonUltra.parse(jsonString)["library.features[2]"]?.asString().logD()
        JsonUltra.parse(jsonString)["library.books[1].title"]?.asString().logD()
        val list: List<String>? =
            JsonUltra.parse(jsonString)["library.features"]?.asList { it.asString() }
        GSON.toJson(list).logE()


        val parse = JsonUltra.parse("{\"key\": \"{\\\"nested\\\": 1234}\"}")
        parse["key.nested"]?.asInt().logD()

        JsonUltra.parse("{\"name\":\"张三\",\"age\":18}")["name"]?.asString()?.logD()

        ctx.requestPermission(Permission.MANAGE_EXTERNAL_STORAGE) {
            LogPure.v {
                "MANAGE_EXTERNAL_STORAGE permission granted"
            }
        }

        val timeoutHandler = TimeoutHandler(3000, object : TimeoutCallback {
            override fun onTimeout() {
                LogPure.e {
                    "onTimeout"
                }
            }
        })
        val fixedRate = AsyncUtils.scheduleAtFixedRate({
            LogPure.w {
                "scheduleAtFixedRate"
            }
            timeoutHandler.onProgress()
        }, 0, 2000, TimeUnit.MILLISECONDS)
        postDelayUI(16000) {
            fixedRate.cancel(true)
        }
    }

    override fun initData() {
        super.initData()
        // 注册监听器
        ProgressNotifier.register { id, progress ->
            LogPure.i { "id:$id\nprogress: $progress%" }
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
    }
}