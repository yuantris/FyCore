package com.core.fy.android.function.yunchuang

import android.os.Bundle
import androidx.core.view.isVisible
import com.core.fy.android.databinding.ActivityVisibilityBinding
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.util.extensions.ui.hide
import io.core.common.util.extensions.cool.launchSync
import io.core.common.util.extensions.ui.onDebouncedClick
import io.core.common.util.extensions.ui.onVisibilityChange
import io.core.common.util.extensions.ui.setVisible
import io.core.common.util.extensions.ui.show
import io.core.common.util.extensions.ui.toast
import kotlinx.coroutines.delay

class VisibilityActivity : ReflectBindingActivity<ActivityVisibilityBinding>() {

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        binding.imageView.onVisibilityChange { _, isVisible ->
            if (isVisible) {
                toast("ImageView is visible")
            } else {
                toast("ImageView is invisible")
            }
        }
    }

    override fun setListener() {
        super.setListener()
        with(binding.imageView){
            onDebouncedClick {
                if (isVisible) {
                    hide()
                    launchSync {
                        delay(1000)
                        show()
                    }
                } else {
                    setVisible(true)
                }
            }
        }
    }
}