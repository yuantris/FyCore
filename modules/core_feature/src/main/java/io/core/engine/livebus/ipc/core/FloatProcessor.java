package io.core.engine.livebus.ipc.core;

import android.os.Bundle;

import io.core.engine.livebus.ipc.consts.IpcConst;

public class FloatProcessor implements Processor {

    @Override
    public boolean writeToBundle(Bundle bundle, Object value) {
        if (!(value instanceof Float)) {
            return false;
        }
        bundle.putFloat(IpcConst.KEY_VALUE, (float) value);
        return true;
    }

    @Override
    public Object createFromBundle(Bundle bundle) {
        return bundle.getFloat(IpcConst.KEY_VALUE);
    }
}
