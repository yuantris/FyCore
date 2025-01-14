package io.core.common.helper.canvasrecorder.pools

import android.graphics.Picture
import io.core.common.helper.objectpool.BaseObjectPool

class PicturePool : BaseObjectPool<Picture>(64) {

    override fun create(): Picture = Picture()

}
