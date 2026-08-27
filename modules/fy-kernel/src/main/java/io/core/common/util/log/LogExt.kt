package io.core.common.util.log

const val TAG = "FyCore_"

private enum class LEVEL {
    V, D, I, W, E
}

fun String.v(tag: String = TAG) = log(LEVEL.V, tag, this)
fun String.d(tag: String = TAG) = log(LEVEL.D, tag, this)
fun String.i(tag: String = TAG) = log(LEVEL.I, tag, this)
fun String.w(tag: String = TAG) = log(LEVEL.W, tag, this)
fun String.e(tag: String = TAG) = log(LEVEL.E, tag, this)

private fun log(level: LEVEL, tag: String, message: String) {
    when (level) {
        LEVEL.V -> LogPure.v(tag, message)
        LEVEL.D -> LogPure.d(tag, message)
        LEVEL.I -> LogPure.i(tag, message)
        LEVEL.W -> LogPure.w(tag, message)
        LEVEL.E -> LogPure.e(tag, message)
    }
}