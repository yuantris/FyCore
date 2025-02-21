package com.core.fy.android.main.fragment.code;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.GsonUtils;
import com.core.fy.android.databinding.FragmentJavaBinding;
import com.core.fy.android.function.TestPageActivity;
import com.core.fy.android.util.SafeJson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;

import io.core.common.base.component.fragment.ReflectBindingFragment;
import io.core.common.helper.AppLifecycleTracker;
import io.core.common.helper.TaskExecutor;
import io.core.common.util.MediaScanner;
import io.core.common.util.ToastUtil;
import io.core.common.util.log.LogCat;
import io.core.common.util.log.LogPure;
import io.core.common.util.tools.AsyncUtils;
import io.core.common.util.tools.CollectionTools;
import io.core.common.util.SQL;
import io.core.common.util.tools.OsUtilsKt;

public class JavaFragment extends ReflectBindingFragment<FragmentJavaBinding, TestPageActivity> {


    public JavaFragment() {
        // Required empty public constructor
    }

    @Override
    protected void initView() {
        super.initView();
        ToastUtil.showShort("初始化");

        String name = SafeJson.parse("{\"name\":\"张三\",\"age\":18}").getString("name");
        LogCat.e(name);
        SafeJson object = SafeJson.parse("{\"key\": \"{\\\"nested\\\": 123}\"}").getObject("key");
        LogCat.e(object.getString("nested"));
    }

    @Override
    protected void onFragmentResume(boolean first) {
        super.onFragmentResume(first);

        AsyncUtils.supplyAsync(() -> {
                    Set<MediaScanner.FileType> fileTypes = new HashSet<>();
                    fileTypes.add(MediaScanner.FileType.JPG);
                    fileTypes.add(MediaScanner.FileType.TXT);
                    return MediaScanner.queryFiles(
                            fileTypes, null, SQL.getTimeAddedDESC());
                }, AsyncUtils.getExecutors())
                .thenAccept(fileInfos -> {
                    ToastUtil.showShort("size:" + fileInfos.size());
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
                        boolean existActivity = AppLifecycleTracker.isExistActivity(TestPageActivity.class);
                        if (existActivity){
                            List<String> strings = CollectionTools.mapValuesToList(results);
                            String json = GsonUtils.toJson(strings);
                            LogPure.e(json);
                        }
                    }

                    @Override
                    public void onEachResult(String result, int index) {
                        boolean existActivity = AppLifecycleTracker.isExistActivity(TestPageActivity.class);
                        if (existActivity){
                            LogPure.d(result);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogCat.e(e);
                    }
                }, AsyncUtils.getExecutors());
    }
}