package com.core.fy.android.widget.temporary

interface FastScrollStateChangeListener {

    /**
     * Called when fast scrolling begins
     */
    fun onFastScrollStart(fastScroller: FastScroller)

    /**
     * Called when fast scrolling ends
     */
    fun onFastScrollStop(fastScroller: FastScroller)
}