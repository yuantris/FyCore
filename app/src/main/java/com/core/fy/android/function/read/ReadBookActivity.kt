package com.core.fy.android.function.read

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.core.fy.android.R
import com.core.fy.android.constants.PreferKey
import com.core.fy.android.databinding.ActivityBookReadBinding
import com.core.fy.android.function.read.bean.BookProgress
import com.core.fy.android.function.read.model.ReadAloud
import com.core.fy.android.function.read.model.ReadBook
import com.core.fy.android.function.read.page.ContentTextView
import com.core.fy.android.function.read.page.ReadView
import com.core.fy.android.function.read.page.delegate.PageDelegate
import com.core.fy.android.function.read.page.provider.TextPageFactory
import com.core.fy.android.help.config.AppConfig
import com.core.fy.android.help.config.ReadBookConfig
import com.core.fy.android.room.entity.Book
import com.core.libraries.common.base.component.activity.ReflectBindingActivity
import com.core.libraries.common.util.log.logD
import com.core.libraries.common.util.ext.ui.MainLooper.handler
import com.core.libraries.common.util.ext.ui.getCompatColor
import com.core.libraries.common.util.ext.ui.getPrefString
import com.core.libraries.common.util.ext.ui.invisible
import com.core.libraries.common.util.ext.ui.keepScreenOn
import com.core.libraries.common.util.ext.ui.navigationBarGravity
import com.core.libraries.common.util.ext.ui.sysScreenOffTime
import com.core.libraries.common.util.ext.ui.toast
import com.core.libraries.common.util.ext.ui.visible
import com.core.libraries.common.util.tools.toastOnUi
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
 * 2025/1/11 10:48
 * @description
 * @author Yuan
 */
