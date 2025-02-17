package com.core.fy.android.main.fragment

import android.annotation.SuppressLint
import android.widget.Toast
import com.core.fy.android.MainActivity
import com.core.fy.android.R
import com.core.fy.android.constants.AppConst.timeFormat
import com.core.fy.android.constants.EventKey.BATTERY_CHANGED
import com.core.fy.android.constants.EventKey.TIME_CHANGED
import com.core.fy.android.databinding.FragmentBlankBinding
import com.core.fy.android.ui.receiver.TimeBatteryReceiver
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.helper.coroutine.Coroutine
import io.core.common.helper.dialogs.showDialog
import io.core.common.util.MediaScanner
import io.core.appCtx
import io.core.common.util.ext.cool.ConvertUtils
import io.core.common.util.ext.cool.observeEvent
import io.core.common.util.ext.cool.observeEventSticky
import io.core.common.util.ext.ui.addViewToZYLayout
import io.core.common.util.ext.ui.getCompatColor
import io.core.common.util.ext.ui.onClick
import io.core.common.util.log.LogPure
import io.core.common.util.log.logE
import io.core.common.util.tools.ColorUtils
import io.core.common.util.tools.MultimediaUtil
import io.core.common.util.tools.UriUtils
import io.core.common.util.tools.runOnUI
import io.core.widget.view.LoadingView
import io.core.widget.view.RotateLoading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import java.util.Date

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/9 9:07
 * @description
 * @author Yuan
 */
class BlankFragment : ReflectBindingFragment<FragmentBlankBinding, MainActivity>() {

    private val timeBatteryReceiver = TimeBatteryReceiver()

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    override fun initView() {
        super.initView()
        appCtx.registerReceiver(timeBatteryReceiver, timeBatteryReceiver.filter)
        binding.time.onClick {
            showDialog("对话框标题", "这是一个对话框消息。") {
                okButton {
                    Toast.makeText(activity, "点击了确定", Toast.LENGTH_SHORT).show()
                }
                cancelButton {
                    Toast.makeText(activity, "点击了取消", Toast.LENGTH_SHORT).show()
                }
            }
        }


    }

    private var job: Coroutine<*>? = null
    override fun onFragmentResume(first: Boolean) {
        super.onFragmentResume(first)
        val loadingView = LoadingView(requireContext(), 100, getCompatColor(R.color.black))
        val loading = RotateLoading(requireContext())
        addViewToZYLayout(binding.zyLayout, loadingView)
        addViewToZYLayout(binding.zyLayout1, loading)
        // 在协程作用域中启动
        job = Coroutine.async(
            scope = CoroutineScope(Dispatchers.Main), // 指定作用域，默认为 MainScope()
            context = Dispatchers.Default,          // 指定执行上下文，默认为 Dispatchers.IO
            start = CoroutineStart.LAZY,            // 指定启动选项，默认为 CoroutineStart.DEFAULT
            executeContext = Dispatchers.Main,      // 指定回调执行上下文，默认为 Dispatchers.Main
        ) {
            while (isActive) { // 循环条件
                runOnUI {
                    loadingView.setColor(ColorUtils.getRandomColor())
                    loading.loadingColor = ColorUtils.getRandomColor()
                }
                delay(3000)    // 非阻塞式延迟
            }
        }
        job?.start()


        XXPermissions.with(this)
            .permission(Permission.MANAGE_EXTERNAL_STORAGE)
            .request { _, _ ->
                Coroutine.async(
                    scope = CoroutineScope(Dispatchers.Main),
                    executeContext = Dispatchers.IO
                ) {
                    val files = MediaScanner.queryFiles(
                        types = setOf(
                            MediaScanner.FileType.MP4,
                        ),
                        addFilter = {
                            it.size > 1024 * 1024
                        }
                    )
                    files
                }.onSuccess { result ->
                    result.forEach {
                        LogPure.logI("文件：${it.path}", "FileScanHelper_")
                    }
                    val file = File(result[0].path)
                    val uri = UriUtils.file2Uri(File(file.path))
                    "数量：${result.size} 第一个文件：${file.absolutePath} uri：${uri}".logE()
                    MultimediaUtil.getDuration(file.absolutePath)?.logE()
                    ConvertUtils.formatFileSize(result[0].size).logE()
                }

            }

    }

    override fun initData() {
        super.initData()
        observeEventSticky<String>(TIME_CHANGED) {
            binding.time.text = timeFormat.format(Date(System.currentTimeMillis()))
        }
        observeEvent<Int>(BATTERY_CHANGED) {
            binding.battery.text = "当前电量：$it%"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job?.cancel()
    }
}