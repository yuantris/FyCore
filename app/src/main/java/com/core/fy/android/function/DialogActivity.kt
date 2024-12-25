package com.core.fy.android.function

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.core.fy.android.R
import com.core.fy.android.databinding.ActivityDialogBinding
import com.core.fy.android.ui.WaitDialog
import com.core.libraries.base.activity.ReflectBindingActivity
import com.core.libraries.base.dialog.BaseDialog
import com.gyf.immersionbar.ImmersionBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2024/12/25 13:43
 * @description
 * @author Yuan
 */
class DialogActivity : ReflectBindingActivity<ActivityDialogBinding>() {
    private lateinit var waitDialog: BaseDialog
    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        waitDialog = WaitDialog.Builder(this).create()
        // 设置标题，左对齐
        mBinding.titleBar.apply {
            addRightButtonText("关闭"){
                Toast.makeText(this@DialogActivity, "关闭", Toast.LENGTH_SHORT).show()
            }
            setTitleStyle(Typeface.BOLD)
            setLeftButton(null) {
                finish()
            }
        }
    }

    override fun setListener() {
        super.setListener()
        mBinding.apply {
            show.setOnClickListener {
                lifecycleScope.launch {
                    waitDialog.show()
                    delay(3000)
                    waitDialog.dismiss()
                }
            }

            dismiss.setOnClickListener {
                waitDialog.dismiss()
            }
        }
    }
}