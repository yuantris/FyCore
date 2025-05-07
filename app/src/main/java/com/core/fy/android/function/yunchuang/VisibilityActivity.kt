package com.core.fy.android.function.yunchuang

import android.os.Bundle
import androidx.core.view.isVisible
import com.core.fy.android.databinding.ActivityVisibilityBinding
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.util.extensions.cool.launch
import io.core.common.util.extensions.cool.runDelayedMain
import io.core.common.util.extensions.ui.hide
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
        runDelayedMain(3000){
            binding.imageView.animations {
                parallel {
                    translateY(200f) { duration = 1000 }
                    sequence {
                        alpha(0f) { duration = 500 }
                        alpha(1f) { duration = 500 }
                    }
                }
                sequence {
                    rotation(360f) { duration = 1000 }
                    scale(2f) { duration = 500 }
                }
            }.start()
        }


    }

    override fun setListener() {
        super.setListener()
        with(binding.imageView){
            onDebouncedClick {
                if (isVisible) {
                    hide()
                    launch {
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