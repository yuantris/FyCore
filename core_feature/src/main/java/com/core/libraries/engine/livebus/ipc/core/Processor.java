package com.core.libraries.engine.livebus.ipc.core;

import android.os.Bundle;

public interface Processor {

    boolean writeToBundle(Bundle bundle, Object value) throws Exception;

    Object createFromBundle(Bundle bundle) throws Exception;
}
