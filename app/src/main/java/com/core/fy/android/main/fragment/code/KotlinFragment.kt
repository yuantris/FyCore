package com.core.fy.android.main.fragment.code

import com.blankj.utilcode.util.GsonUtils
import com.core.fy.android.databinding.FragmentKotlinBinding
import com.core.fy.android.function.TestPageActivity
import com.hjq.permissions.Permission
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.helper.TaskExecutor
import io.core.common.helper.TimeoutCallback
import io.core.common.helper.TimeoutHandler
import io.core.common.util.extensions.cool.launchAsync
import io.core.common.util.extensions.cool.postDelayUI
import io.core.common.util.extensions.cool.requestPermission
import io.core.common.util.extensions.ui.ctx
import io.core.common.util.log.LogPure
import io.core.common.util.tools.AsyncUtils
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit


class KotlinFragment : ReflectBindingFragment<FragmentKotlinBinding, TestPageActivity>() {

    override fun initView() {
        super.initView()

        ctx.requestPermission(Permission.MANAGE_EXTERNAL_STORAGE) {
            LogPure.i {
                "MANAGE_EXTERNAL_STORAGE permission granted"
            }
        }

        val timeoutHandler = TimeoutHandler(1000, object : TimeoutCallback {
            override fun onTimeout() {
                LogPure.i {
                    "onTimeout"
                }
            }
        })
        val fixedRate = AsyncUtils.scheduleAtFixedRate({
            LogPure.i {
                "scheduleAtFixedRate"
            }
            timeoutHandler.onProgress()
        }, 0, 2000, TimeUnit.MILLISECONDS)
        postDelayUI(6000){
            fixedRate.cancel(true)
        }
    }

    override fun onFragmentResume(first: Boolean) {
        super.onFragmentResume(first)
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
                    LogPure.d("result:$result,index:$index")
                },
            )
        }
    }
}