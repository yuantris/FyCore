package com.core.fy.android.ui.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.core.fy.android.constants.EventKey.BATTERY_CHANGED
import com.core.fy.android.constants.EventKey.TIME_CHANGED
import io.core.common.util.ext.cool.postEvent


class TimeBatteryReceiver : BroadcastReceiver() {

    val filter = IntentFilter().apply {
        addAction(Intent.ACTION_TIME_TICK)
        addAction(Intent.ACTION_BATTERY_CHANGED)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_TIME_TICK -> {
                // sendEvent("", TIME_CHANGED)
                postEvent(TIME_CHANGED, "")
            }

            Intent.ACTION_BATTERY_CHANGED -> {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                // sendEvent(level, BATTERY_CHANGED)
                postEvent(BATTERY_CHANGED, level)
            }
        }
    }

/*    registerBroadcastReceiver(requireContext(), {
        addAction(Intent.ACTION_TIME_TICK)
        addAction(Intent.ACTION_BATTERY_CHANGED)
    }) {
        when (it.action) {
            Intent.ACTION_TIME_TICK -> {
                // sendEvent("", TIME_CHANGED)
                postEvent(TIME_CHANGED, "")
            }

            Intent.ACTION_BATTERY_CHANGED -> {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                // sendEvent(level, BATTERY_CHANGED)
                postEvent(BATTERY_CHANGED, level)
            }
        }
    }*/
}