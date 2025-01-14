package io.core.engine.livebus.core;

import android.content.Context;
import androidx.annotation.NonNull;
import io.core.engine.livebus.logger.Logger;


/**
 # ██████████
 # █▄█████▄█
 # █▼▼▼▼▼
 # █
 # █▲▲▲▲▲
 # ██████████
 # ██ ██
 # 注释的艺术，正在加载……
 * 2025/1/15 14:48
 * @author Yuan
 */
public class Config {

    /**
     * lifecycleObserverAlwaysActive
     * set if then observer can always receive message
     * true: observer can always receive message
     * false: observer can only receive message when resumed
     *
     * @param active boolean
     * @return Config
     */
    public Config lifecycleObserverAlwaysActive(boolean active) {
        LiveEventBusCore.get().setLifecycleObserverAlwaysActive(active);
        return this;
    }

    /**
     * @param clear boolean
     * @return true: clear livedata when no observer observe it
     * false: not clear livedata unless app was killed
     */
    public Config autoClear(boolean clear) {
        LiveEventBusCore.get().setAutoClear(clear);
        return this;
    }

    /**
     * config broadcast
     * only if you called this method, you can use broadcastValue() to send broadcast message
     *
     * @param context Context
     * @return Config
     */
    public Config setContext(Context context) {
        LiveEventBusCore.get().registerReceiver();
        return this;
    }

    /**
     * setLogger, if not set, use DefaultLogger
     *
     * @param logger Logger
     * @return Config
     */
    public Config setLogger(@NonNull Logger logger) {
        LiveEventBusCore.get().setLogger(logger);
        return this;
    }

    /**
     * set logger enable or disable, default enable
     *
     * @param enable boolean
     * @return Config
     */
    public Config enableLogger(boolean enable) {
        LiveEventBusCore.get().enableLogger(enable);
        return this;
    }
}
