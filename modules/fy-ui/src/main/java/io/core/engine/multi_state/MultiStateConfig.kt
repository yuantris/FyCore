package io.core.engine.multi_state

import androidx.annotation.DrawableRes
import io.core.ui.R

data class MultiStateConfig(
    val errorMsg: String = "哎呀,出错了",
    @DrawableRes
    val errorIcon: Int = R.drawable.ic_error_24,
    val emptyMsg: String = "这里什么都没有",
    @DrawableRes
    val emptyIcon: Int = R.drawable.ic_error_24,
    val loadingMsg: String = "loading...",
    var alphaDuration: Long = 500,
    val defaultState: MultiState? = null
) {

    class Builder {
        private var errorMsg: String = "哎呀,出错了"

        @DrawableRes
        private var errorIcon: Int = R.drawable.ic_error_24
        private var emptyMsg: String = "这里什么都没有"

        @DrawableRes
        private var emptyIcon: Int = R.drawable.ic_error_24
        private var loadingMsg: String = "loading..."
        private var alphaDuration: Long = 500

        fun errorMsg(msg: String): Builder {
            this.errorMsg = msg
            return this
        }

        fun errorIcon(@DrawableRes icon: Int): Builder {
            this.errorIcon = icon
            return this
        }

        fun emptyMsg(msg: String): Builder {
            this.emptyMsg = msg
            return this
        }

        fun emptyIcon(@DrawableRes icon: Int): Builder {
            this.emptyIcon = icon
            return this
        }

        fun loadingMsg(msg: String): Builder {
            this.loadingMsg = msg
            return this
        }

        fun alphaDuration(duration: Long): Builder {
            this.alphaDuration = duration
            return this
        }

        fun build() = MultiStateConfig(
            errorMsg = errorMsg,
            errorIcon = errorIcon,
            emptyMsg = emptyMsg,
            emptyIcon = emptyIcon,
            loadingMsg = loadingMsg,
            alphaDuration = alphaDuration,
        )
    }
}