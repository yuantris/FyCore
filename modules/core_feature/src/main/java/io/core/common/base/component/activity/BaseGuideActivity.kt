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
import io.core.common.helper.dialogs.showDialog
import io.core.common.util.ext.addCallback
import io.core.common.util.ext.exitApp
import io.core.common.util.ext.ui.setDebouncedClickListener

// GuideConfig.kt
data class GuideConfig(
    val guideImages: List<Int>,
    val enterButtonRes: Int,
    val enableCustomEnterButton: Boolean = false,
    val showIndicator: Boolean = true,
    val indicatorGravity: Int = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
    val enterButtonGravity: Int = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL,
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
                if (config.enableCustomEnterButton) {
                    btnEnterCustom.visibility =
                        if (position == config.guideImages.lastIndex) View.VISIBLE else View.GONE
                    btnEnterCustom.breathingAnim()
                } else {
                    btnEnter.visibility =
                        if (position == config.guideImages.lastIndex) View.VISIBLE else View.GONE
                    btnEnter.breathingAnim()
                }
            }
        })
    }

    private fun initEnterButton(config: GuideConfig) {
        if (config.enableCustomEnterButton) {
            btnEnterCustom = findViewById(R.id.btnEnterCustom)
            (btnEnterCustom.layoutParams as FrameLayout.LayoutParams).apply {
                gravity = config.enterButtonGravity
                setMargins(
                    config.enterButtonMargin, config.enterButtonMargin,
                    config.enterButtonMargin, config.enterButtonMargin
                )
            }
            btnEnterCustom.setDebouncedClickListener { onEnterClicked() }
        } else {
            btnEnter = findViewById(R.id.btnEnter)
            btnEnter.setImageResource(config.enterButtonRes)
            (btnEnter.layoutParams as FrameLayout.LayoutParams).apply {
                gravity = config.enterButtonGravity
                setMargins(
                    config.enterButtonMargin, config.enterButtonMargin,
                    config.enterButtonMargin, config.enterButtonMargin
                )
            }
            btnEnter.setDebouncedClickListener { onEnterClicked() }
        }

    }

    private fun View.breathingAnim() {
        // 按钮呼吸动效
        val animation = ScaleAnimation(
            1.0f, 1.1f, 1.0f, 1.1f,
            Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f
        )
        animation.duration = 350
        animation.repeatMode = Animation.REVERSE
        animation.repeatCount = Animation.INFINITE
        this.startAnimation(animation)
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
                config.indicatorMargin, config.indicatorMargin,
                config.indicatorMargin, config.indicatorMargin
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
                && !config.enableCustomEnterButton
                && position == config.guideImages.lastIndex
            ) {
                holder.imageView.setDebouncedClickListener { onEnterClicked() }
            }
        }

        override fun getItemCount() = config.guideImages.size
    }
}