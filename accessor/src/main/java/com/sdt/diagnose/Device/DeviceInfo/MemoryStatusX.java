package com.sdt.diagnose.Device.DeviceInfo;

import android.app.ActivityManager;
import android.content.Context;

import com.sdt.annotations.Tr369Get;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.log.LogUtils;


public class MemoryStatusX {
    private static final String TAG = "MemoryStatusX";

    @Tr369Get("Device.DeviceInfo.MemoryStatus.Total")
    public String SK_TR369_GetMemoryStatusTotal() {
        String result = "";
        try {
            ActivityManager am = (ActivityManager) GlobalContext.getContext().getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            result = String.valueOf(mi.totalMem / 1024);
        } catch (Exception e) {
            LogUtils.e(TAG, "GetMemoryStatusTotal error, " + e.getMessage());
        }

        LogUtils.d(TAG, "GetMemoryStatusTotal result: " + result);
        return result;
    }

    @Tr369Get("Device.DeviceInfo.MemoryStatus.Free")
    public String SK_TR369_GetMemoryStatusFree() {
        String result = "";
        try {
            ActivityManager am = (ActivityManager) GlobalContext.getContext().getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            result = String.valueOf(mi.availMem / 1024);
        } catch (Exception e) {
            LogUtils.e(TAG, "GetMemoryStatusFree error, " + e.getMessage());
            return "-1";
        }
        LogUtils.d(TAG, "GetMemoryStatusFree result: " + result);
        return result;
    }


}
