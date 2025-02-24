package com.core.fy.android.help.canvasrecorder.pools

import android.graphics.Picture
import io.core.common.helper.pool.BaseObjectPool

class PicturePool : BaseObjectPool<Picture>(64) {

    override fun create(): Picture = Picture()

}
