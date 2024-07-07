package com.sdt.diagnose.Device.DeviceInfo;

import com.realtek.hardware.RtkVoutUtilManager;
import com.sdt.diagnose.common.log.LogUtils;

public class RtkProcessStatusX {
    private static final String TAG = "RtkProcessStatusX";
    private static RtkProcessStatusX mRtkProcessStatusX;

    RtkProcessStatusX() {
    }

    public static RtkProcessStatusX getInstance() {
        if (null == mRtkProcessStatusX) {
            mRtkProcessStatusX = new RtkProcessStatusX();
        }
        return mRtkProcessStatusX;
    }

    public double getCpuUsageByRtk() {
        double usage = 0;
        try {
            RtkVoutUtilManager manager = new RtkVoutUtilManager();
            usage = (double) manager.getProcStat();
        } catch (NoClassDefFoundError e) {
            LogUtils.e(TAG, "getCpuUsageByRtk: RtkVoutUtilManager call failed, " + e.getMessage());
        }
        LogUtils.d(TAG, "getCpuUsageByRtk usage: " + usage);
        return usage;
    }
}
