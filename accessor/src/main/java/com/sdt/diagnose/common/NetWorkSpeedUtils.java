package com.sdt.diagnose.common;

import android.net.TrafficStats;

/**
 * @Author Outis
 * @Date 2023/11/30 15:51
 * @Version 1.0
 */
public class NetWorkSpeedUtils {
    private static final String TAG = "NetWorkSpeedUtils";
    private static long lastTotalRxBytes = 0;
    private static long lastTotalTxBytes = 0;
    private static long lastRxTimeStamp = 0;
    private static long lastTxTimeStamp = 0;

    private static long getTotalRxBytes() {
        return TrafficStats.getTotalRxBytes() == TrafficStats.UNSUPPORTED
                ? 0 : (TrafficStats.getTotalRxBytes()); // 单位: KB
    }

    private static long getTotalTxBytes() {
        return TrafficStats.getTotalTxBytes() == TrafficStats.UNSUPPORTED
                ? 0 : (TrafficStats.getTotalTxBytes()); // 单位: KB
    }

    public static int calcDownSpeed() {
        long nowTimeStamp = System.currentTimeMillis();
        long nowTotalRxBytes = getTotalRxBytes();
        if (nowTotalRxBytes <= lastTotalRxBytes) return 0;
        else if (nowTimeStamp <= lastRxTimeStamp) return 0;
        float downSpeed = ((nowTotalRxBytes - lastTotalRxBytes) * 1000f / (nowTimeStamp - lastRxTimeStamp));
        lastTotalRxBytes = nowTotalRxBytes;
        lastRxTimeStamp = nowTimeStamp;
        return (int) downSpeed; // 单位: b/s
    }

    public static int calcUpSpeed() {
        long nowTimeStamp = System.currentTimeMillis();
        long nowTotalTxBytes = getTotalTxBytes();
        if (nowTotalTxBytes <= lastTotalTxBytes) return 0;
        else if (nowTimeStamp <= lastTxTimeStamp) return 0;
        float upSpeed = ((nowTotalTxBytes - lastTotalTxBytes) * 1000f / (nowTimeStamp - lastTxTimeStamp));
        lastTxTimeStamp = nowTimeStamp;
        lastTotalTxBytes = nowTotalTxBytes;
        return (int) upSpeed; // 单位: b/s
    }

}
