package com.core.fy.android.function.select

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.blankj.utilcode.util.GsonUtils
import com.core.fy.android.R
import com.core.fy.android.databinding.ActivitySingleSelectBinding
import io.core.common.base.component.activity.ReflectBindingActivity
import io.core.common.util.ToastUtil
import io.core.common.util.extensions.ui.notifyAllDataChanged
import io.core.common.util.extensions.ui.onClick
import io.core.common.util.extensions.ui.toast
import io.core.common.util.tools.findFirstByProperty
import io.core.other.SelectableAdapter
import io.core.other.SelectableItem
import io.core.other.SelectionController

class SingleSelectActivity : ReflectBindingActivity<ActivitySingleSelectBinding>(),
    SelectionController.SelectionStateListener {

    data class SelectItem(
        override val uniqueId: Long,
        val title: String,
        val content: String
    ) : SelectableItem


    private lateinit var controller: SelectionController<SelectItem>
    private lateinit var adapter: SelectAdapter
    private val demoData = List(100) { SelectItem(it.toLong(), "Title $it", "Content $it") }


    override fun initial(savedInstanceState: Bundle?) {
        super.initial(savedInstanceState)

        controller = SelectionController<SelectItem>().apply {
            addListener(this@SingleSelectActivity)
        }
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        adapter = SelectAdapter(demoData, controller)
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@SingleSelectActivity)
            adapter = this@SingleSelectActivity.adapter
        }
    }

    override fun setListener() {
        binding.edit.onClick {
            controller.setEditMode(true)
            adapter.notifyAllDataChanged()
        }
        binding.exitEdit.onClick {
            controller.setEditMode(false)
            adapter.notifyAllDataChanged()
        }
        binding.allSelect.onClick {
            controller.selectAll(demoData)
            adapter.notifyAllDataChanged()
        }
        binding.cancelAllSelect.onClick {
            controller.clearAll()
            adapter.notifyAllDataChanged()
        }
    }

    fun String.removeWhitespace(): String {
        return this.replace("\\s+".toRegex(), "")
    }

    override fun onSelectionChanged(selectedCount: Int) {
        val toJson = GsonUtils.toJson(controller.getSelectedIds())
        toJson.removeWhitespace()
        binding.tip.text = "已选择 $selectedCount 项，\n：$toJson"
        controller.getSelectedIds().apply {
            if (this.isEmpty()) {
                ToastUtil.show("没有选择")
            } else {
                val longs = this.toList()
                val firstByProperty = longs.findFirstByProperty({ it }, 0)
                firstByProperty?.let {
                    ToastUtil.show("$firstByProperty")
                }
            }
        }
    }

    override fun onEditModeChanged(isEditMode: Boolean) {
        toast(if (isEditMode) "进入编辑模式" else "退出编辑模式")
    }

    class SelectAdapter(
        private val data: List<SelectItem>,
        private val controller: SelectionController<SelectItem>
    ) : SelectableAdapter<SelectItem, SelectAdapter.ViewHolder>(controller) {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val checkbox: CheckBox = itemView.findViewById(R.id.checkbox)
            val titleView: TextView = itemView.findViewById(R.id.title)
        }

        override fun onNormalClick(item: SelectItem) {
            ToastUtil.show("点击了${item.title}")
        }

        override fun onItemBind(holder: ViewHolder, item: SelectItem, isSelected: Boolean) {
            with(holder) {
                checkbox.isChecked = isSelected
                checkbox.visibility = if (controller.isEditMode()) View.VISIBLE else View.INVISIBLE
                titleView.text = item.title

                checkbox.onClick { controller.toggleSelection(item) }
            }
        }

        override fun getItem(position: Int): SelectItem = data[position]

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_select_data, parent, false)
            return ViewHolder(view)

        }


        override fun getItemCount(): Int = data.size


    }

}