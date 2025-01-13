package com.core.fy.android.help

import android.os.Build
import com.core.fy.android.help.config.AppConfig
import com.core.libraries.common.helper.canvasrecorder.CanvasRecorder
import com.core.libraries.common.helper.canvasrecorder.CanvasRecorderApi23Impl
import com.core.libraries.common.helper.canvasrecorder.CanvasRecorderApi29Impl
import com.core.libraries.common.helper.canvasrecorder.CanvasRecorderImpl
import com.core.libraries.common.helper.canvasrecorder.CanvasRecorderLocked

object CanvasRecorderFactory {

    private val atLeastApi24 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
    private val atLeastApi29 = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    // issue 3868
    fun create(locked: Boolean = false): CanvasRecorder {
        val impl = when {
            !AppConfig.optimizeRender -> CanvasRecorderImpl()
            atLeastApi29 -> CanvasRecorderApi29Impl()
            atLeastApi24 -> CanvasRecorderApi23Impl()
            else -> CanvasRecorderImpl()
        }
        return if (locked) {
            CanvasRecorderLocked(impl)
        } else {
            impl
        }
    }

}