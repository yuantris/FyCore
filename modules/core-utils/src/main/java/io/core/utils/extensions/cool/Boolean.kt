package io.core.utils.extensions.cool

class BooleanConfig {
    var ifTrue: (() -> Unit)? = null
    var ifFalse: (() -> Unit)? = null

    operator fun invoke(config: BooleanConfig.() -> Unit) {
        config()
    }
}

fun Boolean.ifNext(config: BooleanConfig.() -> Unit) {
    val setup = BooleanConfig().apply(config).apply {
        require(ifTrue != null || ifFalse != null) {
            "至少需要设置一个条件分�?
        }
    }
    if (this) {
        setup.ifTrue?.invoke()
    } else {
        setup.ifFalse?.invoke()
    }
}