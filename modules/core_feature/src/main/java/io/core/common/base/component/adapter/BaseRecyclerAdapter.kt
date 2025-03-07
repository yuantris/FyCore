package io.core.common.base.component.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import io.core.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonDisposableHandle.parent
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 通用Adapter基类
abstract class BaseRecyclerAdapter<T : Any> : RecyclerView.Adapter<BaseViewHolder<T>>() {

    // 改为属性方式便于继承修改
    var itemClickListener: OnItemClickListener<T>? = null
    var itemLongClickListener: OnItemLongClickListener<T>? = null

    // region 核心字段
    protected val items = mutableListOf<T>()
    private val scope = CoroutineScope(Dispatchers.Default + Job())
    // endregion

    // region 必须实现的抽象方法
    abstract override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T>
    // endregion

    // region 基础功能增强
    override fun onBindViewHolder(holder: BaseViewHolder<T>, position: Int) {
        val item = items.getOrNull(position) ?: return
        holder.bind(item)
        setupItemClickListeners(holder, item, position)
    }

    /**
     * 带局部更新的绑定方法（当使用DiffUtil Payload时触发）
     */
    override fun onBindViewHolder(
        holder: BaseViewHolder<T>,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            holder.bindWithPayload(getItem(position), payloads)
        }
    }

    override fun getItemCount(): Int = items.size

    /**
     * 获取指定位置的数据项
     */
    fun getItem(position: Int): T = items[position]
    // endregion

    // region 列表更新功能
    /**
     * 异步提交新列表（自动处理线程切换）
     * @param newItems 新数据列表
     * @param commitCallback 更新完成回调（可选）
     */
    fun submitList(newItems: List<T>, commitCallback: (() -> Unit)? = null) {
        if (newItems === items) return

        scope.launch {
            val copyList = ArrayList(newItems)
            val diffResult = DiffUtil.calculateDiff(DiffCallback(items, copyList))
            withContext(Dispatchers.Main) {
                items.clear()
                items.addAll(copyList)
                diffResult.dispatchUpdatesTo(this@BaseRecyclerAdapter)
                commitCallback?.invoke()
            }
        }
    }

    /**
     * 同步提交新列表（直接替换）
     * @param newItems 新数据列表
     */
    fun setList(newItems: List<T>) {
        items.clear()
        items.addAll(newItems)
    }

    /**
     * 添加数据项
     * @param item 新数据项
     */
    fun addItem(item: T) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }
    // endregion

    // region 点击事件处理
    private fun setupItemClickListeners(holder: BaseViewHolder<T>, item: T, position: Int) {
        holder.itemView.setOnClickListener {
            if (isValidClickPosition(position)) {
                itemClickListener?.onItemClick(item, position)
            }
        }
        holder.itemView.setOnLongClickListener {
            if (isValidClickPosition(position)) {
                itemLongClickListener?.onItemLongClick(item, position) ?: false
            } else false
        }
    }

    /**
     * 验证点击位置有效性（防止快速点击时列表已更新导致位置越界）
     */
    private fun isValidClickPosition(position: Int): Boolean {
        return position in 0 until itemCount && position < items.size
    }
    // endregion

    // region DiffUtil增强实现
    /**
     * 自定义DiffCallback实现
     * - 通过itemId进行项对比
     * - 支持Payload局部更新
     */
    private inner class DiffCallback(
        private val oldList: List<T>,
        private val newList: List<T>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean {
            return getItemId(oldList[oldPos]) == getItemId(newList[newPos])
        }

        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean {
            return oldList[oldPos] == newList[newPos]
        }

        override fun getChangePayload(oldPos: Int, newPos: Int): Any? {
            return if (payloadTypes.isNotEmpty()) {
                getPayload(oldList[oldPos], newList[newPos])
            } else null
        }
    }

    /**
     * 获取数据项唯一标识（子类可重写）
     */
    open fun getItemId(item: T): Long = item.hashCode().toLong()

    /**
     * 生成Payload数据（需要子类实现具体逻辑）
     */
    protected open fun getPayload(oldItem: T, newItem: T): Any? = null

    /**
     * 声明支持的Payload类型（需要子类重写）
     */
    protected open val payloadTypes: Set<Int> = emptySet()
    // endregion

    // region 接口定义
    fun interface OnItemClickListener<T> {
        fun onItemClick(item: T, position: Int)
    }

    interface OnItemLongClickListener<T> {
        fun onItemLongClick(item: T, position: Int): Boolean
    }
    // endregion
}

// region ViewHolder基类
/**
 * 增强版ViewHolder基类
 * - 添加Payload绑定支持
 * - 内置视图缓存功能
 */
abstract class BaseViewHolder<T> : RecyclerView.ViewHolder {

    constructor(parent: ViewGroup, layoutRes: Int) : super(
        LayoutInflater.from(parent.context).inflate(layoutRes, parent, false)
    )

    constructor(binding: ViewBinding) : super(binding.root)

    /**
     * 常规数据绑定方法
     */
    abstract fun bind(item: T)

    /**
     * 带Payload的绑定方法（默认调用常规绑定）
     */
    open fun bindWithPayload(item: T, payloads: List<Any>) {
        bind(item)
    }
}
// endregion

// region 多类型适配器实现
/**
 * 基于委托模式的多类型适配器
 * @param delegates 视图委托集合（每个委托处理一种类型）
 */
