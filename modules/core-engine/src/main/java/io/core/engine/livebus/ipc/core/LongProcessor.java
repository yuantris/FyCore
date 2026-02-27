package io.core.engine.livebus.ipc.core;

import android.os.Bundle;

import io.core.engine.livebus.ipc.consts.IpcConst;


public class LongProcessor implements Processor {

    @Override
    public boolean writeToBundle(Bundle bundle, Object value) {
        if (!(value instanceof Long)) {
            return false;
        }
        bundle.putLong(IpcConst.KEY_VALUE, (long) value);
        return true;
    }

    @Override
    public Object createFromBundle(Bundle bundle) {
        return bundle.getLong(IpcConst.KEY_VALUE);
    }
}
