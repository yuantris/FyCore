package com.core.fy.android.function

import android.os.Bundle
import androidx.databinding.adapters.TextViewBindingAdapter.setText
import com.core.fy.android.databinding.ActivityClickTextBinding
import com.core.fy.android.ui.MessageDialog
import com.core.libraries.base.activity.ReflectBindingActivity
import java.util.regex.Pattern

class ClickTextActivity : ReflectBindingActivity<ActivityClickTextBinding>() {
    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
//        binding.ctText.apply {
//            text = formatTextWithNewLines(binding.ctText.text.toString())
//        }
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

    /**
     * 格式化文本，在标点符号后加入换行符，并去掉其他位置的换行符
     */
    private fun formatTextWithNewLines(text: String): String {
        // 去除所有换行符
        val noNewLines = text.replace("\\n".toRegex(), "")

        // 在标点符号后加入换行符
        val punctuationPattern = Pattern.compile("[.,!?;:。！？，；：]")
        val matcher = punctuationPattern.matcher(noNewLines)
        val formattedText = matcher.replaceAll("$0\n")

        return formattedText
    }
}