package io.core.common.util.ext.ui

import android.widget.SeekBar


fun SeekBar.progressAdd(int: Int) {
    progress += int
}

fun SeekBar.onTrackingTouch(start: (SeekBar) -> Unit, stop: (SeekBar) -> Unit){
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean){}

        override fun onStartTrackingTouch(p0: SeekBar) {
            start.invoke(p0)
        }

        override fun onStopTrackingTouch(p0: SeekBar) {
            stop.invoke(p0)
        }
    })
}

fun SeekBar.onProgressChanged(changed: (SeekBar, Int, Boolean) -> Unit){
    setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekbar: SeekBar, progress: Int, fromUser: Boolean) {
            changed.invoke(seekbar, progress, fromUser)
        }

        override fun onStartTrackingTouch(p0: SeekBar) {}

        override fun onStopTrackingTouch(p0: SeekBar) {}
   })
}