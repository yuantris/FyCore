package com.core.fy.android.function

import android.graphics.Color
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import com.core.fy.android.databinding.ActivityTtsBinding
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.util.log.logE
import java.util.Locale

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/10 11:45
 * @description
 * @author Yuan
 */
class TTSActivity : ReflectBindingActivity<ActivityTtsBinding>(), TextToSpeech.OnInitListener {

    private lateinit var tts: TextToSpeech
    private var textToRead = "这是一个测试文本。这是两个测试文本,这是三个测试文本,这是四个测试文本"
    private var sentenceList = listOf<String>()
    private var currentSentenceIndex = 0
    private val utteranceIds = mutableListOf<String>()

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        tts = TextToSpeech(this, this)

        // 分割文本为句子
        sentenceList = textToRead.split(Regex("[。,.，]")).filter { it.isNotBlank() }
        binding.text.text = textToRead
    }

    override fun setListener() {
        super.setListener()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                runOnUiThread {
                    highlightCurrentSentence()
                }
            }

            override fun onDone(utteranceId: String?) {
                runOnUiThread {
                    currentSentenceIndex++
                    if (currentSentenceIndex < sentenceList.size) {
                        readNextSentence()
                    } else {
                        clearHighlight()
                    }
                }
            }

            override fun onError(utteranceId: String?) {
                "onError: $utteranceId".logE()
            }
        })
    }

    private fun highlightCurrentSentence() {
        val spannable = SpannableString(textToRead)
        var startIndex = 0
        for (i in 0 until currentSentenceIndex) {
            startIndex += sentenceList[i].length + 1 // +1 for punctuation
        }
        val endIndex = startIndex + sentenceList[currentSentenceIndex].length

        spannable.setSpan(
            ForegroundColorSpan(Color.RED),
            startIndex,
            endIndex,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        binding.text.text = spannable
    }

    private fun clearHighlight() {
        binding.text.text = textToRead
    }

    private fun readNextSentence() {
        val sentence = sentenceList[currentSentenceIndex]
        val utteranceId = "utterance_$currentSentenceIndex"
        utteranceIds.add(utteranceId)

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        tts.speak(sentence, TextToSpeech.QUEUE_ADD, params, utteranceId)
    }

    override fun onInit(status: Int) {
        when (status) {
            TextToSpeech.SUCCESS -> {
                // 初始化成功
                val result = tts.setLanguage(Locale.CHINA)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "This Language is not supported")
                } else {
                    // 设置语速（1.0 是默认语速，0.5 是慢速，2.0 是快速）
                    tts.setSpeechRate(0.2f) // 设置为 1.2 倍速
                    // 设置音调（1.0 是默认音调，0.5 是低音调，2.0 是高音调）
                    tts.setPitch(0.5f) // 设置为 1.1 倍音调
                    readNextSentence()
                }
            }

            TextToSpeech.ERROR -> {
                // 初始化失败
                Log.e("TTS", "Initialization Failed!")
            }

        }
    }

    override fun onDestroy() {
        if (::tts.isInitialized) {
            tts.stop()
            tts.shutdown()
        }
        super.onDestroy()
    }
}
