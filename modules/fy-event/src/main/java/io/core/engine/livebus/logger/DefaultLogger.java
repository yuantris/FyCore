package io.core.engine.livebus.logger;

import android.util.Log;

import java.util.logging.Level;


public class DefaultLogger implements Logger {

    public static final String TAG = "[LiveBus]";

    @Override
    public void log(Level level, String msg) {
        if (level == Level.SEVERE) {
            Log.e(TAG, msg);
        } else if (level == Level.WARNING) {
            Log.w(TAG, msg);
        } else if (level == Level.INFO) {
            Log.i(TAG, msg);
        } else if (level == Level.CONFIG) {
            Log.d(TAG, msg);
        } else if (level != Level.OFF) {
            Log.v(TAG, msg);
        }
    }

    @Override
    public void log(Level level, String msg, Throwable th) {
        if (level == Level.SEVERE) {
            Log.e(TAG, msg, th);
        } else if (level == Level.WARNING) {
            Log.w(TAG, msg, th);
        } else if (level == Level.INFO) {
            Log.i(TAG, msg, th);
        } else if (level == Level.CONFIG) {
            Log.d(TAG, msg, th);
        } else if (level != Level.OFF) {
            Log.v(TAG, msg, th);
        }
    }
}
