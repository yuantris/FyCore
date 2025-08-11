package com.core.fy.android.main.fragment

import android.app.Activity
import android.app.Service
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import com.core.fy.android.Config
import com.core.fy.android.MainActivity
import com.core.fy.android.R
import com.core.fy.android.databinding.FragmentStatusBinding
import com.core.fy.android.databinding.ItemFunctionBinding
import com.core.fy.android.function.CollapsingBarActivity
import com.core.fy.android.function.brv.BrvActivity
import com.core.fy.android.function.camerax.CameraXActivity
import com.core.fy.android.function.database.RoomActivity
import com.core.fy.android.function.dialog.DialogActivity
import com.core.fy.android.function.event.EventActivity
import com.core.fy.android.function.ffmpeg.FFmpegActivity
import com.core.fy.android.function.json.JsonActivity
import com.core.fy.android.function.keyboard.KeyboardActivity
import com.core.fy.android.function.lottie.LottieActivity
import com.core.fy.android.function.media.MediaPlayerActivity
import com.core.fy.android.function.read.ReadBookActivity
import com.core.fy.android.function.record.AudioRecordActivity
import com.core.fy.android.function.select.SingleSelectActivity
import com.core.fy.android.function.toast.CustomToastActivity
import com.core.fy.android.function.tts.ClickTextActivity
import com.core.fy.android.function.tts.TTSActivity
import com.core.fy.android.function.yunchuang.ImgTextActivity
import com.core.fy.android.function.yunchuang.VisibilityActivity
import com.core.fy.android.interfaces.LeastAnimationStateChangedHandler
import com.core.fy.android.room.VMFactory
import com.core.fy.android.room.entity.Function
import com.core.fy.android.room.repository.FunctionRepository
import com.core.fy.android.viewmodel.FunctionVM
import com.core.fy.android.ui.CustomToast
import io.core.common.base.component.custom.ToastGT
import io.core.common.base.component.fragment.ReflectBindingFragment
import io.core.common.helper.track.AppTrackV2
import io.core.common.helper.track.v3.AppTrackV3
import io.core.common.helper.track.v3.ComponentFilter
import io.core.common.util.extensions.cool.GSON
import io.core.common.util.extensions.cool.launch
import io.core.common.util.extensions.cool.runDelayedMain
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.extensions.ui.startActivity
import io.core.common.util.log.e
import io.core.common.util.log.v
import io.core.common.util.tools.TimeTools
import io.core.engine.brv.BindingAdapter
import io.core.engine.brv.listener.DefaultItemTouchCallback
import io.core.engine.brv.utils.grid
import io.core.engine.brv.utils.setup

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/2/13 8:22
 * @description
 * @author Yuan
 */
class StatusFragment : ReflectBindingFragment<FragmentStatusBinding, MainActivity>() {

    private val functionVM by viewModels<FunctionVM> {
        VMFactory(FunctionRepository)
    }


    override fun initView() {
        binding.state.stateChangedHandler = LeastAnimationStateChangedHandler()
        if (Config.isDisplayHomeSkeletonAnim) {
            binding.state.onRefresh {
                functionVM.initRvData()
            }.showLoading()
        } else {
            functionVM.initRvData()
            binding.state.showContent()
        }
    }

    override fun initData() {
        "当前季节：${TimeTools.getSeason().name}".e()
        functionVM.data.observe(this) {
            binding.rv.apply {
                grid(2).setup {
                    addType<Function>(R.layout.item_function)
                    itemTouchHelper = ItemTouchHelper(object : DefaultItemTouchCallback() {
                        override fun onDrag(
                            source: BindingAdapter.BindingViewHolder,
                            target: BindingAdapter.BindingViewHolder
                        ) {
                            launch {
                                models?.forEachIndexed { index, model ->
                                    if (model is Function) {
                                        model.position = index
                                        // 更新位置信息
                                        functionVM.repository.dao.update(model)
                                    }
                                }
                            }
                        }
                    })
                    onBind {
                        val binding = getBinding<ItemFunctionBinding>()
                        val data = getModel<Function>()
                        binding.item.text = data.design.function
                        binding.item.onClick {
                            when (data.design) {
                                FunctionVM.Design.KEYBOARD -> startActivity<KeyboardActivity>()
                                FunctionVM.Design.云创控件 -> {
                                    startActivity<ImgTextActivity> {
                                        putExtra("title", "云创控件")
                                        putExtra("url", "当前的Url")
                                    }
                                }

                                FunctionVM.Design.单文字点击的TextView -> startActivity<ClickTextActivity>()
                                FunctionVM.Design.ROOM -> startActivity<RoomActivity>()
                                FunctionVM.Design.DIALOG -> startActivity<DialogActivity>()
                                FunctionVM.Design.TOAST -> startActivity<CustomToastActivity>()
                                FunctionVM.Design.EVENT -> startActivity<EventActivity>()
                                FunctionVM.Design.COLL_BAR -> startActivity<CollapsingBarActivity>()
                                FunctionVM.Design.VIEW_VISIBILITY -> startActivity<VisibilityActivity>()
                                FunctionVM.Design.TTS -> startActivity<TTSActivity>()
                                FunctionVM.Design.READ -> startActivity<ReadBookActivity>()
                                FunctionVM.Design.相机 -> startActivity<CameraXActivity>()
                                FunctionVM.Design.单选多选 -> startActivity<SingleSelectActivity>()
                                FunctionVM.Design.Brv -> startActivity<BrvActivity>()
                                FunctionVM.Design.录音 -> startActivity<AudioRecordActivity>()
                                FunctionVM.Design.Media -> startActivity<MediaPlayerActivity>()
                                FunctionVM.Design.Json -> startActivity<JsonActivity>()
                                FunctionVM.Design.FFmpeg -> startActivity<FFmpegActivity>()
                                FunctionVM.Design.Lottie -> startActivity<LottieActivity>()
                                else -> {
                                    // do nothing
                                    ToastGT.show(requireContext(), "该添加点击事件了")
                                }
                            }
                        }
                    }
                }.models = it?.ifEmpty { functionVM.list }
            }
            binding.state.showContent()
        }
    }

    override fun onFragmentResume(first: Boolean) {
        super.onFragmentResume(first)
        runDelayedMain(100) {
            AppTrackV2.getTopFragment()?.let {
                "当前展示的Fragment为：${it.javaClass.simpleName}".v()
            }
        }
    }
}