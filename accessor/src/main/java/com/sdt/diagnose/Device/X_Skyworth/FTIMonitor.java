package com.sdt.diagnose.Device.X_Skyworth;

import android.content.ContentResolver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.log.LogUtils;


/**
 * @Author Outis
 * @Date 2023/11/30 13:51
 * @Version 1.0
 */
public class FTIMonitor {
    private static final String TAG = "FTIMonitor";
    private final Handler mHandler;
    private final HandlerThread mThread;
    private final ContentResolver mResolver;
    private FTIContentObserver mContentObserver = null;
    private static long mTimeSpentAtLastBoot = 0;  // 之前已花费的时间（用于还在FTI阶段但重启后，需要接着上次的时间）
    public static final int MSG_START_MONITOR_FTI_DURATION = 3314;
    public static final int MSG_STOP_MONITOR_FTI_DURATION = 3315;
    private static final int DEFAULT_PERIOD_SECOND_TIME = 60;    // 默认60s监控一次

    public FTIMonitor(ContentResolver resolver) {
        mResolver = resolver;
        mThread = new HandlerThread("FTIMonitorThread", Process.THREAD_PRIORITY_BACKGROUND);
        mThread.start();
        mHandler =
                new Handler(mThread.getLooper()) {
                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        switch (msg.what) {
                            case MSG_START_MONITOR_FTI_DURATION:
                                mHandler.removeMessages(MSG_START_MONITOR_FTI_DURATION);
                                LogUtils.d(TAG, "MSG_START_MONITOR_FTI_DURATION");
                                startMonitorFTIDuration();
                                break;
                            case MSG_STOP_MONITOR_FTI_DURATION:
                                mHandler.removeMessages(MSG_STOP_MONITOR_FTI_DURATION);
                                LogUtils.d(TAG, "MSG_STOP_MONITOR_FTI_DURATION");
                                stopMonitorFTIDuration();
                                break;
                            default:
                                break;
                        }
                    }
                };
        if (!isUserSetupComplete()) {
            LogUtils.i(TAG, "The FTI setup is not completed...");
            initTimeSpent();
            mContentObserver = new FTIContentObserver(mHandler);
            if (mResolver != null) {
                mResolver.registerContentObserver(
                        Settings.Secure.getUriFor(Settings.Secure.USER_SETUP_COMPLETE),
                        false,
                        mContentObserver);
            }
            mHandler.sendEmptyMessage(MSG_START_MONITOR_FTI_DURATION);
        }
    }

    public boolean isUserSetupComplete() {
        return Settings.Secure.getIntForUser(GlobalContext.getContext().getContentResolver(),
                Settings.Secure.USER_SETUP_COMPLETE, 0, UserHandle.USER_CURRENT) != 0;
    }

    public static void initTimeSpent() {
        String timeSpent = SystemProperties.get("persist.sys.tr369.FTI.residence.duration", "0");
        try {
            if (timeSpent.length() != 0 && Long.parseLong(timeSpent) > 0) {
                mTimeSpentAtLastBoot = Long.parseLong(timeSpent);
                return;
            }
        } catch (NumberFormatException e) {
            LogUtils.e(TAG, "initTimeSpent error, " + e.getMessage());
        }
        mTimeSpentAtLastBoot = 0;
    }

    private void startMonitorFTIDuration() {
        updateFTIDuration();

        if (isUserSetupComplete()) {
            mHandler.sendEmptyMessage(MSG_STOP_MONITOR_FTI_DURATION);
        } else {
            mHandler.sendEmptyMessageDelayed(MSG_START_MONITOR_FTI_DURATION, DEFAULT_PERIOD_SECOND_TIME * 1000);
        }
    }

    private void stopMonitorFTIDuration() {
        mHandler.removeMessages(MSG_START_MONITOR_FTI_DURATION);
        updateFTIDuration();
        if (mResolver != null) {
            mResolver.unregisterContentObserver(mContentObserver);
        }
    }

    private void updateFTIDuration() {
        // 本次开机后在FTI阶段花费的时长（单位：秒）
        long timeSpentAtThisBoot = SystemClock.elapsedRealtime() / 1000;
        // 此设备在FTI阶段花费的总时长（单位：秒）
        long duration = mTimeSpentAtLastBoot + timeSpentAtThisBoot;
        SystemProperties.set("persist.sys.tr369.FTI.residence.duration", String.valueOf(duration));
        LogUtils.d(TAG, "Last FTI stay: " + mTimeSpentAtLastBoot
                + ", current FTI stay: " + timeSpentAtThisBoot
                + ", total FTI stay: " + duration);
    }
}
