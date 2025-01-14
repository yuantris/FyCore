package com.core.fy.android.ui.fragment

import android.annotation.SuppressLint
import com.core.fy.android.MainActivity
import com.core.fy.android.constants.AppConst.timeFormat
import com.core.fy.android.databinding.FragmentBlankBinding
import com.core.fy.android.ui.receiver.TimeBatteryReceiver
import io.core.common.base.fragment.ReflectBindingFragment
import io.core.common.util.log.logD
import io.core.common.util.log.logI
import io.core.common.util.ext.cool.observeEvent
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
    }

    override fun initData() {
        super.initData()
        observeEvent<String>(timeBatteryReceiver.TIME_CHANGED){
            binding.time.text = timeFormat.format(Date(System.currentTimeMillis()))
            Console.getInfo().logD()
        }
        observeEvent<Int>(timeBatteryReceiver.BATTERY_CHANGED) {
            binding.battery.text = "当前电量：$it%"
            Console.getInfo().logI()
        }
    }
}