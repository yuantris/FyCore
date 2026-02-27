package io.core.ui.widget.view.scroller

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