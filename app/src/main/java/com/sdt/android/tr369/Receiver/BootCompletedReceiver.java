package com.sdt.android.tr369.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemProperties;

import com.sdt.android.tr369.SdtTr369Service;
import com.sdt.diagnose.common.log.LogUtils;

public class BootCompletedReceiver extends BroadcastReceiver {
    private final static String TAG = "BootCompletedReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtils.d(TAG, "onReceive action: " + action);
        if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
            // 启动SdtTr369Service
            context.startForegroundService(new Intent(context, SdtTr369Service.class));
            // 记录重启次数
            int rebootCount = 0;
            String numbersBeforeReboot = SystemProperties.get("persist.sys.tr369.reboot.numbers", "");
            if (numbersBeforeReboot.length() != 0 &&
                    Integer.parseInt(numbersBeforeReboot) >= 0) {
                rebootCount = Integer.parseInt(numbersBeforeReboot);
            }
            SystemProperties.set("persist.sys.tr369.reboot.numbers", String.valueOf(++rebootCount));
        }
    }
}
