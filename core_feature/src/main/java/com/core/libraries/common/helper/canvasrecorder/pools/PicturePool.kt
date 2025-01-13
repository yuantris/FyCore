package com.core.libraries.common.helper.canvasrecorder.pools

import android.graphics.Picture
import com.core.libraries.common.helper.objectpool.BaseObjectPool

class PicturePool : BaseObjectPool<Picture>(64) {

    override fun create(): Picture = Picture()

}
