package io.core.engine.multi_state.state

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.DrawableRes
import io.core.R
import io.core.engine.multi_state.MultiState
import io.core.engine.multi_state.MultiStateContainer
import io.core.engine.multi_state.MultiStatePage

class ErrorState : MultiState() {

    private lateinit var tvErrorMsg: TextView
    private lateinit var imgError: ImageView
    private lateinit var tvRetry: TextView

    private var retry: OnRetryClickListener? = null

    override fun onCreateView(
        context: Context,
        inflater: LayoutInflater,
        container: MultiStateContainer
    ): View {
        return inflater.inflate(R.layout.mult_state_error, container, false)
    }

    override fun onViewCreated(view: View) {
        tvErrorMsg = view.findViewById(R.id.tv_error_msg)
        imgError = view.findViewById(R.id.img_error)
        tvRetry = view.findViewById(R.id.tv_retry)

        setErrorMsg(MultiStatePage.config.errorMsg)
        setErrorIcon(MultiStatePage.config.errorIcon)

        tvRetry.setOnClickListener { retry?.retry() }
    }

    fun setErrorMsg(errorMsg: String) {
        tvErrorMsg.text = errorMsg
    }

    fun setErrorIcon(@DrawableRes errorIcon: Int) {
        imgError.setImageResource(errorIcon)
    }

    fun retry(retry: OnRetryClickListener) {
        this.retry = retry
    }

    fun interface OnRetryClickListener {
        fun retry()
    }
    override fun onHiddenChanged(hide: Boolean) {
        super.onHiddenChanged(hide)
        if (hide) {
            Log.e("TAG", "ErrorState: 隐藏�?)
        } else {
            Log.e("TAG", "ErrorState: 显示�?)
        }
    }
}