package com.core.fy.android.function.ffmpeg

import android.os.Bundle
import android.widget.ScrollView
import androidx.lifecycle.lifecycleScope
import com.core.fy.android.constants.EventKey
import com.core.fy.android.databinding.ActivityFfmpegBinding
import com.core.fy.android.ui.service.FFmpegService
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.util.MediaScanner
import io.core.common.util.extensions.cool.postEvent
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.extensions.ui.startService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FFmpegActivity : ReflectBindingActivity<ActivityFfmpegBinding>() {
    private var list: List<MediaScanner.FileInfo>? = null

    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)
        startService<FFmpegService>()
        lifecycleScope.launch(Dispatchers.IO) {
            val files = MediaScanner.queryFiles(setOf(MediaScanner.MediaFileType.MP4))
            withContext(Dispatchers.Main) {
                val text = buildString {
                    for (file in files) {
                        append("${file.path}\n")
                    }
                }
                binding.videoList.text = text
                list = files
            }
        }


    }

    override fun setListener() {
        super.setListener()
        with(binding) {
            compressAll.onClick {
                postEvent(EventKey.FFmpeg, list)
            }
        }
    }
}