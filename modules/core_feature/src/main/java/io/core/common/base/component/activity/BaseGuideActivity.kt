package io.core.common.base.component.activity

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.gyf.immersionbar.BarHide
import com.gyf.immersionbar.ImmersionBar
import io.core.R
import io.core.common.util.extensions.addCallback
import io.core.common.util.extensions.cool.dpToPx
import io.core.common.util.extensions.exitApp
import io.core.common.util.extensions.ui.onDebouncedClick
import io.core.engine.dialogs.showDialog
import io.core.engine.multi_state.MultiStatePage.config

// GuideConfig.kt
data class GuideConfig(
    val guideImages: List<Int>,
    val enterButtonRes: Int,
    val enableEnterButton: Boolean = false,
    val showIndicator: Boolean = false,
    val indicatorGravity: Int = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
    val enterButtonGravity: Int = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,

    val enterButtonAnim: Animation? = null, // 自定义进入按钮动画
    val enableButtonAnim: Boolean = true, // 自定义自定义按钮动画

    // 单位dp
    val indicatorMargin: Int = 32,
    val enterButtonMargin: Int = 64,
    val indicatorDotNormal: Int = 0,
    val indicatorDotSelected: Int = 0
)

// BaseGuideActivity.kt
abstract class BaseGuideActivity : AppCompatActivity() {
    abstract fun getGuideConfig(): GuideConfig
    abstract fun onEnterClicked()

    private lateinit var viewPager: ViewPager2
    private lateinit var indicatorContainer: LinearLayout
    private lateinit var btnEnter: ImageView
    private lateinit var btnEnterCustom: View

    override fun onCreate(savedInstanceState: Bundle?) {
        ImmersionBar.with(this).hideBar(BarHide.FLAG_HIDE_BAR).init()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_base_guide)

        val config = getGuideConfig()
        initViewPager(config)
        initEnterButton(config)
        if (config.showIndicator) initIndicator(config)

        onBackPressedDispatcher.addCallback(this, enabled = true) {
            showDialog("温馨提示", "是否退出应用？") {
                cancelButton {}
                okButton {
                    exitApp()
                }
            }
        }
    }

    private fun initViewPager(config: GuideConfig) {
        viewPager = findViewById(R.id.viewPager)
        viewPager.adapter = GuidePagerAdapter(config)
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateIndicator(position)
                if (config.enableEnterButton) {
                    btnEnterCustom.visibility =
                        if (position == config.guideImages.lastIndex) View.VISIBLE else View.GONE
                    btnEnterCustom.breathingAnim(config)
                } else {
                    btnEnter.visibility =
                        if (position == config.guideImages.lastIndex) View.VISIBLE else View.GONE
                    btnEnter.breathingAnim(config)
                }
            }
        })
    }

    private fun initEnterButton(config: GuideConfig) {
        if (config.enableEnterButton) {
            btnEnterCustom = findViewById(R.id.btnEnterCustom)
            (btnEnterCustom.layoutParams as FrameLayout.LayoutParams).apply {
                gravity = config.enterButtonGravity
                setMargins(
                    config.enterButtonMargin.dpToPx(), config.enterButtonMargin.dpToPx(),
                    config.enterButtonMargin.dpToPx(), config.enterButtonMargin.dpToPx()
                )
            }
            btnEnterCustom.onDebouncedClick { onEnterClicked() }
        } else {
            btnEnter = findViewById(R.id.btnEnter)
            btnEnter.setImageResource(config.enterButtonRes)
            (btnEnter.layoutParams as FrameLayout.LayoutParams).apply {
                gravity = config.enterButtonGravity
                setMargins(
                    0, 0, 0, config.enterButtonMargin.dpToPx()
                )
            }
            btnEnter.onDebouncedClick { onEnterClicked() }
        }

    }

    private fun View.breathingAnim(config: GuideConfig) {
        // 优先使用自定义动画
        val anim = config.enterButtonAnim ?: createDefaultAnim()
        if (config.enableButtonAnim) {
            this.startAnimation(anim)
        }
    }

    // 创建默认动画
    private fun createDefaultAnim(): Animation {
        // 按钮呼吸动效
        return ScaleAnimation(
            1.0f, 1.1f, 1.0f, 1.1f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        ).apply {
            duration = 350
            repeatMode = Animation.REVERSE
            repeatCount = Animation.INFINITE
        }
    }

    private fun initIndicator(config: GuideConfig) {
        indicatorContainer = findViewById(R.id.indicatorContainer)
        config.guideImages.forEach { _ ->
            val dot = ImageView(this).apply {
                setImageResource(config.indicatorDotNormal)
                layoutParams = LinearLayout.LayoutParams(24, 24).apply {
                    setMargins(8, 0, 8, 0)
                }
            }
            indicatorContainer.addView(dot)
        }
        (indicatorContainer.layoutParams as FrameLayout.LayoutParams).apply {
            gravity = config.indicatorGravity
            setMargins(
                config.indicatorMargin.dpToPx(), config.indicatorMargin.dpToPx(),
                config.indicatorMargin.dpToPx(), config.indicatorMargin.dpToPx()
            )
        }
        updateIndicator(0)
    }

    private fun updateIndicator(position: Int) {
        if (!getGuideConfig().showIndicator) return
        indicatorContainer.children.forEachIndexed { index, view ->
            (view as ImageView).setImageResource(
                if (index == position) getGuideConfig().indicatorDotSelected
                else getGuideConfig().indicatorDotNormal
            )
        }
    }

    private inner class GuidePagerAdapter(private val config: GuideConfig) :
        RecyclerView.Adapter<GuidePagerAdapter.PagerViewHolder>() {
        inner class PagerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val imageView: ImageView = view.findViewById(R.id.ivGuide)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            PagerViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_guide_vp2, parent, false)
            )

        override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
            holder.imageView.setImageResource(config.guideImages[position])
            holder.imageView.scaleType = ImageView.ScaleType.FIT_XY
            if (config.enterButtonRes == 0
                && !config.enableEnterButton
                && position == config.guideImages.lastIndex
            ) {
                holder.imageView.onDebouncedClick { onEnterClicked() }
            }
        }

        override fun getItemCount() = config.guideImages.size
    }
}