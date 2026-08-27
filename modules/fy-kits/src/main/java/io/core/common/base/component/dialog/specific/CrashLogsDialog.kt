package io.core.common.base.component.dialog.specific

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import io.core.kits.R
import io.core.appCtx
import io.core.common.base.component.adapter.BaseRecyclerAdapter
import io.core.common.base.component.adapter.SingleTypeAdapter
import io.core.common.base.component.dialog.BaseDialogFragment
import io.core.common.base.vm.BaseViewModel
import io.core.common.util.FileDoc
import io.core.common.util.extensions.cool.PathType
import io.core.common.util.extensions.cool.getFile
import io.core.common.util.extensions.cool.getSettingsPathV2
import io.core.common.util.extensions.cool.getUri
import io.core.common.util.extensions.cool.toastOnUI
import io.core.common.util.extensions.logE
import io.core.common.util.extensions.logI
import io.core.common.util.extensions.ui.ctx
import io.core.common.util.extensions.ui.getCompatColor
import io.core.common.util.extensions.ui.setLayout
import io.core.common.util.extensions.ui.showDialogFragment
import io.core.common.util.extensions.ui.toast
import io.core.common.util.extensions.ui.toastLong
import io.core.common.util.extensions.ui.viewBinding
import io.core.common.util.log.bury.AppLog
import io.core.common.util.share.FileSharer
import io.core.common.util.share.ShareAir
import io.core.common.util.tools.FileTools
import io.core.common.util.tools.UriTools
import io.core.constant.CRASH_FOLDER_NAME
import io.core.kits.databinding.DialogRecyclerViewBinding
import io.core.other.DoubleClickProcessor
import io.core.other.RandomEventGenerator
import kotlinx.coroutines.isActive
import java.io.File
import java.io.FileFilter

class CrashLogsDialog : BaseDialogFragment(R.layout.dialog_recycler_view),
    Toolbar.OnMenuItemClickListener {

    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val viewModel by viewModels<CrashViewModel>()
    private val adapter by lazy {
        SingleTypeAdapter(
            layoutRes = R.layout.item_1line_text,
            bindFunction = { holder, item ->
                holder.itemView.findViewById<TextView>(R.id.text_view).text = item.name
            },
            itemClickListener = { item, _ ->
                showLogFile(item)
            },
            itemLongClickListener = object : BaseRecyclerAdapter.OnItemLongClickListener<FileDoc> {
                override fun onItemLongClick(item: FileDoc, position: Int): Boolean {
                    viewModel.readFile(item) {
                        if (lifecycleScope.isActive) {
                            UriTools.uri2File(item.uri)?.let {
                                ShareAir.share {
                                    title("分享崩溃日志")
                                    file(it.getUri())
                                }
                            }
                        }
                    }
                    return true
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.apply {
            setBackgroundColor(getCompatColor(R.color.md_amber_300))
            title = "崩溃日志"
            inflateMenu(R.menu.crash_log)
            setOnMenuItemClickListener(this@CrashLogsDialog)
            getChildAt(0).apply {
                // ToolBar标题单击查看AppLog详细信息
                setOnClickListener {
                    execute {
                        val file = AppLog.getLogFiles()[0]
                        val temporaryLog = AppLog.clearTemporaryLog()
                        val outputFile = File(temporaryLog)
                        val bool = AppLog.decryptFile(file, outputFile)
                        if (bool) {
                            outputFile.readText()
                        } else {
                            file.readText()
                        }

                    }.onSuccess {
                        showDialogFragment(TextDialog("运行日志", it))
                    }
                }
                // 长按分享AppLog
                setOnLongClickListener {
                    AppLog.getLogFiles().let {
                        if (it.isNotEmpty()) {
                            val file = it[0]
                            val temporaryLog = AppLog.clearTemporaryLog()
                            val outputFile = File(temporaryLog)
                            val bool = AppLog.decryptFile(file, outputFile)
                            FileSharer.Builder()
                                .setChooserTitle("分享运行日志")
                                .setFileList(listOf(if (bool) outputFile else file))
                                .share(ctx)
                        }
                    }
                    true
                }
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        viewModel.logLiveData.observe(viewLifecycleOwner) {
            adapter.submitList(it)
        }
        viewModel.initData()
    }

    private val randomProcessor by lazy {
        RandomEventGenerator()
            .addEvent { ctx.toast("NoNoNoNo~ 好孩子是不会想着销毁日志的") }
            .addEvent { ctx.toast("心理阴暗！居然想着清除日志！！！") }
            .addEvent { ctx.toastLong("心里要默念 `Tai Shang Lao Jun, quickly quickly biu biu biu!` 就删除了~") }
    }

    private val clearProcessor by lazy {
        DoubleClickProcessor(
            doubleClickAction = { viewModel.clearCrashLog() },
            singleClickHint = { randomProcessor.generate() },
            lifecycle = lifecycle
        )
    }

    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_clear -> clearProcessor.handleClick()
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

    class CrashViewModel : BaseViewModel() {

        val logLiveData = MutableLiveData<List<FileDoc>>()

        fun initData() {
            execute {
                val list = arrayListOf<FileDoc>()
                appCtx.externalCacheDir
                    ?.getFile(CRASH_FOLDER_NAME)
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
                it.message.logE()
                appCtx.toastOnUI(it.localizedMessage)
            }
        }

        fun clearCrashLog() {
            execute {
                appCtx.externalCacheDir
                    ?.getFile(CRASH_FOLDER_NAME)
                    ?.let {
                        FileTools.delete(it, false)
                    }
            }.onError {
                appCtx.toastOnUI(it.localizedMessage)
            }.onFinally {
                initData()
            }
        }

    }

}