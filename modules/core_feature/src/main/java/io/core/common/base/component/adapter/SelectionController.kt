package io.core.common.base.component.adapter

import androidx.recyclerview.widget.RecyclerView
import io.core.common.util.extensions.ui.notifyAllDataChanged

// --------------------- 核心逻辑层 ---------------------
interface SelectableItem {
    val uniqueId: Long
}

class SelectionController<T : SelectableItem> {
    private val selectedItems = mutableSetOf<Long>()
    private var isEditMode = false
    private val listeners = mutableSetOf<SelectionStateListener>()

    fun toggleSelection(item: T) {
        if (selectedItems.contains(item.uniqueId)) {
            selectedItems.remove(item.uniqueId)
        } else {
            selectedItems.add(item.uniqueId)
        }
        notifySelectionChanged()
    }

    fun selectAll(items: Collection<T>) {
        if (!isEditMode) return
        selectedItems.addAll(items.map { it.uniqueId })
        notifySelectionChanged()
    }

    fun clearAll() {
        selectedItems.clear()
        notifySelectionChanged()
    }

    fun isSelected(item: T): Boolean = selectedItems.contains(item.uniqueId)

    fun getSelectedCount(): Int = selectedItems.size

    fun getSelectedIds(): Set<Long> = selectedItems.toSet()

    fun setEditMode(enable: Boolean) {
        isEditMode = enable
        if (!enable) clearAll()
        notifyEditModeChanged()
    }

    fun isEditMode(): Boolean = isEditMode

    fun addListener(listener: SelectionStateListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: SelectionStateListener) {
        listeners.remove(listener)
    }

    private fun notifySelectionChanged() {
        listeners.forEach {
            it.onSelectionChanged(selectedItems.size)
            it.onSelectedIdsChanged(selectedItems.toSet())
        }
    }

    private fun notifyEditModeChanged() {
        listeners.forEach { it.onEditModeChanged(isEditMode) }
    }

    interface SelectionStateListener {
        fun onSelectionChanged(selectedCount: Int)
        fun onEditModeChanged(isEditMode: Boolean)
        // 新增完整选中集合通知
        fun onSelectedIdsChanged(ids: Set<Long>) = Unit
    }
}

abstract class SelectableAdapter<T : SelectableItem, VH : RecyclerView.ViewHolder>(
    private val controller: SelectionController<T>
) : RecyclerView.Adapter<VH>() {

    abstract fun onNormalClick(item: T)
    abstract fun onItemBind(holder: VH, item: T, isSelected: Boolean)

    final override fun onBindViewHolder(holder: VH, position: Int) {
        val item = getItem(position)
        setupClickListeners(holder, item)
        onItemBind(holder, item, controller.isSelected(item))
    }

    protected abstract fun getItem(position: Int): T

    private fun setupClickListeners(holder: VH, item: T) {
        holder.itemView.setOnClickListener {
            if (controller.isEditMode()) {
                controller.toggleSelection(item)
                notifyItemChanged(holder.layoutPosition)
            } else {
                onNormalClick(item)
            }
        }

        holder.itemView.setOnLongClickListener {
            if (!controller.isEditMode()) {
                controller.setEditMode(true)
                controller.toggleSelection(item)
                notifyAllDataChanged()
                true
            } else {
                false
            }
        }
    }
}