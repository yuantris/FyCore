package com.core.fy.android.main.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Build
import androidx.core.graphics.createBitmap
import androidx.lifecycle.lifecycleScope
import com.core.fy.android.MainActivity
import com.core.fy.android.R
import com.core.fy.android.databinding.FragmentBlankBinding
import com.core.fy.android.databinding.ItemSingleTextBinding
import com.core.fy.android.ui.receiver.TimeBatteryReceiver
import io.core.appCtx
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.helper.coroutine.Coroutine
import io.core.common.helper.pool.UniversalPool
import io.core.common.util.extensions.ui.getCompatColor
import io.core.engine.brv.utils.linear
import io.core.engine.brv.utils.setup
import io.core.other.IntentData
import io.core.widget.view.LoadingView
import io.core.widget.view.RotateLoading
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

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

        val universalPool = UniversalPool.Builder<Bitmap>().apply {
            creator = { createBitmap(1080, 1920) }
        }.build()
        val bitmap = universalPool.borrow()
        universalPool.release(bitmap)

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

            val manufacturer = Build.MANUFACTURER.orEmpty().lowercase()
            val brandName = Build.BRAND.orEmpty().lowercase()
            val model = Build.MODEL.orEmpty().lowercase()
            val product = Build.PRODUCT.orEmpty().lowercase()


            val list = mutableListOf<String>()
            list.add("detect: manufacturer=$manufacturer, brand=$brandName, model=$model, product=$product")
            list.add("\n")
            props.forEach { (key, value) ->
                list.add("[${key}] = [$value]")
            }
            withContext(Dispatchers.Main) {
                binding.fastScroller.apply {
                    linear().setup {
                        addType<String>(R.layout.item_single_text)
                        onBind {
                            val binding = getBinding<ItemSingleTextBinding>()
                            val data = getModel<String>()
                            binding.text.text = data
                        }
                    }.models = list
                }
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