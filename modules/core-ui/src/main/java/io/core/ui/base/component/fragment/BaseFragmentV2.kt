package io.core.ui.base.component.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import io.core.ui.helper.track.FragmentVisibilityDetectorV2


abstract class BaseFragmentV2 : Fragment(), FragmentVisibilityDetectorV2.VisibilityCallback {

    /** 载体 */
    private var container: ViewGroup? = null

    /** 当前是否加载�?*/
    private var loading: Boolean = false

    private var visibilityDetectorV2: FragmentVisibilityDetectorV2? = null

    open fun contentViewBind(): View? {
        return null
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        loading = false
        this.container = container
        this.visibilityDetectorV2 =
            FragmentVisibilityDetectorV2.attachToFragment(this, callback = this)
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

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        visibilityDetectorV2?.onHiddenChanged(hidden)
    }

    @Deprecated("Deprecated in Java")
    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        visibilityDetectorV2?.onUserVisibleHintChanged(isVisibleToUser)
    }

    override fun onVisibilityChanged(visible: Boolean) {
        if (visible) {
            onFragmentVisible()
        }
    }

    /**
     * 这个 Fragment 是否已经加载过了
     */
    open fun isLoading(): Boolean {
        return loading
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
     * Fragment 可见回调V2
     */
    protected open fun onFragmentVisible() {}

    /**
     * Activity 可见回调
     */
    protected open fun onActivityResume() {}


}