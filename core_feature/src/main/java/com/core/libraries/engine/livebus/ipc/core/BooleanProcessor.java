package com.core.libraries.engine.livebus.ipc.core;

import android.os.Bundle;
import com.core.libraries.engine.livebus.ipc.consts.IpcConst;


public class BooleanProcessor implements Processor {

    @Override
    public boolean writeToBundle(Bundle bundle, Object value) {
        if (!(value instanceof Boolean)) {
            return false;
        }
        bundle.putBoolean(IpcConst.KEY_VALUE, (boolean) value);
        return true;
    }

    @Override
    public Object createFromBundle(Bundle bundle) {
        return bundle.getBoolean(IpcConst.KEY_VALUE);
    }
}
