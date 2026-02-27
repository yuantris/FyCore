package io.core.engine.livebus.core;

/**
 * # ██████████
 * # █▄█████▄�?
 * # █▼▼▼▼▼
 * # �?
 * # █▲▲▲▲▲
 * # ██████████
 * # ██ ██
 * # 注释的艺术，正在加载…�?
 * 2025/1/15 14:48
 * 调试信息控制�?
 *
 * @author Yuan
 */
public final class Console {

    private Console() {
    }

    /**
     * 获取控制台信�?
     *
     * @return 调试信息
     */
    public static String getInfo() {
        return LiveEventBusCore.get().console.getConsoleInfo();
    }
}
