package io.core.engine.location

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import java.io.Closeable

class LocationKit(
    config: LocationConfig = LocationConfig()
) : DefaultLifecycleObserver, Closeable {

    val provider: LocationProvider = LocationProvider(config)
    val tracker: LocationTracker = LocationTracker(config)

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        release()
    }

    override fun close() {
        release()
    }

    fun release() {
        provider.cancel()
        tracker.cancel()
    }
}
