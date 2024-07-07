package com.sdt.diagnose.Device.X_Skyworth;

import com.sdt.diagnose.common.log.LogUtils;

/**
 * @Author Outis
 * @Date 2023/11/30 10:32
 * @Version 1.0
 */
public abstract class LogThread extends Thread {
    private static final String TAG = "LogThread";
    private volatile boolean suspend = false;
    private final String control = ""; // 只是需要一个对象而已，这个对象没有实际意义

    public void setSuspend(boolean suspend) {
        this.suspend = suspend;
        if (!suspend) {
            synchronized (control) {
                control.notifyAll();
            }
        }
    }

    public boolean isSuspend() {
        return this.suspend;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            this.getLog();
        }
        LogUtils.d(TAG, "Thread is finished.");
    }

    public abstract void getLog();
}
