package io.core.common.helper.event.channel

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/7 8:54
 * @description
 * @author Yuan
 */

/**
 * Channel承载事件的模型
 */
@PublishedApi
internal class ChannelEvent<T>(val event: T, val tag: String? = null)