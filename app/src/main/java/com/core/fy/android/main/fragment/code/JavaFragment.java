package com.core.fy.android.main.fragment.code;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.GsonUtils;
import com.core.fy.android.databinding.FragmentJavaBinding;
import com.core.fy.android.function.TestPageActivity;
import com.core.fy.android.util.SafeJson;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.SortedMap;

import io.core.common.base.component.fragment.ReflectBindingFragment;
import io.core.common.helper.AppLifecycleTracker;
import io.core.common.helper.JsonUltra;
import io.core.common.helper.TaskExecutor;
import io.core.common.util.MediaScanner;
import io.core.common.util.ToastUtil;
import io.core.common.util.extensions.cool.CollectionKt;
import io.core.common.util.log.LogCat;
import io.core.common.util.log.LogPure;
import io.core.common.util.tools.AsyncUtils;
import io.core.common.util.tools.CollectionTools;
import io.core.common.util.tools.Preferences;
import io.core.common.util.tools.ThreadUltra;

public class JavaFragment extends ReflectBindingFragment<FragmentJavaBinding, TestPageActivity> {


    public JavaFragment() {
        // Required empty public constructor
    }

    @Override
    protected void initView() {
        super.initView();
        ToastUtil.show("初始化");

        String format = JsonUltra.format("{\"name\":\"张三\",\"age\":18}",false);
        LogCat.e(format);

        ThreadUltra.execute(new ThreadUltra.Task<List<Bitmap>>() {
            @Override
            public List<Bitmap> doInBackground() throws Throwable {
                for (int i = 0; i < 20; i++) {
                    publishProgress(i);
                    Thread.sleep(1000);
                }
                return Collections.emptyList();
            }

            @Override
            public void onSuccess(List<Bitmap> result) {

            }

            @Override
            public void onProgress(@NonNull int... values) {
                // LogCat.e(values[0]);
            }

            @Override
            public void onFail(@NonNull ThreadUltra.ErrorType errorType, @NonNull Throwable ex) {
                LogCat.e(ex);
            }
        });
    }

    @Override
    protected void initData() {
        super.initData();

        String name = SafeJson.parse("{\"name\":\"张三\",\"age\":18}").getString("name");
        LogCat.e(name);
        SafeJson object = SafeJson.parse("{\"key\": \"{\\\"nested\\\": 123}\"}").getObject("key");
        LogCat.e(object.getString("nested"));

        JsonUltra.parse("{\"key\": \"{\\\"nested\\\": 123}\"}");
    }

    @Override
    protected void onFragmentResume(boolean first) {
        super.onFragmentResume(first);

        if (first) {
            MediaScanner.registerContentObserver();
        }

        // 使用键值对参数（自动装箱）
        HashMap<String, Object> map1 = CollectionKt.create(
                "name", "Alice",
                "age", 30,
                "scores", new int[]{90, 85}
        );

        // 使用构建器模式（类型安全）
        HashMap<String, Integer> map2 = CollectionKt.<String, Integer>mapBuilder()
                .put("width", 1080)
                .put("height", 1920)
                .build();

        AsyncUtils.supplyAsync(() -> MediaScanner.queryFiles(CollectionKt.createSet(MediaScanner.FileType.M4A)))
                .thenAccept(fileInfos -> {
                    ToastUtil.show("size:" + fileInfos.size());
                }).exceptionally(throwable -> {
                    LogCat.e(throwable);
                    return null;
                });


        List<TaskExecutor.ProcessorTask<String>> tasks = new ArrayList<>();
        tasks.add(() -> "234");
        tasks.add(() -> {
            try {
                Thread.sleep(4000);
            } catch (InterruptedException e) {
            }
            return "兼容";
        });

        TaskExecutor.Companion.get().executeForJava(tasks,
                new TaskExecutor.ConcurrentCallback<String>() {
                    @Override
                    public void onComplete(@NonNull SortedMap<Integer, String> results) {
                        boolean existActivity = AppLifecycleTracker.hasActivity(TestPageActivity.class);
                        if (existActivity) {
                            List<String> strings = CollectionTools.mapValuesToList(results);
                            String json = GsonUtils.toJson(strings);
                            LogPure.e(json);
                        }
                    }

                    @Override
                    public void onEachResult(String result, int index) {
                        boolean existActivity = AppLifecycleTracker.hasActivity(TestPageActivity.class);
                        if (existActivity) {
                            LogPure.d(result);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogCat.e(e);
                    }
                });
    }

    @Override
    public void onStop() {
        super.onStop();
        //MediaScanner.unregisterContentObserver();
    }
}