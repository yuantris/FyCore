package com.core.fy.android.function.camerax

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner

// 示例用法
class CameraUsage {
    fun setupCamera(previewView: PreviewView, lifecycleOwner: LifecycleOwner, context: Context) {
        val cameraHelper = CameraHelper.Builder(context, lifecycleOwner, previewView)
            .enableImageCapture(true)
            .enableVideoCapture(true)
            .setImageResolution(Size(1920, 1080))
            .setAnalyzer { image ->
                // 实现图像分析逻辑
                image.close()
            }
            .setErrorCallback { error ->
                Log.e("CameraHelper", error)
            }
            .build()

        cameraHelper.initializeCamera()

        // 拍照示例
        cameraHelper.takePicture { bitmap, error ->
            // 处理结果
        }

        // 录像示例
        cameraHelper.startRecording { file, error ->
            // 处理结果
        }
    }
}