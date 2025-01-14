package com.core.fy.android.function

import android.os.Bundle
import androidx.core.view.isVisible
import com.core.fy.android.databinding.ActivityVisibilityBinding
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.util.ext.ui.hide
import io.core.common.util.ext.cool.launchSync
import io.core.common.util.ext.ui.onVisibilityChange
import io.core.common.util.ext.ui.setDebouncedClickListener
import io.core.common.util.ext.ui.setVisible
import io.core.common.util.ext.ui.show
import io.core.common.util.ext.ui.toast
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
            setDebouncedClickListener {
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