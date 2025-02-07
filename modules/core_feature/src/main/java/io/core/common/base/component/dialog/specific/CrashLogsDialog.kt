package io.core.common.base.component.dialog.specific

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.core.R
import io.core.common.base.component.dialog.BaseDialogFragment
import io.core.common.base.vm.BaseViewModel
import io.core.common.helper.rv.ItemViewHolder
import io.core.common.helper.rv.RecyclerAdapter
import io.core.common.helper.viewbindingdelegate.viewBinding
import io.core.common.util.FileDoc
import io.core.common.util.ext.appCtx
import io.core.common.util.ext.cool.getFile
import io.core.common.util.ext.ui.getCompatColor
import io.core.common.util.ext.ui.setLayout
import io.core.common.util.ext.ui.showDialogFragment
import io.core.common.util.tools.FileUtils
import io.core.common.util.tools.toastOnUi
import io.core.databinding.DialogRecyclerViewBinding
import io.core.databinding.Item1lineTextBinding
import kotlinx.coroutines.isActive
import java.io.FileFilter

class CrashLogsDialog : BaseDialogFragment(R.layout.dialog_recycler_view),
    Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val viewModel by viewModels<CrashViewModel>()
    private val adapter by lazy { LogAdapter() }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(getCompatColor(R.color.md_amber_300))
        binding.toolBar.setTitle("崩溃日志")
        binding.toolBar.inflateMenu(R.menu.crash_log)
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        viewModel.logLiveData.observe(viewLifecycleOwner) {
            adapter.setItems(it)
        }
        viewModel.initData()
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_clear -> viewModel.clearCrashLog()
        }
        return true
    }

    private fun showLogFile(fileDoc: FileDoc) {
        viewModel.readFile(fileDoc) {
            if (lifecycleScope.isActive) {
                showDialogFragment(TextDialog(fileDoc.name, it))
            }
        }

    }

    inner class LogAdapter : RecyclerAdapter<FileDoc, Item1lineTextBinding>(requireContext()) {

        override fun getViewBinding(parent: ViewGroup): Item1lineTextBinding {
            return Item1lineTextBinding.inflate(inflater, parent, false)
        }

        override fun registerListener(holder: ItemViewHolder, binding: Item1lineTextBinding) {
            binding.root.setOnClickListener {
                getItemByLayoutPosition(holder.layoutPosition)?.let { item ->
                    showLogFile(item)
                }
            }
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: Item1lineTextBinding,
            item: FileDoc,
            payloads: MutableList<Any>
        ) {
            binding.textView.text = item.name
        }

    }

    class CrashViewModel() : BaseViewModel() {

        val logLiveData = MutableLiveData<List<FileDoc>>()

        fun initData() {
            execute {
                val list = arrayListOf<FileDoc>()
                appCtx.externalCacheDir
                    ?.getFile("crash")
                    ?.listFiles(FileFilter { it.isFile })
                    ?.forEach {
                        list.add(FileDoc.fromFile(it))
                    }
                return@execute list.sortedByDescending { it.name }
            }.onSuccess {
                logLiveData.postValue(it)
            }
        }

        fun readFile(fileDoc: FileDoc, success: (String) -> Unit) {
            execute {
                String(fileDoc.readBytes())
            }.onSuccess {
                success.invoke(it)
            }.onError {
                appCtx.toastOnUi(it.localizedMessage)
            }
        }

        fun clearCrashLog() {
            execute {
                appCtx.externalCacheDir
                    ?.getFile("crash")
                    ?.let {
                        FileUtils.delete(it, false)
                    }
            }.onError {
                appCtx.toastOnUi(it.localizedMessage)
            }.onFinally {
                initData()
            }
        }

    }

}