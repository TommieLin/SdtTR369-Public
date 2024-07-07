package com.sdt.diagnose.Device.X_Skyworth;

import static android.content.Context.NETWORK_STATS_SERVICE;

import android.app.ActivityManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.HardwarePropertiesManager;
import android.os.Message;
import android.os.Process;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;

import androidx.annotation.NonNull;

import com.sdt.diagnose.Device.DeviceInfo.AmlProcessStatusX;
import com.sdt.diagnose.Device.DeviceInfo.RtkProcessStatusX;
import com.sdt.diagnose.Device.Platform.ModelX;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.NetWorkSpeedUtils;
import com.sdt.diagnose.common.NetworkUtils;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.database.DbManager;
import com.sdt.diagnose.extra.CmsExtraServiceManager;
import com.skyworth.scrrtcsrv.Device;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * @Author Outis
 * @Date 2023/11/30 13:48
 * @Version 1.0
 */
public class SystemDataStat {
    private static final String TAG = "SystemDataStat";
    private static Context mContext;
    private final Handler mHandler;
    private final HandlerThread mThread;
    private static int mPeriodicMillisTime = 0;
    private static final int MAX_CACHE_NUM = 144;   // 最大储存量
    private static int CUR_CACHE_NUM = 0;
    private static final int MSG_START_SYSTEM_DATA_STAT = 3305;
    private static final int DEFAULT_PERIOD_MILLIS_TIME = 600000;   // 默认十分钟统计一次
    private static List<JSONObject> mDataStatListMap;
    private long lastRxTotal = 0;
    private long lastTxTotal = 0;
    private static CmsExtraServiceManager mCmsExtraServiceManager;
    private static ModelX.Type mStbModelType = null;

