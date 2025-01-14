package com.core.libraries.engine.livebus.ipc.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.core.libraries.engine.livebus.LiveEventBus;
import com.core.libraries.engine.livebus.ipc.consts.IpcConst;
import com.core.libraries.engine.livebus.ipc.core.ProcessorManager;


public class LebIpcReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (IpcConst.ACTION.equals(intent.getAction())) {
            try {
                String key = intent.getStringExtra(IpcConst.KEY);
                Object value = ProcessorManager.getManager().createFrom(intent);
                if (key != null && value != null) {
                    LiveEventBus.get(key).post(value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
