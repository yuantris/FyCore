package com.core.fy.android.main.fragment.code;

import android.app.Activity;
import android.graphics.Bitmap;
import android.net.Uri;

import androidx.annotation.NonNull;

import com.blankj.utilcode.util.GsonUtils;
import com.core.fy.android.databinding.FragmentJavaBinding;
import com.core.fy.android.function.TestPageActivity;
import com.core.fy.android.help.ProgressNotifier;
import com.core.fy.android.util.SafeJson;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import io.core.common.base.component.custom.ToastGT;
import io.core.common.base.component.fragment.ReflectBindingFragmentV2;
import io.core.common.helper.JsonUltra;
import io.core.common.helper.TryV2;
import io.core.common.helper.track.AppTrackV2;
import io.core.common.helper.valid.NullCheck;
import io.core.common.util.MediaScanner;
import io.core.common.util.Toaster;
import io.core.common.util.concurrent.Concurrency;
import io.core.common.util.concurrent.TaskExecutor;
import io.core.common.util.extensions.cool.CollectionKt;
import io.core.common.util.log.LogCat;
import io.core.common.util.log.LogPure;
import io.core.common.util.tools.CollectionTools;
import io.core.common.util.tools.ThreadUltra;
import io.core.common.util.tools.UriTools;
import io.core.other.LiveDataPro;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;

public class JavaFragment extends ReflectBindingFragmentV2<FragmentJavaBinding> {


    public JavaFragment() {
        // Required empty public constructor
    }

    @Override
    protected void initView() {
        super.initView();

        AppTrackV2.executeUISafely(requireActivity(), activity -> {
            ToastGT.Companion.show(activity,"初始化",3000);
            return true;
        });


        if (NullCheck.isEmpty("1")) {
            Toaster.show("初始化");
        }

        String format = JsonUltra.format("{\"name\":\"张三\",\"age\":18}", false);
        LogCat.e(format);

        JsonUltra ultra = JsonUltra.parse("{\"code\":1,\"message\":\"success\",\"data\":{\"邮政平邮\":\"youzhengbk\",\"申通快递\":\"shentong\",\"圆通快递\":\"yuantong\",\"中通快递\":\"zhongtong\",\"极兔速递\":\"jtexpress\",\"韵达快递\":\"yunda\",\"德邦快递\":\"debangkuaidi\",\"顺丰快递\":\"shunfeng\"}}");
        String data = ultra.getNotNull("data").asString();
        Map<String, String> map = CollectionTools.jsonToMap(data);
        LogCat.e(map);

        ThreadUltra.executeWithLifecycle(this, new ThreadUltra.Task<List<Bitmap>>() {
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
        TryV2.runSafely(() -> {
            SafeJson object = SafeJson.parse("{\"key\": \"{\\\"nested\\\": 123}\"}").getObject("key");
            LogCat.e(object.getString("nested"));
        }, LogCat::e);

        String formatted = JsonUltra.format("{\"key\": \"{\\\"nested\\\": 123}\"}");
        LogCat.e(formatted);


        String task = ProgressNotifier.startTask(100, 5000L, taskId -> {

        });
        ProgressNotifier.register((taskId, percent) -> {

        });
    }

    @Override
    protected void onFragmentVisible() {
        super.onFragmentVisible();
        LogPure.e("Fragment JavaFragment is visible");
    }

    @Override
    protected void onFragmentResume(boolean first) {
        super.onFragmentResume(first);

        LiveDataPro.on("12", String.class)
                .with(this, LogPure::d);
        LiveDataPro.postEvent("12", "12");

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

        Concurrency.supplyAsync(() -> MediaScanner.queryFiles(CollectionKt.createSet(MediaScanner.MediaFileType.M4A)))
                .thenAccept(fileInfos -> {
                    Toaster.show("size:" + fileInfos.size());
                    String path = fileInfos.get(0).getPath();
                    Uri uri = UriTools.path2Uri(path);
                }).exceptionally(throwable -> {
                    LogCat.e(throwable);
                    return null;
                });

        List<TaskExecutor.ProcessorTask<String>> tasks = Arrays.asList(
                () -> "234",
                () -> {
                    try {
                        Thread.sleep(4000);
                    } catch (InterruptedException e) {
                    }
                    return "兼容";
                }
        );

        TaskExecutor.get().execute(tasks, new TaskExecutor.ConcurrentCallback<String>() {
            @Override
            public void onError(@NotNull Throwable e) {
                LogCat.e(e);
            }

            @Override
            public void onComplete(@NotNull SortedMap<Integer, String> results) {
                boolean existActivity = AppTrackV2.hasActivity(TestPageActivity.class);
                if (existActivity) {
                    List<String> strings = CollectionTools.mapValuesToList(results);
                    String json = GsonUtils.toJson(strings);
                    LogPure.e(json);
                }
            }

            @Override
            public void onEachResult(String result, int index) {
                TaskExecutor.ConcurrentCallback.super.onEachResult(result, index);
                boolean existActivity = AppTrackV2.hasActivity(TestPageActivity.class);
                if (existActivity) {
                    LogPure.d(result);
                }
            }

        });
    }

    @Override
    public void onStop() {
        super.onStop();
        //MediaScanner.unregisterContentObserver();
    }
}