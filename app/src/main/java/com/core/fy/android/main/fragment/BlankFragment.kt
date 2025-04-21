package com.core.fy.android.main.fragment

import android.annotation.SuppressLint
import android.os.Build
import androidx.lifecycle.lifecycleScope
import com.core.fy.android.MainActivity
import com.core.fy.android.R
import com.core.fy.android.constants.AppConst.timeFormat
import com.core.fy.android.constants.EventKey.BATTERY_CHANGED
import com.core.fy.android.constants.EventKey.TIME_CHANGED
import com.core.fy.android.databinding.FragmentBlankBinding
import com.core.fy.android.ui.receiver.TimeBatteryReceiver
import io.core.appCtx
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.helper.coroutine.Coroutine
import io.core.common.helper.dialogs.showDialog
import io.core.common.util.Toaster
import io.core.common.util.extensions.cool.observeEvent
import io.core.common.util.extensions.cool.observeEventSticky
import io.core.common.util.extensions.cool.runMain
import io.core.common.util.extensions.ui.addViewToZYLayout
import io.core.common.util.extensions.ui.getCompatColor
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.tools.ColorTools
import io.core.common.util.tools.buildMultiLine
import io.core.other.IntentData
import io.core.widget.view.LoadingView
import io.core.widget.view.RotateLoading
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
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

        val get = IntentData.get<Int>("23")
        appCtx.registerReceiver(timeBatteryReceiver, timeBatteryReceiver.filter)
//        binding.time.onClick {
//            showDialog("对话框标题", "这是一个对话框消息。") {
//                okButton {
//                    Toaster.show("点击了确定")
//                }
//                cancelButton {
//                    Toaster.show("点击了取消")
//                }
//            }
//        }

        lifecycleScope.launch(Dispatchers.Default) {
            val props = getSystemProperties()
            val build = StringBuilder()

            val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
            val brandName = Build.BRAND.orEmpty().lowercase()
            val model = Build.MODEL.orEmpty().lowercase()
            val product = Build.PRODUCT.orEmpty().lowercase()


            build.append("detect: manufacturer=$manufacturer, brand=$brandName, model=$model, product=$product\n\n")
            props.forEach { (key, value) ->
                build.append("[$key] = [$value]\n")
            }
            withContext(Dispatchers.Main){
                binding.text.text = build.toString()
            }
        }

    }

    private var job: Coroutine<*>? = null

    override fun onFragmentResume(first: Boolean) {
        super.onFragmentResume(first)
        val loadingView = LoadingView(requireContext(), 100, getCompatColor(R.color.black))
        val loading = RotateLoading(requireContext())
//        addViewToZYLayout(binding.zyLayout, loadingView)
//        addViewToZYLayout(binding.zyLayout1, loading)
        // 在协程作用域中启动
//        job = Coroutine.async(
//            scope = CoroutineScope(Dispatchers.Main), // 指定作用域，默认为 MainScope()
//            context = Dispatchers.Default,          // 指定执行上下文，默认为 Dispatchers.IO
//            start = CoroutineStart.LAZY,            // 指定启动选项，默认为 CoroutineStart.DEFAULT
//            executeContext = Dispatchers.Main,      // 指定回调执行上下文，默认为 Dispatchers.Main
//        ) {
//            while (isActive) { // 循环条件
//                runMain {
//                    loadingView.setColor(ColorTools.getRandomColor())
//                    loading.loadingColor = ColorTools.getRandomColor()
//                }
//                delay(3000)    // 非阻塞式延迟
//            }
//        }
//        job?.start()
    }

    override fun observers() {
//        observeEventSticky<String>(TIME_CHANGED) {
//            binding.time.text = timeFormat.format(Date(System.currentTimeMillis()))
//        }
//        observeEvent<Int>(BATTERY_CHANGED) {
//            binding.battery.text = "当前电量：$it%"
//        }
    }

    override fun onDestroy() {
        super.onDestroy()
//        job?.cancel()
    }

    fun getSystemProperties(): Map<String, String> {
        val properties = mutableMapOf<String, String>()

        try {
            // 执行getprop命令
            val process = Runtime.getRuntime().exec("getprop")
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            // 读取命令输出
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // 解析每一行，格式通常为"[property.name]: [value]"
                line?.let {
                    val parts = it.split("]: [", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].removePrefix("[")
                        val value = parts[1].removeSuffix("]")
                        properties[key] = value
                    }
                }
            }

            // 等待命令执行完成
            process.waitFor()
            reader.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return properties
    }
}