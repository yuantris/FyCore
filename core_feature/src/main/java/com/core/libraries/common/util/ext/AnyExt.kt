package com.core.libraries.common.util.ext

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/3 17:58
 * @description
 * @author Yuan
 */

fun Any?.isNotNull(action: () -> Unit) {
    if (this != null) {
        action()
    }
}

fun Any?.isNull(action: () -> Unit) {
    if (this == null) {
        action()
    }
}

fun Any?.verify(action: (isNull: Boolean) -> Unit) {
    action(this == null)
}
