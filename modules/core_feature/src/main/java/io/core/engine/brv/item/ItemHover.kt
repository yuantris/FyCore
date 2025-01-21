package io.core.engine.brv.item

/**
 * 可粘性头部的条目
 */
interface ItemHover {
    /**
     * 是否启用粘性头部
     * [io.core.engine.brv.utils.RecyclerUtilsKt.linear]
     * [io.core.engine.brv.utils.RecyclerUtilsKt.grid]
     * [io.core.engine.brv.utils.RecyclerUtilsKt.staggered]
     */
    var itemHover: Boolean
}