class ReadBookActivity : ReflectBindingActivity<ActivityBookReadBinding>(),
    View.OnTouchListener,
    ReadView.CallBack,
    ReadMenu.CallBack,
    TextActionMenu.CallBack,
    ReadBook.CallBack,
    ContentTextView.CallBack {


    val isInMultiWindow: Boolean
        @SuppressLint("ObsoleteSdkInt")
        get() {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                isInMultiWindowMode
            } else {
                false
            }
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        val accentColor = getCompatColor(R.color.red)
        binding.cursorLeft.setColorFilter(accentColor)
        binding.cursorRight.setColorFilter(accentColor)
        binding.cursorLeft.setOnTouchListener(this)
        binding.cursorRight.setOnTouchListener(this)
        ReadBook.register(this)
    }

    /**
     * 按键拦截,显示菜单
     */
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val action = event.action
        val isDown = action == 0

        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (isDown && !binding.readMenu.canShowMenu) {
                binding.readMenu.runMenuIn()
                return true
            }
            if (!isDown && !binding.readMenu.canShowMenu) {
                binding.readMenu.canShowMenu = true
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        upSystemUiVisibility()
        if (hasFocus) {
            binding.readMenu.upBrightnessState()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        upSystemUiVisibility()
        binding.readView.upStatusBar()
    }

    override fun onResume() {
        super.onResume()
        upSystemUiVisibility()
        //registerReceiver(timeBatteryReceiver, timeBatteryReceiver.filter)
        binding.readView.upTime()
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
    val textActionMenu: TextActionMenu by lazy {
        TextActionMenu(this, this)
    }

    override fun upSelectedStart(x: Float, y: Float, top: Float) = binding.run {
        cursorLeft.x = x - cursorLeft.width
        cursorLeft.y = y
        cursorLeft.visible(true)
        textMenuPosition.x = x
        textMenuPosition.y = top
    }

    override fun upSelectedEnd(x: Float, y: Float) = binding.run {
        cursorRight.x = x
        cursorRight.y = y
        cursorRight.visible(true)
    }

    override fun onImageLongPress(x: Float, y: Float, src: String) {
        TODO("Not yet implemented")
    }

    override fun onCancelSelect() = binding.run {
        cursorLeft.invisible()
        cursorRight.invisible()
        textActionMenu.dismiss()
    }

    override fun onLongScreenshotTouchEvent(event: MotionEvent): Boolean {
        return binding.readView.onTouchEvent(event)
    }

    override val isInitFinish: Boolean
        get() = true

    override fun showActionMenu() {
        toastOnUi("showActionMenu")
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
        textActionMenu.show(
            binding.textMenuPosition,
            binding.root.height + navigationBarHeight,
            binding.textMenuPosition.x.toInt(),
            binding.textMenuPosition.y.toInt(),
            binding.cursorLeft.y.toInt() + binding.cursorLeft.height,
            binding.cursorRight.x.toInt(),
            binding.cursorRight.y.toInt() + binding.cursorRight.height
        )
    }

    private val isAutoPage get() = binding.readView.isAutoPage
    override fun autoPageStop() {
        if (isAutoPage) {
            binding.readView.autoPager.stop()
            binding.readMenu.setAutoPage(false)
            upScreenTimeOut()
        }
    }

    override fun autoPage() {
        ReadAloud.stop(this)
        if (isAutoPage) {
            autoPageStop()
        } else {
            binding.readView.autoPager.start()
            binding.readMenu.setAutoPage(true)
            screenTimeOut = -1L
            screenOffTimerStart()
        }
    }

    override fun openReplaceRule() {
        toastOnUi("打开替换规则")
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

    override fun openSourceEditActivity() {
        toastOnUi("openSourceEditActivity")
    }

    override fun openBookInfoActivity() {
        toastOnUi("openBookInfoActivity")
    }

    override fun showReadStyle() {
        toastOnUi("showReadStyle")
    }

    override fun showMoreSetting() {
        toastOnUi("showMoreSetting")
    }

    override fun showReadAloudDialog() {
        toastOnUi("showReadAloudDialog")
    }

    override fun upSystemUiVisibility() {
//        upSystemUiVisibility(isInMultiWindow, !menuLayoutIsVisible, bottomDialog > 0)
//        upNavigationBarColor()
    }

    override fun onClickReadAloud() {
        toastOnUi("onClickReadAloud")
    }

    override fun showHelp() {
        toastOnUi("showHelp")
    }

    override fun showLogin() {
        toastOnUi("showLogin")
    }

    override fun payAction() {
        toastOnUi("payAction")
    }

    override fun disableSource() {
        toastOnUi("disableSource")
    }

    override fun skipToChapter(index: Int) {
        toastOnUi("skipToChapter")
    }

    override fun onMenuShow() {
        //toastOnUi("onMenuShow")
        binding.readView.autoPager.pause()
    }

    override fun onMenuHide() {
        toastOnUi("onMenuHide")
        binding.readView.autoPager.resume()
    }

    override fun upMenuView() {
        TODO("Not yet implemented")
    }

    override fun loadChapterList(book: Book) {
        TODO("Not yet implemented")
    }

    override fun upContent(
        relativePosition: Int,
        resetPageOffset: Boolean,
        success: (() -> Unit)?
    ) {
        TODO("Not yet implemented")
    }

    override suspend fun upContentAwait(
        relativePosition: Int,
        resetPageOffset: Boolean,
        success: (() -> Unit)?
    ) {
        TODO("Not yet implemented")
    }

    override fun pageChanged() {
        TODO("Not yet implemented")
    }

    override fun contentLoadFinish() {
        TODO("Not yet implemented")
    }

    override fun upPageAnim(upRecorder: Boolean) {
        TODO("Not yet implemented")
    }

    override fun notifyBookChanged() {
        toast("notifyBookChanged")
    }

    override fun sureNewProgress(progress: BookProgress) {
        toastOnUi("sureNewProgress")
    }

    override fun cancelSelect() {
        TODO("Not yet implemented")
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
            MotionEvent.ACTION_DOWN -> textActionMenu.dismiss()
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

    override val selectedText: String
        get() = binding.readView.getSelectText()

    override fun onMenuItemSelected(itemId: Int): Boolean {
        when (itemId) {
            R.id.menu_aloud -> when (AppConfig.contentSelectSpeakMod) {
                1 -> lifecycleScope.launch {
                    binding.readView.aloudStartSelect()
                }

                else -> {
                    binding.readView.getSelectText().logD()
                    // speak(binding.readView.getSelectText())
                }
            }

            R.id.menu_bookmark -> binding.readView.curPage.let {
                val bookmark = it.createBookmark()
                if (bookmark == null) {
                    //toastOnUi(R.string.create_bookmark_error)
                } else {
                    //showDialogFragment(BookmarkDialog(bookmark))
                }
                return true
            }

            R.id.menu_replace -> {
                val scopes = arrayListOf<String>()
                ReadBook.book?.name?.let {
                    scopes.add(it)
                }
//                ReadBook.bookSource?.bookSourceUrl?.let {
//                    scopes.add(it)
//                }
//                replaceActivity.launch(
//                    ReplaceEditActivity.startIntent(
//                        this,
//                        pattern = selectedText,
//                        scope = scopes.joinToString(";")
//                    )
//                )
                return true
            }

            R.id.menu_search_content -> {
                //viewModel.searchContentQuery = selectedText
                openSearchActivity(selectedText)
                return true
            }

            R.id.menu_dict -> {
                //showDialogFragment(DictDialog(selectedText))
                return true
            }
        }
        return false
    }

    override fun onMenuActionFinally() {
        textActionMenu.dismiss()
        binding.readView.cancelSelect()
    }
}