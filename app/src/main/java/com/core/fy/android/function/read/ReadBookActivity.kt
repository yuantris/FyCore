package com.core.fy.android.function.read

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.core.fy.android.R
import com.core.fy.android.constants.PreferKey
import com.core.fy.android.databinding.ActivityBookReadBinding
import com.core.fy.android.function.read.bean.BookProgress
import com.core.fy.android.function.read.model.ReadBook
import com.core.fy.android.function.read.page.ContentTextView
import com.core.fy.android.function.read.page.ReadView
import com.core.fy.android.function.read.page.delegate.PageDelegate
import com.core.fy.android.function.read.page.provider.TextPageFactory
import com.core.fy.android.help.config.ReadBookConfig
import com.core.fy.android.help.tryParesExportFileName
import com.core.libraries.common.util.ext.ui.MainLooper.handler
import com.core.libraries.common.util.ext.ui.getPrefString
import com.core.libraries.common.util.ext.ui.keepScreenOn
import com.core.libraries.common.util.ext.ui.navigationBarGravity
import com.core.libraries.common.util.ext.ui.sysScreenOffTime
import com.core.libraries.common.util.tools.toastOnUi

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/11 10:48
 * @description
 * @author Yuan
 */
class ReadBookActivity : AppCompatActivity(),
    View.OnTouchListener,
    ReadView.CallBack,
    ContentTextView.CallBack {

    private val binding: ActivityBookReadBinding
        get() {
            val binding = ActivityBookReadBinding.inflate(layoutInflater)
            return binding
        }

    val isInMultiWindow: Boolean
        @SuppressLint("ObsoleteSdkInt")
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                isInMultiWindowMode
            } else {
                false
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }

    private var screenTimeOut: Long = 0
    private val screenOffRunnable by lazy { kotlinx.coroutines.Runnable { keepScreenOn(false) } }
    override val headerHeight: Int
        get() = binding.readView.curPage.headerHeight
    override val pageFactory: TextPageFactory
        get() = binding.readView.pageFactory
    override val pageDelegate: PageDelegate?
        get() = binding.readView.pageDelegate
    override val isScroll: Boolean
        get() = binding.readView.isScroll
    var isShowingSearchResult = false
    override var isSelectingSearchResult = false
        set(value) {
            field = value && isShowingSearchResult
        }

    override fun upSelectedStart(x: Float, y: Float, top: Float) {
        TODO("Not yet implemented")
    }

    override fun upSelectedEnd(x: Float, y: Float) {
        TODO("Not yet implemented")
    }

    override fun onImageLongPress(x: Float, y: Float, src: String) {
        TODO("Not yet implemented")
    }

    override fun onCancelSelect() {
        TODO("Not yet implemented")
    }

    override fun onLongScreenshotTouchEvent(event: MotionEvent): Boolean {
        TODO("Not yet implemented")
    }

    override val isInitFinish: Boolean
        get() = true

    override fun showActionMenu() {
        binding.readMenu.runMenuIn()
    }

    override fun screenOffTimerStart() {
        handler.post {
            if (screenTimeOut < 0) {
                keepScreenOn(true)
                return@post
            }
            val t = screenTimeOut - sysScreenOffTime
            if (t > 0) {
                keepScreenOn(true)
                handler.removeCallbacks(screenOffRunnable)
                handler.postDelayed(screenOffRunnable, screenTimeOut)
            } else {
                keepScreenOn(false)
            }
        }
    }


    override fun showTextActionMenu() {
        val navigationBarHeight =
            if (!ReadBookConfig.hideNavigationBar && navigationBarGravity == Gravity.BOTTOM)
                binding.navigationBar.height else 0
//        textActionMenu.show(
//            binding.textMenuPosition,
//            binding.root.height + navigationBarHeight,
//            binding.textMenuPosition.x.toInt(),
//            binding.textMenuPosition.y.toInt(),
//            binding.cursorLeft.y.toInt() + binding.cursorLeft.height,
//            binding.cursorRight.x.toInt(),
//            binding.cursorRight.y.toInt() + binding.cursorRight.height
//        )
    }

    private val isAutoPage get() = binding.readView.isAutoPage
    override fun autoPageStop() {
        if (isAutoPage) {
            binding.readView.autoPager.stop()
            binding.readMenu.setAutoPage(false)
            upScreenTimeOut()
        }
    }

    override fun openChapterList() {
        ReadBook.book?.let {
            // tocActivity.launch(it.bookUrl)
            toastOnUi("打开目录")
        }
    }

    override fun addBookmark() {
        toastOnUi("addBookmark")
    }

    override fun changeReplaceRuleState() {
        toastOnUi("changeReplaceRuleState")
    }

    override fun openSearchActivity(searchWord: String?) {
        toastOnUi("openSearchActivity")
    }

    override fun upSystemUiVisibility() {
//        upSystemUiVisibility(isInMultiWindow, !menuLayoutIsVisible, bottomDialog > 0)
//        upNavigationBarColor()
        toastOnUi("upSystemUiVisibility")
    }

    override fun sureNewProgress(progress: BookProgress) {
        toastOnUi("sureNewProgress")
    }

    private fun upScreenTimeOut() {
        val keepLightPrefer = getPrefString(PreferKey.keepLight)?.toInt() ?: 0
        screenTimeOut = keepLightPrefer * 1000L
        screenOffTimerStart()
    }

    /**
     * view触摸,文字选择
     */
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, event: MotionEvent): Boolean = binding.run {
        if (!binding.readView.isTextSelected) {
            return false
        }
        when (event.action) {
            //MotionEvent.ACTION_DOWN -> textActionMenu.dismiss()
            MotionEvent.ACTION_MOVE -> {
                when (v.id) {
                    R.id.cursor_left -> if (!readView.curPage.getReverseStartCursor()) {
                        readView.curPage.selectStartMove(
                            event.rawX + cursorLeft.width,
                            event.rawY - cursorLeft.height
                        )
                    } else {
                        readView.curPage.selectEndMove(
                            event.rawX - cursorRight.width,
                            event.rawY - cursorRight.height
                        )
                    }

                    R.id.cursor_right -> if (readView.curPage.getReverseEndCursor()) {
                        readView.curPage.selectStartMove(
                            event.rawX + cursorLeft.width,
                            event.rawY - cursorLeft.height
                        )
                    } else {
                        readView.curPage.selectEndMove(
                            event.rawX - cursorRight.width,
                            event.rawY - cursorRight.height
                        )
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
                readView.curPage.resetReverseCursor()
                showTextActionMenu()
            }
        }
        return true
    }
}