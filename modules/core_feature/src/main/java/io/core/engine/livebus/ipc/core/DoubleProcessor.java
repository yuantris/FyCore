package io.core.engine.livebus.ipc.core;

import android.os.Bundle;
import io.core.engine.livebus.ipc.consts.IpcConst;

public class DoubleProcessor implements Processor {

    @Override
    public boolean writeToBundle(Bundle bundle, Object value) {
        if (!(value instanceof Double)) {
            return false;
        }
        bundle.putDouble(IpcConst.KEY_VALUE, (Double) value);
        return true;
    }

    @Override
    public Object createFromBundle(Bundle bundle) {
        return bundle.getDouble(IpcConst.KEY_VALUE);
    }
}
