package com.core.fy.android.function;

import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.blankj.utilcode.util.ThreadUtils;
import com.core.fy.android.R;
import com.core.fy.android.databinding.ActivityTestCompatibleBinding;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Function;

import io.core.common.base.component.activity.ReflectBindingActivity;
import io.core.common.base.component.dialog.CustomDialog;
import io.core.common.util.MediaScanner;
import io.core.common.util.ToastUtil;
import io.core.common.util.log.LogCat;
import io.core.common.util.tools.AsyncUtils;
import io.core.common.util.tools.ToastUtilsKt;
import io.core.other.CustomToast;
import io.core.other.CustomToastKt;

/**
 # ██████████
 # █▄█████▄█
 # █▼▼▼▼▼
 # █ 
 # █▲▲▲▲▲
 # ██████████
 # ██ ██
 # 注释的艺术，正在加载……
 * 2025/2/13 14:55
 * Java调用Kotlin代码测试
 * @author Yuan
 */
public class TestCodePageActivity extends ReflectBindingActivity<ActivityTestCompatibleBinding> {
    @Override
    protected void initial(@Nullable Bundle savedInstanceState) {
        super.initial(savedInstanceState);

        ToastUtil.showShort("初始化");

        AsyncUtils.supplyAsync(() -> {
                    Set<MediaScanner.FileType> fileTypes = new HashSet<>();
                    fileTypes.add(MediaScanner.FileType.PNG);
                    fileTypes.add(MediaScanner.FileType.TXT);
                    return MediaScanner.Companion.queryFiles(
                            fileTypes, null, MediaStore.MediaColumns.DATE_ADDED + " DESC");
                })
                .thenAccept(fileInfos -> {
                    ToastUtil.showShort("size:" + fileInfos.size());
                    new CustomDialog.Builder(TestCodePageActivity.this)
                            .setLayout(R.layout.dialog_bottom_street)
                            .setSize(500, 400)
                            .build()
                            .show();
                }).exceptionally(throwable -> {
                    LogCat.e(throwable);
                    return null;
                });
    }
}
