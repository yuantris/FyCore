package io.core.common.base.fragment

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import io.core.common.base.component.activity.BaseActivity

/**
# ██████████
# █▄█████▄█
# █▼▼▼▼▼
# █
# █▲▲▲▲▲
# ██████████
# ██ ██
# 注释的艺术，正在加载……
 * 2025/1/7 16:57
 * @description
 * @author Yuan
 */
abstract class BaseFragment<A : BaseActivity> : Fragment() {

    /** Activity 对象 */
    private var activity: A? = null

    /** 载体 */
    private var container: ViewGroup? = null

    /** 当前是否加载过 */
    private var loading: Boolean = false

    open fun contentViewBind(): View? {
        return null
    }

    @Suppress("UNCHECKED_CAST")
    override fun onAttach(context: Context) {
        super.onAttach(context)
        // 获得全局的 Activity
        activity = requireActivity() as A
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        loading = false
        this.container = container
        val root = contentViewBind()
        initView()
        return root
    }

    override fun onResume() {
        super.onResume()

        if (!loading) {
            loading = true
            initData()
            onFragmentResume(true)
            observers()
            return
        }

        if (this.activity?.lifecycle?.currentState == Lifecycle.State.STARTED) {
            onActivityResume()
        } else {
            onFragmentResume(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        loading = false
    }

    override fun onDetach() {
        super.onDetach()
        activity = null
    }

    /**
     * 这个 Fragment 是否已经加载过了
     */
    open fun isLoading(): Boolean {
        return loading
    }

    /**
     * 获取绑定的 Activity，防止出现 getActivity 为空
     */
    open fun getAttachActivity(): A? {
        return activity
    }

    override fun getContext(): Context? {
        return activity
    }

    open fun getFragmentContainer(): ViewGroup? {
        return container
    }

    protected open fun initView() {}
    protected open fun initData() {}
    protected open fun observers() {}

    /**
     * Fragment 可见回调
     *
     * @param first                 是否首次调用
     */
    protected open fun onFragmentResume(first: Boolean) {}

    /**
     * Activity 可见回调
     */
    protected open fun onActivityResume() {}

}