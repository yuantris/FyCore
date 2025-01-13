package com.core.fy.android.function

import android.graphics.Typeface
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.core.fy.android.R
import com.core.fy.android.databinding.ActivityDialogBinding
import com.core.fy.android.ui.WaitDialog
import com.core.libraries.common.base.activity.ReflectBindingActivity
import com.core.libraries.common.util.ToastUtil
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
    private val dialog by lazy {
        WaitDialog.Builder(this)
    }

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        // 设置标题，左对齐
        binding.titleBar.apply {
            addRightButtonImage(R.drawable.ic_launcher_background) {
                ToastUtil.show("关闭")
            }
            setTitleStyle(Typeface.BOLD)
        }
    }

    override fun setListener() {
        super.setListener()
        binding.apply {
            show.setOnClickListener {
                lifecycleScope.launch {
                    dialog.setMessage("正在加载中").show()
                    delay(2000)
                    dialog.setMessage("加载完成")
                    delay(3000)
                    dialog.dismiss()
                }
            }

            dismiss.setOnClickListener {
                dialog.dismiss()
            }
        }
    }
}