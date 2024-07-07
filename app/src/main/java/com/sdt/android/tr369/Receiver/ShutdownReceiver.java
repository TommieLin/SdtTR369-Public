package com.sdt.android.tr369.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sdt.diagnose.Tr369PathInvoke;
import com.sdt.diagnose.common.DeviceInfoUtils;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.common.net.HttpsUtils;
import com.sdt.diagnose.database.DbManager;

import java.util.HashMap;

/**
 * @Author Outis
 * @Date 2022/7/12 14:04
 * @Version 1.0
 */
public class ShutdownReceiver extends BroadcastReceiver {
    private static final String TAG = "ShutdownReceiver";
    private final String path = "/acs/offline";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.ACTION_SHUTDOWN")) {
            LogUtils.d(TAG, "ACTION_SHUTDOWN");
            HashMap<String, String> params = new HashMap<>();
            String serialNumber = DeviceInfoUtils.getSerialNumber();
            params.put("serialNumber", serialNumber);
            String url = Tr369PathInvoke.getInstance().getString("Device.X_Skyworth.ManagementServer.Url");
            if (!url.isEmpty()) {
                HttpsUtils.noticeResponse(url + path, params);
            } else {
                LogUtils.e(TAG, "ManagementServer URL is empty.");
            }
        }
    }
}
