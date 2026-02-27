package io.core.engine.brv.item

/**
 * 可展开/折叠的条�?
 */
interface ItemExpand {

    /** 同级别的分组的索引位�?*/
    var itemGroupPosition: Int

    /** 是否已展开 */
    var itemExpand: Boolean

    /** 子列�?*/
    fun getItemSublist(): List<Any?>?
}