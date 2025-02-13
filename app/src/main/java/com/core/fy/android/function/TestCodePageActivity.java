package com.core.fy.android.function;

import android.os.Bundle;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.core.fy.android.databinding.ActivityTestCompatibleBinding;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import io.core.common.base.component.activity.ReflectBindingActivity;
import io.core.common.helper.ConcurrentProcessor;
import io.core.common.util.MediaScanner;
import io.core.common.util.ToastUtil;
import io.core.common.util.log.LogCat;
import io.core.common.util.tools.AsyncUtils;

/**
 * # ██████████
 * # █▄█████▄█
 * # █▼▼▼▼▼
 * # █
 * # █▲▲▲▲▲
 * # ██████████
 * # ██ ██
 * # 注释的艺术，正在加载……
 * 2025/2/13 14:55
 * Java调用Kotlin代码测试
 *
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
                }).exceptionally(throwable -> {
                    LogCat.e(throwable);
                    return null;
                });

        List<ConcurrentProcessor.ProcessorTask<String>> tasks = new ArrayList<>();
        tasks.add(() -> "234");
        tasks.add(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
            }
            return "兼容";
        });
        ConcurrentProcessor.Companion.get().executeForJava(tasks,
                new ConcurrentProcessor.ConcurrentCallback<String>() {
                    @Override
                    public void onComplete(@NonNull List<? extends String> results) {

                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                }, 1, TimeUnit.MINUTES);
    }
}
