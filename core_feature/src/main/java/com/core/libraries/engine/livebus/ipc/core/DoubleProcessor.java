package com.core.libraries.engine.livebus.ipc.core;

import android.os.Bundle;
import com.core.libraries.engine.livebus.ipc.consts.IpcConst;

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
