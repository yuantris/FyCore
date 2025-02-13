package com.core.fy.android.function

import android.os.Bundle
import android.view.MotionEvent
import com.core.fy.android.databinding.ActivityTestPageBinding
import com.core.fy.android.function.read.page.ContentTextView
import com.core.fy.android.function.read.page.delegate.PageDelegate
import com.core.fy.android.function.read.page.provider.TextPageFactory
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.helper.ConcurrentProcessor
import io.core.common.util.ToastUtil
import io.core.common.util.ext.cool.launchAsync

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/13 16:43
 * @description
 * @author Yuan
 */
class TestPageActivity: ReflectBindingActivity<ActivityTestPageBinding>() {


    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)

        val tasks = listOf<suspend () -> List<String>>(
            { /* 扫描图片实现 */ listOf("img1", "img2") },
            { /* 扫描视频实现 */ listOf("video1") },
            { /* 扫描音频实现 */ listOf("audio1") }
        )
        launchAsync {
            ConcurrentProcessor.get().executeConcurrent(tasks,
                onComplete = {
                    it.size
                })
        }

    }
}