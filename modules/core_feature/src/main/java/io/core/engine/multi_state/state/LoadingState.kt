package io.core.engine.multi_state.state

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import io.core.R
import io.core.engine.multi_state.MultiState
import io.core.engine.multi_state.MultiStateContainer
import io.core.engine.multi_state.MultiStatePage

class LoadingState : MultiState() {
    private lateinit var tvLoadingMsg: TextView
    override fun onCreateView(
        context: Context,
        inflater: LayoutInflater,
        container: MultiStateContainer,
    ): View {
        return inflater.inflate(R.layout.mult_state_loading, container, false)
    }

    override fun onViewCreated(view: View) {
        tvLoadingMsg = view.findViewById(R.id.tv_loading_msg)
        setLoadingMsg(MultiStatePage.config.loadingMsg)
    }

    override fun onHiddenChanged(hide: Boolean) {
        super.onHiddenChanged(hide)
        if (hide) {
            Log.e("TAG", "LoadingState: 隐藏了")
        } else {
            Log.e("TAG", "LoadingState: 显示了")
        }
    }

    fun setLoadingMsg(loadingMsg: String) {
        tvLoadingMsg.text = loadingMsg
    }
}