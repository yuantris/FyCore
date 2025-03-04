package com.core.fy.android.function.read.services

import android.app.PendingIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.core.fy.android.R
import com.core.fy.android.constants.AppPattern
import com.core.fy.android.function.read.bean.SelectItem
import com.core.fy.android.function.read.model.ReadAloud
import com.core.fy.android.function.read.model.ReadBook
import com.core.fy.android.help.MediaHelp
import com.core.fy.android.help.config.AppConfig
import io.core.common.helper.coroutine.Coroutine
import io.core.common.util.extensions.cool.GSON
import io.core.common.util.extensions.cool.fromJsonObject
import io.core.common.util.extensions.logE
import io.core.common.util.extensions.ui.servicePendingIntent
import io.core.common.util.log.AppLog
import io.core.common.util.log.LogCat
import io.core.common.util.log.LogPure
import io.core.common.util.tools.toastOnUI
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive

/**
 * 本地朗读
 */
class TTSReadAloudService : BaseReadAloudService(), TextToSpeech.OnInitListener {

    private var textToSpeech: TextToSpeech? = null
    private var ttsInitFinish = false
    private val ttsUtteranceListener = TTSUtteranceListener()
    private var speakJob: Coroutine<*>? = null
    private val TAG = "TTSReadAloudService"

    override fun onCreate() {
        super.onCreate()
        initTts()
    }

    override fun onDestroy() {
        super.onDestroy()
        clearTTS()
    }

    @Synchronized
    private fun initTts() {
        ttsInitFinish = false
        val engine = GSON.fromJsonObject<SelectItem<String>>(ReadAloud.ttsEngine).getOrNull()?.value
        LogPure.d(TAG, "initTts engine:$engine")
        textToSpeech = if (engine.isNullOrBlank()) {
            TextToSpeech(this, this)
        } else {
            TextToSpeech(this, this, engine)
        }
        upSpeechRate()
    }

