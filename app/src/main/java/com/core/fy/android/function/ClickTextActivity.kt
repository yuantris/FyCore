package com.core.fy.android.function

import android.os.Bundle
import com.core.fy.android.databinding.ActivityClickTextBinding
import com.core.fy.android.ui.MessageDialog
import com.core.libraries.base.activity.ReflectBindingActivity

class ClickTextActivity : ReflectBindingActivity<ActivityClickTextBinding>() {
    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        binding.ctText.setOnLetterClickListener { charSequence, index ->

            MessageDialog.Builder(this)
                .setTitle("温馨提示")
                .setMessage("点击了[$charSequence] 索引为$index")
                .setListener(
                    onConfirm = { binding.ctText.removeHighlight() }
                )
                .show()
        }
    }
}