package com.sdt.android.tr369.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import static android.content.Intent.ACTION_SCREEN_OFF;
import static android.content.Intent.ACTION_SCREEN_ON;

import com.sdt.diagnose.common.bean.StandbyBean;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.common.net.HttpsUtils;

public class StandbyModeReceiver extends BroadcastReceiver {
    private static final String TAG = "StandbyModeReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_SCREEN_OFF.equals(action)) {
            LogUtils.d(TAG, "Start standby mode, isEnable: " + StandbyBean.getInstance().isEnable());
            if (StandbyBean.getInstance().isEnable()) {
                HttpsUtils.uploadStandbyStatus(0);
            }
        } else if (ACTION_SCREEN_ON.equals(action)) {
            LogUtils.d(TAG, "Stop standby mode");
            if (StandbyBean.getInstance().isEnable()) {
                HttpsUtils.uploadStandbyStatus(1);
            }
        }
    }
}