    @Synchronized
    fun clearTTS() {
        textToSpeech?.runCatching {
            stop()
            shutdown()
        }
        textToSpeech = null
        ttsInitFinish = false
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let {
                it.setOnUtteranceProgressListener(ttsUtteranceListener)
                ttsInitFinish = true
                play()
            }
        } else {
            toastOnUI(R.string.tts_init_failed)
        }
    }

    @Synchronized
    override fun play() {
        if (!ttsInitFinish) return
        if (!requestFocus()) return
        if (contentList.isEmpty()) {
            LogCat.d("朗读列表为空")
            ReadBook.readAloud()
            return
        }
        super.play()
        MediaHelp.playSilentSound(this@TTSReadAloudService)
        speakJob?.cancel()
        speakJob = execute {
            LogPure.d(TAG, "朗读列表大小 ${contentList.size}")
            LogPure.d(TAG, "朗读页数 ${textChapter?.pageSize}")
            val tts = textToSpeech ?: throw RuntimeException("tts is null")
            val contentList = contentList
            var isAddedText = false
            for (i in nowSpeak until contentList.size) {
                ensureActive()
                var text = contentList[i]
                if (paragraphStartPos > 0 && i == nowSpeak) {
                    text = text.substring(paragraphStartPos)
                }
                if (text.matches(AppPattern.notReadAloudRegex)) {
                    continue
                }
                if (!isAddedText) {
                    val result = tts.runCatching {
                        speak(text, TextToSpeech.QUEUE_FLUSH, null, TAG + i)
                    }.getOrElse {
                        LogCat.e(it, msg = "tts出错\n${it.localizedMessage}")
                        TextToSpeech.ERROR
                    }
                    if (result == TextToSpeech.ERROR) {
                        LogCat.e("tts出错 尝试重新初始化")
                        clearTTS()
                        initTts()
                        return@execute
                    }
                } else {
                    val result = tts.runCatching {
                        speak(text, TextToSpeech.QUEUE_ADD, null, TAG + i)
                    }.getOrElse {
                        LogCat.e(it, msg = "tts出错\n${it.localizedMessage}")
                        TextToSpeech.ERROR
                    }
                    if (result == TextToSpeech.ERROR) {
                        LogCat.e("tts朗读出错:$text")
                    }
                }
                isAddedText = true
            }
            LogPure.d(TAG, "朗读内容添加完成")
            if (!isAddedText) {
                playStop()
                delay(1000)
                nextChapter()
            }
        }.onError {
            LogCat.e(it, msg = "tts朗读出错\n${it.localizedMessage}")
        }
    }

    override fun playStop() {
        textToSpeech?.runCatching {
            stop()
        }
    }

    /**
     * 更新朗读速度
     */
    override fun upSpeechRate(reset: Boolean) {
        if (AppConfig.ttsFlowSys) {
            if (reset) {
                clearTTS()
                initTts()
            }
        } else {
            val speechRate = (AppConfig.ttsSpeechRate + 5) / 10f
            textToSpeech?.setSpeechRate(speechRate)
        }
    }

    /**
     * 暂停朗读
     */
    override fun pauseReadAloud(abandonFocus: Boolean) {
        super.pauseReadAloud(abandonFocus)
        speakJob?.cancel()
        textToSpeech?.runCatching {
            stop()
        }
    }

    /**
     * 恢复朗读
     */
    override fun resumeReadAloud() {
        super.resumeReadAloud()
        play()
    }

    /**
     * 朗读监听
     */
    private inner class TTSUtteranceListener : UtteranceProgressListener() {

        private val TAG = "TTSUtteranceListener"

        override fun onStart(s: String) {
            LogPure.d(TAG, "onStart nowSpeak:$nowSpeak pageIndex:$pageIndex utteranceId:$s")
            textChapter?.let {
                if (contentList[nowSpeak].matches(AppPattern.notReadAloudRegex)) {
                    nextParagraph()
                }
                if (readAloudNumber + 1 > it.getReadLength(pageIndex + 1)) {
                    pageIndex++
                    ReadBook.moveToNextPage()
                }
                upTtsProgress(readAloudNumber + 1)
            }
        }

        override fun onDone(s: String) {
            LogPure.d(TAG, "onDone utteranceId:$s")
            nextParagraph()
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
            super.onRangeStart(utteranceId, start, end, frame)
            val msg =
                "onRangeStart nowSpeak:$nowSpeak pageIndex:$pageIndex utteranceId:$utteranceId start:$start end:$end frame:$frame"
            LogPure.d(TAG, msg)
            textChapter?.let {
                if (readAloudNumber + start > it.getReadLength(pageIndex + 1)) {
                    pageIndex++
                    ReadBook.moveToNextPage()
                    upTtsProgress(readAloudNumber + start)
                }
            }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            LogPure.e(
                TAG,
                "onError nowSpeak:$nowSpeak pageIndex:$pageIndex utteranceId:$utteranceId errorCode:$errorCode"
            )
            nextParagraph()
        }

        private fun nextParagraph() {
            //跳过全标点段落
            do {
                readAloudNumber += contentList[nowSpeak].length + 1 - paragraphStartPos
                paragraphStartPos = 0
                nowSpeak++
                if (nowSpeak >= contentList.size) {
                    nextChapter()
                    return
                }
            } while (contentList[nowSpeak].matches(AppPattern.notReadAloudRegex))
        }

        @Deprecated("Deprecated in Java")
        override fun onError(s: String) {
            LogPure.e(TAG, "onError nowSpeak:$nowSpeak pageIndex:$pageIndex s:$s")
            nextParagraph()
        }

    }

    override fun aloudServicePendingIntent(actionStr: String): PendingIntent? {
        return servicePendingIntent<TTSReadAloudService>(actionStr)
    }

    override fun isForegroundService(): Boolean {
        return true
    }

}