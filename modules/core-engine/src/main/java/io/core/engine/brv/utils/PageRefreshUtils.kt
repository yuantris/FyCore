package io.core.engine.brv.utils

import android.view.View
import android.view.ViewGroup
import io.core.engine.brv.PageRefreshLayout


/**
 * 创建一个[PageRefreshLayout]来包裹视�?
 * 但是更建议在XML布局中创建[PageRefreshLayout], 可保持代码可读性且避免不必要的问题发生, 性能也更�?
 *
 * @param loadMoreEnabled 启用上拉加载
 * @param stateEnabled 启用缺省�?
 */
fun View.pageCreate(
    loadMoreEnabled: Boolean = true,
    stateEnabled: Boolean = true
): PageRefreshLayout {
    val pageRefreshLayout = PageRefreshLayout(context)

    val parent = parent as ViewGroup
    pageRefreshLayout.id = id
    val index = parent.indexOfChild(this)
    val layoutParams = layoutParams

    parent.removeView(this)
    pageRefreshLayout.setRefreshContent(this@pageCreate)
    parent.addView(pageRefreshLayout, index, layoutParams)
    pageRefreshLayout.setEnableLoadMore(loadMoreEnabled)
    pageRefreshLayout.stateEnabled = stateEnabled
    pageRefreshLayout.initialize()

    return pageRefreshLayout
}