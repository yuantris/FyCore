package io.core.engine.multi_state.state

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import io.core.engine.multi_state.MultiState
import io.core.engine.multi_state.MultiStateContainer

class SuccessState : MultiState() {
    override fun onCreateView(
        context: Context,
        inflater: LayoutInflater,
        container: MultiStateContainer
    ): View {
        return View(context)
    }

    override fun onViewCreated(view: View) = Unit

}