    public SystemDataStat(Context context) {
        mContext = context;
        mDataStatListMap = new ArrayList<>();
        mCmsExtraServiceManager = CmsExtraServiceManager.getInstance(mContext);
        setPeriodicMillisTime();

        String rxTraffic = SystemProperties.get("persist.sys.tr369.total.rx.traffic", "");
        if (rxTraffic.length() != 0 && Integer.parseInt(rxTraffic) >= 0)
            lastRxTotal = Integer.parseInt(rxTraffic);

        String txTraffic = SystemProperties.get("persist.sys.tr369.total.tx.traffic", "");
        if (txTraffic.length() != 0 && Integer.parseInt(txTraffic) >= 0)
            lastTxTotal = Integer.parseInt(txTraffic);

        mThread = new HandlerThread("SystemDataStatThread", Process.THREAD_PRIORITY_BACKGROUND);
        mThread.start();
        mHandler =
                new Handler(mThread.getLooper()) {
                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        if (msg.what == MSG_START_SYSTEM_DATA_STAT) {
                            // 缓存系统数据
                            startSystemDataStatInfo();
                        }
                    }
                };
        mHandler.sendEmptyMessage(MSG_START_SYSTEM_DATA_STAT);
    }

    public static void setPeriodicMillisTime(String time) {
        if (time != null &&
                time.length() != 0 &&
                Integer.parseInt(time) > 0) {
            setPeriodicMillisTime(Integer.parseInt(time) * 1000);
        } else {
            setPeriodicMillisTime(DEFAULT_PERIOD_MILLIS_TIME);
        }
    }

    private static void setPeriodicMillisTime(int time) {
        mPeriodicMillisTime = time;
    }

    private static void setPeriodicMillisTime() {
        mPeriodicMillisTime = DEFAULT_PERIOD_MILLIS_TIME;
        String time = DbManager.getDBParam("Device.X_Skyworth.ArrayRecordInterval");
        // ArrayRecordInterval单位：秒
        if (time.length() != 0 && Integer.parseInt(time) >= 0) {
            mPeriodicMillisTime = Integer.parseInt(time) * 1000;
        }
    }

    public static int getPeriodicMillisTime() {
        return mPeriodicMillisTime / 1000;
    }

    public static double getCpuTemp() {
        HardwarePropertiesManager manager =
                (HardwarePropertiesManager) mContext.getSystemService(
                        Context.HARDWARE_PROPERTIES_SERVICE);
        float[] temps = manager.getDeviceTemperatures(
                HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU,
                HardwarePropertiesManager.TEMPERATURE_CURRENT);
        if (temps.length > 0) {
            return temps[0];
        }
        return 0;
    }

    public static double getCpuUsageRate() {
        double rate = 0;
        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            rate = AmlProcessStatusX.getInstance().getCpuUsageByAml();
        } else if (mStbModelType == ModelX.Type.Realtek) {
            rate = RtkProcessStatusX.getInstance().getCpuUsageByRtk();
        } else {
            try {
                if (null != mCmsExtraServiceManager) {
                    rate = mCmsExtraServiceManager.getCpuUsage();
                } else {
                    LogUtils.e(TAG, "getCpuUsageRate: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "getCpuUsageRate: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }
        return rate;
    }

    public static long getMemoryAvailSizeKb() {
        long availMem = 0;
        try {
            ActivityManager am = (ActivityManager) GlobalContext.getContext().getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            availMem = mi.availMem / 1024;
        } catch (Exception e) {
            LogUtils.e(TAG, "getMemoryIdleSize error: " + e.getMessage());
            return 0;
        }
        LogUtils.d(TAG, "getMemoryAvailSize: " + availMem);
        return availMem;
    }

    public static long getMemoryTotalSizeKb() {
        long totalMem = 0;
        try {
            ActivityManager am = (ActivityManager) GlobalContext.getContext().getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            totalMem = mi.totalMem / 1024;
        } catch (Exception e) {
            LogUtils.e(TAG, "getMemoryTotalSize error: " + e.getMessage());
            return 0;
        }
        LogUtils.d(TAG, "getMemoryTotalSize: " + totalMem);
        return totalMem;
    }

    public static long getInternalDataFreeStorageKb() {
        long freeSpace = 0;
        try {
            final StorageManager sm = GlobalContext.getContext().getSystemService(StorageManager.class);
            final List<VolumeInfo> volumes = sm.getVolumes();

            for (VolumeInfo vol : volumes) {
                if (vol.getType() == VolumeInfo.TYPE_PRIVATE && vol.isMountedReadable()) {
                    final File path = vol.getPath();
                    if (path != null) {
                        freeSpace = path.getFreeSpace() / 1024;
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getInternalDataFreeStorage error: " + e.getMessage());
            return 0;
        }

        LogUtils.d(TAG, "getInternalDataFreeStorage: " + freeSpace);
        return freeSpace;
    }

    public static long getInternalDataTotalStorageKb() {
        long totalSpace = 0;
        try {
            final StorageManager sm = GlobalContext.getContext().getSystemService(StorageManager.class);
            final List<VolumeInfo> volumes = sm.getVolumes();

            for (VolumeInfo vol : volumes) {
                if (vol.getType() == VolumeInfo.TYPE_PRIVATE && vol.isMountedReadable()) {
                    final File path = vol.getPath();
                    if (path != null) {
                        totalSpace = path.getTotalSpace() / 1024;
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getInternalDataTotalStorage error: " + e.getMessage());
            return 0;
        }

        LogUtils.d(TAG, "getInternalDataTotalStorage: " + totalSpace);
        return totalSpace;
    }

    public static int getDownlinkRate() {
        return NetWorkSpeedUtils.calcDownSpeed();
    }

    public static int getUplinkRate() {
        return NetWorkSpeedUtils.calcUpSpeed();
    }

    public static int getWiFiSignalStrength() {
        WifiInfo info = NetworkUtils.getConnectedWifiInfo(mContext);
        return info.getRssi();
    }

    public static int getScreenStatus() {
        return Boolean.compare(Device.isScreenOn(), false);
    }

    public static long getRxTotalTraffic() {
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) GlobalContext.getContext().getSystemService(NETWORK_STATS_SERVICE);
        try {
            NetworkStats.Bucket wifi = networkStatsManager.querySummaryForDevice(
                    ConnectivityManager.TYPE_WIFI, "",
                    getTodayZeroDate().getTime(), System.currentTimeMillis());

            NetworkStats.Bucket ethernet = networkStatsManager.querySummaryForDevice(
                    ConnectivityManager.TYPE_ETHERNET, "",
                    getTodayZeroDate().getTime(), System.currentTimeMillis());
            long wifiUsage = (wifi == null) ? 0 : wifi.getRxBytes();
            long ethernetUsage = (ethernet == null) ? 0 : ethernet.getRxBytes();
            return (wifiUsage + ethernetUsage) / (1024 * 1024);   // 单位：MB
        } catch (Exception e) {
            LogUtils.e(TAG, "getRxTotalBytes error: " + e.getMessage());
            return 0;
        }
    }

    public static long getTxTotalTraffic() {
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) GlobalContext.getContext().getSystemService(NETWORK_STATS_SERVICE);
        try {
            NetworkStats.Bucket wifi = networkStatsManager.querySummaryForDevice(
                    ConnectivityManager.TYPE_WIFI, "",
                    getTodayZeroDate().getTime(), System.currentTimeMillis());

            NetworkStats.Bucket ethernet = networkStatsManager.querySummaryForDevice(
                    ConnectivityManager.TYPE_ETHERNET, "",
                    getTodayZeroDate().getTime(), System.currentTimeMillis());
            long wifiUsage = (wifi == null) ? 0 : wifi.getTxBytes();
            long ethernetUsage = (ethernet == null) ? 0 : ethernet.getTxBytes();
            return (wifiUsage + ethernetUsage) / (1024 * 1024);   // 单位：MB
        } catch (Exception e) {
            LogUtils.e(TAG, "getTxTotalBytes error: " + e.getMessage());
            return 0;
        }
    }

    public static int getWifiSNR(String parm) {
        int pos = -1;
        int wifiSnr = 0;
        String line; // 用来保存每行读取的内容

        InputStream is;
        BufferedReader reader;

        try {
            is = Files.newInputStream(Paths.get("/proc/net/wireless"));
            reader = new BufferedReader(new InputStreamReader(is));
            line = reader.readLine(); // 读取第一行
            while (line != null) {
                LogUtils.d(TAG, "Read a line from the/proc/net/wireless file: " + line);
                pos = line.indexOf(parm);
                if (pos >= 0) {
                    break;
                }
                line = reader.readLine(); // 读取下一行
            }
            reader.close();
            is.close();

            if (pos >= 0) {
                String[] numbers = line.replaceAll("\\.", "").trim().split("\\s+");
                LogUtils.d(TAG, "Interface: " + numbers[0] + ", status: " + numbers[1] +
                        ", link: " + numbers[2] + ", level: " + numbers[3] + ", noise: " + numbers[4]);
                wifiSnr = Integer.parseInt(numbers[3]) - Integer.parseInt(numbers[4]);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getWifiSNR Exception error: " + e.getMessage());
            return 0;
        }

        return wifiSnr;
    }

    public static Date getTodayZeroDate() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        return calendar.getTime();
    }

    public static String getSystemDataStatInfo() {
        if (mDataStatListMap == null) {
            return "";
        }
        String dataStatInfo = mDataStatListMap.toString();
        // 获取完数据后清除
        mDataStatListMap.clear();
        CUR_CACHE_NUM = 0;
        return dataStatInfo;
    }

    /**
     * 进行系统数据的统计收集，数据内容包含以下信息：<ul>
     * <li> timeStamp                - 秒级时间戳 </li>
     * <li> cpuTemperature           - CPU温度 </li>
     * <li> cpuUsagePercent          - CPU占用率(%) </li>
     * <li> memoryUsage              - 内存使用量(kb) </li>
     * <li> memoryUsagePercent       - 内存占用率(%) </li>
     * <li> storageUsage             - 磁盘使用量(kb) </li>
     * <li> storageUsagePercent      - 磁盘占用率(%) </li>
     * <li> downlinkRate             - 下行链路速率(Byte/s) </li>
     * <li> uplinkRate               - 上行链路速率(Byte/s) </li>
     * <li> wifiSignalStrength       - Wifi信号强度 </li>
     * <li> screenInUse              - 0:屏幕没在使用，1:屏幕正在使用 </li>
     * <li> dailyFlow                - 当前时刻所产生的总日流量统计(上传+下载) </li>
     * <li> flowIncr                 - 同比与上一次统计时的总日流量增长量(上传+下载) </li>
     * <li> rxDelta                  - 同比与上一次统计时下载流量的增长量 </li>
     * <li> txDelta                  - 同比与上一次统计时上传流量的增长量 </li>
     * <li> wifiSnr                  - Wifi信噪比 </li>
     */
    private void startSystemDataStatInfo() {
        String timeStamp = String.valueOf(System.currentTimeMillis() / 1000);
        int cpuTemperature = (int) getCpuTemp();
        int cpuUsagePercent = (int) getCpuUsageRate();

        // Memory Usage
        int memoryUsagePercent = 0;
        long totalMem = getMemoryTotalSizeKb();
        long availMem = getMemoryAvailSizeKb();
        long memoryUsage = (totalMem > availMem) ? (totalMem - availMem) : (0);
        try {
            memoryUsagePercent = (int) (100 * memoryUsage / totalMem);
            if (memoryUsagePercent < 0) {
                memoryUsagePercent = 0;
            } else if (memoryUsagePercent > 100) {
                memoryUsagePercent = 100;
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "Set MemoryUsagePercent error: " + e.getMessage());
        }

        // Disk Storage Usage
        int storageUsagePercent = 0;
        long totalSpace = getInternalDataTotalStorageKb();
        long freeSpace = getInternalDataFreeStorageKb();
        long storageUsage = (totalSpace > freeSpace) ? (totalSpace - freeSpace) : (0);
        try {
            storageUsagePercent = (int) (100 * storageUsage / totalSpace);
            if (storageUsagePercent < 0) {
                storageUsagePercent = 0;
            } else if (storageUsagePercent > 100) {
                storageUsagePercent = 100;
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "Set StorageUsagePercent error: " + e.getMessage());
        }

        // WiFi Information
        int downlinkRate = getDownlinkRate();
        int uplinkRate = getUplinkRate();
        int wifiSignalStrength = getWiFiSignalStrength();
        int wifiSnr = getWifiSNR("wlan0");

        // Screen Status
        int screenInUse = getScreenStatus();

        // 当前时刻所产生的下载流量
        long nowRxTotal = getRxTotalTraffic();
        SystemProperties.set("persist.sys.tr369.total.rx.traffic", String.valueOf(nowRxTotal));

        // 当前时刻所产生的上传流量
        long nowTxTotal = getTxTotalTraffic();
        SystemProperties.set("persist.sys.tr369.total.tx.traffic", String.valueOf(nowTxTotal));

        // 更新当日的总流量
        DbManager.setDBParam("Device.X_Skyworth.TotalBytes.Today", String.valueOf(nowRxTotal + nowTxTotal));
        // 计算差值
        long rxDelta = (nowRxTotal > lastRxTotal) ? (nowRxTotal - lastRxTotal) : (0);
        long txDelta = (nowTxTotal > lastTxTotal) ? (nowTxTotal - lastTxTotal) : (0);
        // 记录数据
        lastRxTotal = nowRxTotal;
        lastTxTotal = nowTxTotal;

        if (++CUR_CACHE_NUM > MAX_CACHE_NUM) {
            mDataStatListMap.clear();
            CUR_CACHE_NUM = 0;
        }

        JSONObject params = new JSONObject();
        try {
            params.put("timeStamp", timeStamp);
            params.put("cpuTemperature", cpuTemperature);
            params.put("cpuUsagePercent", cpuUsagePercent);
            params.put("memoryUsage", memoryUsage);
            params.put("memoryUsagePercent", memoryUsagePercent);
            params.put("storageUsage", storageUsage);
            params.put("storageUsagePercent", storageUsagePercent);
            params.put("downlinkRate", downlinkRate);
            params.put("uplinkRate", uplinkRate);
            params.put("wifiSignalStrength", wifiSignalStrength);
            params.put("screenInUse", screenInUse);
            params.put("dailyFlow", nowRxTotal + nowTxTotal);
            params.put("flowIncr", rxDelta + txDelta);
            params.put("rxDelta", rxDelta);
            params.put("txDelta", txDelta);
            params.put("wifiSnr", wifiSnr);
        } catch (Exception e) {
            LogUtils.e(TAG, "JSONObject PUT error: " + e.getMessage());
        }

        LogUtils.d(TAG, "Set SystemDataStatInfo: " + params);
        mDataStatListMap.add(params);

        mHandler.sendEmptyMessageDelayed(MSG_START_SYSTEM_DATA_STAT, mPeriodicMillisTime);
    }
}