class MultiTypeAdapter<T : Any>(
    private val delegates: List<ItemViewDelegate<T>>
) : BaseRecyclerAdapter<T>() {

    override fun getItemViewType(position: Int): Int {
        return delegates.first { it.isForType(items[position]) }.viewType
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T> {
        return delegates.first { it.viewType == viewType }.createViewHolder(parent)
    }
}

/**
 * 视图委托接口
 */
interface ItemViewDelegate<T> {
    val viewType: Int
    fun isForType(item: T): Boolean
    fun createViewHolder(parent: ViewGroup): BaseViewHolder<T>
}
// endregion

// 单类型Adapter实现
class SingleTypeAdapter<T : Any>(
    private val layoutRes: Int,
    private val bindFunction: (BaseViewHolder<T>, T) -> Unit,
    itemClickListener: OnItemClickListener<T>? = null,
    itemLongClickListener: OnItemLongClickListener<T>? = null
) : BaseRecyclerAdapter<T>() {

    init {
        this.itemClickListener = itemClickListener
        this.itemLongClickListener = itemLongClickListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder<T> {
        return object : BaseViewHolder<T>(parent, layoutRes) {
            override fun bind(item: T) {
                bindFunction(this, item)
            }
        }
    }
}

// region 工具扩展
/**
 * 快速创建单类型适配器的工厂方法
 */
inline fun <reified T : Any> singleTypeAdapter(
    layoutRes: Int,
    crossinline bind: BaseViewHolder<T>.(T, List<Any>?) -> Unit,
) = object : BaseRecyclerAdapter<T>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        object : BaseViewHolder<T>(parent, layoutRes) {
            override fun bind(item: T) = bind.invoke(this, item, null)

            override fun bindWithPayload(item: T, payloads: List<Any>) {
                bind.invoke(this, item, payloads)
            }
        }
}

/**
 * 快速创建普通布局的 ViewHolder
 * @param parent RecyclerView 父容器
 * @param layoutRes 布局资源ID
 * @param bindFunc 数据绑定函数（通过扩展函数访问 ViewHolder）
 */
inline fun <T : Any> createLayoutViewHolder(
    parent: ViewGroup,
    layoutRes: Int,
    crossinline bindFunc: BaseViewHolder<T>.(T, List<Any>?) -> Unit
) = object : BaseViewHolder<T>(parent, layoutRes) {
    override fun bind(item: T) {
        bindFunc(item, null)
    }

    override fun bindWithPayload(item: T, payloads: List<Any>) {
        bindFunc(item, payloads)
    }
}

/**
 * 快速创建支持 ViewBinding 的 ViewHolder
 * @param parent RecyclerView 父容器
 * @param inflate ViewBinding 的 inflate 方法引用
 * @param bindFunc 数据绑定函数（通过扩展函数访问 Binding）
 */
inline fun <T : Any, B : ViewBinding> createBindingViewHolder(
    parent: ViewGroup,
    crossinline inflate: (LayoutInflater, ViewGroup, Boolean) -> B,
    crossinline bindFunc: B.(T, BaseViewHolder<T>, List<Any>?) -> Unit
) = object : BaseViewHolder<T>(
    inflate(LayoutInflater.from(parent.context), parent, false).apply {
        root.setTag(
            R.id.binding_tag,
            this
        )
    }
) {
    val binding: B by lazy(LazyThreadSafetyMode.NONE) {
        // 延迟加载确保tag已设置
        itemView.getTag(R.id.binding_tag) as B
    }

    override fun bind(item: T) {
        val holder = this
        with(binding) {
            bindFunc(item, holder, null)
        }
    }

    override fun bindWithPayload(item: T, payloads: List<Any>) {
        val holder = this
        with(binding) {
            bindFunc(item, holder, payloads)
        }
    }
}
// endregion

/**
 * 链式配置扩展
 */
fun <T : Any> BaseRecyclerAdapter<T>.withConfig(config: BaseRecyclerAdapter<T>.() -> Unit) = apply {
    config()
}
// endregion

// 使用示例
//class ExampleUsage {
//    data class User(val id: Int, val name: String, val type: Int)
//
//    fun setupRecyclerView(recyclerView: RecyclerView) {
//        // 单类型示例
//        val singleAdapter = SingleTypeAdapter<User>(
//            layoutRes = R.layout.item_user,
//            bindFunction = { holder, item ->
//                // 绑定数据到视图
//                holder.itemView.findViewById<TextView>(R.id.tvName).text = item.name
//            },
//            itemClickListener = { item, position ->
//                // 处理点击事件
//            }
//        )
//
//        // 多类型示例
//        val multiAdapter = MultiTypeAdapter<User>(
//            viewHolderFactory = { parent, viewType ->
//                when (viewType) {
//                    VIEW_TYPE_HEADER -> HeaderViewHolder(parent)
//                    else -> UserViewHolder(parent)
//                }
//            },
//            getItemViewType = { item ->
//                if (item.id == 0) VIEW_TYPE_HEADER else VIEW_TYPE_NORMAL
//            }
//        )
//
//        recyclerView.adapter = singleAdapter
//        singleAdapter.submitList(getUserList())
//    }
//
//    private inner class UserViewHolder(parent: ViewGroup) :
//        BaseViewHolder<User>(parent, R.layout.item_user) {
//        override fun bind(item: User) {
//            // 绑定数据
//        }
//    }
//
//    private inner class HeaderViewHolder(parent: ViewGroup) :
//        BaseViewHolder<User>(parent, R.layout.item_header) {
//        override fun bind(item: User) {
//            // 绑定头部数据
//        }
//    }
//
//    companion object {
//        private const val VIEW_TYPE_HEADER = 0
//        private const val VIEW_TYPE_NORMAL = 1
//    }
//}