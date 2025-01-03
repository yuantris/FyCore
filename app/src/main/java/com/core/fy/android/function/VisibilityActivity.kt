package com.core.fy.android.function

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import com.core.fy.android.databinding.ActivityVisibilityBinding
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.base.ext.hide
import com.core.libraries.base.ext.launchSafe
import com.core.libraries.base.ext.onVisibilityChange
import com.core.libraries.base.ext.setDebouncedClickListener
import com.core.libraries.base.ext.setVisible
import com.core.libraries.base.ext.show
import com.core.libraries.base.ext.toast
import kotlinx.coroutines.delay

class VisibilityActivity : ReflectBindingActivity<ActivityVisibilityBinding>() {

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        mBinding.imageView.onVisibilityChange { _, isVisible ->
            if (isVisible) {
                toast("ImageView is visible")
            } else {
                toast("ImageView is invisible")
            }
        }
    }

    override fun setListener() {
        super.setListener()
        with(mBinding.imageView){
            setDebouncedClickListener {
                if (isVisible) {
                    hide()
                    launchSafe {
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