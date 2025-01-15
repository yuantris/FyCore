package com.core.fy.android.ui.fragment

import android.annotation.SuppressLint
import android.widget.Toast
import com.core.fy.android.MainActivity
import com.core.fy.android.constants.AppConst.timeFormat
import com.core.fy.android.constants.EventKey.BATTERY_CHANGED
import com.core.fy.android.constants.EventKey.TIME_CHANGED
import com.core.fy.android.databinding.FragmentBlankBinding
import com.core.fy.android.ui.receiver.TimeBatteryReceiver
import com.google.android.material.dialog.MaterialDialogs
import io.core.common.base.fragment.ReflectBindingFragment
import io.core.common.helper.dialogs.alert
import io.core.common.util.log.logD
import io.core.common.util.log.logI
import io.core.common.util.ext.cool.observeEvent
import io.core.common.util.ext.cool.observeEventSticky
import io.core.common.util.ext.ui.onClick
import io.core.engine.livebus.core.Console
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
        context?.registerReceiver(timeBatteryReceiver, timeBatteryReceiver.filter)
        binding.time.onClick {
            // throw RuntimeException("ssssssss")
            alert("对话框标题", "这是一个对话框消息。") {
                okButton {
                    Toast.makeText(activity, "点击了确定", Toast.LENGTH_SHORT).show()
                }
                cancelButton {
                    Toast.makeText(activity, "点击了取消", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun initData() {
        super.initData()
        observeEventSticky<String>(TIME_CHANGED) {
            binding.time.text = timeFormat.format(Date(System.currentTimeMillis()))
            Console.getInfo().logD()
        }
        observeEvent<Int>(BATTERY_CHANGED) {
            binding.battery.text = "当前电量：$it%"
            Console.getInfo().logI()
        }
    }
}