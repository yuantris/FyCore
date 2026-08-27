package io.core.engine.multi_state

fun interface OnNotifyListener<T : MultiState> {
    fun onNotify(multiState: T)
}