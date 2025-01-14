package com.core.libraries.engine.livebus.ipc.core;

import android.os.Bundle;
import com.core.libraries.engine.livebus.ipc.consts.IpcConst;


public class IntProcessor implements Processor {

    @Override
    public boolean writeToBundle(Bundle bundle, Object value) {
        if (!(value instanceof Integer)) {
            return false;
        }
        bundle.putInt(IpcConst.KEY_VALUE, (int) value);
        return true;
    }

    @Override
    public Object createFromBundle(Bundle bundle) {
        return bundle.getInt(IpcConst.KEY_VALUE);
    }
}
