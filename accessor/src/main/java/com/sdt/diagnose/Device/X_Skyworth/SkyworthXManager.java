package com.sdt.diagnose.Device.X_Skyworth;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.media.AudioManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.HardwarePropertiesManager;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;

import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.log.LogUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class SkyworthXManager {
    private static final String TAG = "SkyworthXManager";
    private static SkyworthXManager instance = null;
    private final Context mContext = GlobalContext.getContext();
    final StorageManager sm = mContext.getSystemService(StorageManager.class);

    private SkyworthXManager() {
    }

    public static SkyworthXManager getInstance() {
        synchronized (SkyworthXManager.class) {
            if (instance == null) {
                instance = new SkyworthXManager();
            }
        }
        return instance;
    }

    public List<VolumeInfo> getVolumeInfos() {
        final List<VolumeInfo> volumes = sm.getVolumes();
        if (volumes == null || volumes.size() <= 0) {
            return null;
        }
        volumes.sort(VolumeInfo.getDescriptionComparator());
        return volumes;
    }

    public String getInternalDataStorageFree() {
        try {
            final StorageManager sm = mContext.getSystemService(StorageManager.class);
            final List<VolumeInfo> volumes = sm.getVolumes();

            for (VolumeInfo vol : volumes) {
                if (vol.getType() == VolumeInfo.TYPE_PRIVATE && vol.isMountedReadable()) {
                    final File path = vol.getPath();
                    if (path != null) {
                        long free = path.getFreeSpace();
                        return String.valueOf(free / 1024);
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getInternalDataStorageFree error, " + e.getMessage());
        }

        return "";
    }

    public String getInternalDataStorageTotal() {
        try {
            final StorageManager sm = mContext.getSystemService(StorageManager.class);
            final List<VolumeInfo> volumes = sm.getVolumes();

            for (VolumeInfo vol : volumes) {
                if (vol.getType() == VolumeInfo.TYPE_PRIVATE && vol.isMountedReadable()) {
                    final File path = vol.getPath();
                    if (path != null) {
                        long total = path.getTotalSpace();
                        return String.valueOf(total / 1024);
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getInternalDataStorageTotal error, " + e.getMessage());
        }
        return "";
    }

    public String getInternalDataStorageUtilisation() {
        try {
            final StorageManager sm = mContext.getSystemService(StorageManager.class);
            final List<VolumeInfo> volumes = sm.getVolumes();

            for (VolumeInfo vol : volumes) {
                if (vol.getType() == VolumeInfo.TYPE_PRIVATE && vol.isMountedReadable()) {
                    final File path = vol.getPath();
                    if (path != null) {
                        double total = path.getTotalSpace();
                        double free = path.getFreeSpace();
                        double utilisation = (total - free) * 100 / total;
                        return String.format("%.2f", utilisation);
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getInternalDataStorageUtilisation error, " + e.getMessage());
        }

        return "";
    }

    public String getMemoryUtilisation() {
        String result = "";
        try {
            ActivityManager am =
                    (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            double usage = mi.totalMem - mi.availMem;
            double utilisation = usage * 100f / mi.totalMem;
            result = String.format("%.2f", utilisation);
        } catch (Exception e) {
            LogUtils.e(TAG, "getMemoryUtilisation error, " + e.getMessage());
        }
        return result;
    }

    public boolean mute() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager.isStreamMute(AudioManager.STREAM_MUSIC)) {
            LogUtils.d(TAG, "The device has been muted.");
            return false;
        }
        LogUtils.d(TAG, "Mute now.");
        int flags = AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI;
        audioManager.adjustSuggestedStreamVolume(
                AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.STREAM_MUSIC, flags);
        return true;
    }

    public boolean unmute() {
        AudioManager audioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
        if (!audioManager.isStreamMute(AudioManager.STREAM_MUSIC)) {
            LogUtils.d(TAG, "The device has been unmute.");
            return false;
        }
        LogUtils.d(TAG, "Unmute now.");
        // int flags = AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND;
        int flags = AudioManager.FLAG_SHOW_UI | AudioManager.FLAG_PLAY_SOUND;
        audioManager.adjustSuggestedStreamVolume(
                AudioManager.ADJUST_TOGGLE_MUTE, AudioManager.STREAM_MUSIC, flags);
        return true;
    }

    public boolean uninstall(String packageName) {
        LogUtils.d(TAG, "UninstallApp pkg: " + packageName);
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent sender = PendingIntent.getActivity(mContext, 0, intent, 0);
        PackageInstaller mPackageInstaller = mContext.getPackageManager().getPackageInstaller();
        // 卸载APK
        mPackageInstaller.uninstall(packageName, sender.getIntentSender());
        return true;
    }

    public String getWiFiRFSNR() {
        int pos = -1;
        int i = -1, len = -1;
        int findNumber = 0, snrInt = 0;
        String line; // 用来保存每行读取的内容
        String targetStr = "signal_qual:";
        InputStream is;
        BufferedReader reader;

        try {
            is = Files.newInputStream(Paths.get("/proc/net/rtl88x2cs/wlan0/rx_signal"));
            reader = new BufferedReader(new InputStreamReader(is));
            line = reader.readLine(); // 读取第一行
            LogUtils.d(TAG, "SNR line: " + line);
            while (line != null) { // 如果 line 为空说明读完了
                pos = line.indexOf(targetStr);
                if (pos >= 0) {
                    break;
                }
                line = reader.readLine(); // 读取下一行
                LogUtils.d(TAG, "SNR line: " + line);
            }
            reader.close();
            is.close();

            if (pos >= 0) {
                len = line.length();
                i = pos + targetStr.length();
                while (i < len) {
                    if ((line.charAt(i) >= '0') && (line.charAt(i) <= '9')) {
                        snrInt = snrInt * 10 + line.charAt(i) - '0';
                        findNumber = 1;
                    } else if (1 == findNumber) {
                        // 数字字符后面出现非数字的，认为已经结束
                        break;
                    }
                    i++;
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getWiFiRFSNR error, " + e.getMessage());
        }

        return String.valueOf(snrInt);
    }

    public String getWiFiSecureConnection() {
        String str = "unknown";
        WifiManager mWifiManager =
                (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = mWifiManager.getConnectionInfo(); // 当前wifi连接信息
        List<ScanResult> scanResults = mWifiManager.getScanResults();

        for (ScanResult result : scanResults) {
            if (result.BSSID.equalsIgnoreCase(wifiInfo.getBSSID())
                    && result.SSID.equalsIgnoreCase(
                    wifiInfo.getSSID().substring(1, wifiInfo.getSSID().length() - 1))) {
                if (result.capabilities.contains("WEP")) {
                    str = "WEP";
                } else if (result.capabilities.contains("PSK")) {
                    str = "PSK";
                } else if (result.capabilities.contains("EAP")) {
                    str = "EAP";
                } else {
                    str = "unsecure";
                }
                break;
            }
        }
        return str;
    }

    public String getWifiPHYType() {
        int pos = -1;
        int i = -1, len = -1;

        String phyStr = "unknown";
        String line; // 用来保存每行读取的内容
        String targetStr = "rx_rate = ";
        InputStream is;
        BufferedReader reader;

        try {
            is = Files.newInputStream(Paths.get("/proc/net/rtl88x2cs/wlan0/trx_info_debug"));
            reader = new BufferedReader(new InputStreamReader(is));
            line = reader.readLine(); // 读取第一行
            LogUtils.d(TAG, "HYType line: " + line);
            while (line != null) { // 如果 line 为空说明读完了
                pos = line.indexOf(targetStr);
                if (pos >= 0) {
                    break;
                }
                line = reader.readLine(); // 读取下一行
                LogUtils.d(TAG, "HYType line: " + line);
            }
            reader.close();
            is.close();
            if (pos >= 0) {
                len = line.length();
                i = pos + targetStr.length();
                phyStr = "";
                while (i < len) {
                    if (line.charAt(i) != ' ') {
                        phyStr = phyStr + line.charAt(i);
                    } else {
                        // rx_rate = OFDM_6M
                        break;
                    }
                    i++;
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getWifiPHYType error, " + e.getMessage());
        }

        return phyStr;
    }

    public String getWiFiMIMOMode() {
        int pos = -1, mimo_type = -1;
        int i = 0, len = 0, findNumber = 0;
        String mimoStr;
        String line; // 用来保存每行读取的内容
        String targetStr = "mimo_type";
        InputStream is;
        BufferedReader reader;

        try {
            is = Files.newInputStream(Paths.get("/proc/net/rtl88x2cs/wlan0/trx_info_debug"));
            reader = new BufferedReader(new InputStreamReader(is));
            line = reader.readLine(); // 读取第一行
            while (line != null) { // 如果 line 为空说明读完了
                pos = line.indexOf(targetStr);
                if (pos >= 0) {
                    break;
                }
                line = reader.readLine(); // 读取下一行
            }
            reader.close();
            is.close();

            if (pos >= 0) {
                len = line.length();
                i = pos + targetStr.length();
                mimo_type = 0;
                while (i < len) {
                    if ((line.charAt(i) >= '0') && (line.charAt(i) <= '9')) {
                        mimo_type = mimo_type * 10 + line.charAt(i) - '0';
                        findNumber = 1;
                    } else if (1 == findNumber) {
                        // 数字字符后面出现非数字的，认为已经结束
                        break;
                    }
                    i++;
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getWiFiMIMOMode error, " + e.getMessage());
        }

        mimoStr = ConvertMimoTypeIntToString_rtl88x2cs(mimo_type);

        return mimoStr;
    }

    private String ConvertMimoTypeIntToString_rtl88x2cs(int mimo_type) {
        String retStr;

        final int MIMO_TYPE_RF_1T1R = 0;
        final int MIMO_TYPE_RF_1T2R = 1;
        final int MIMO_TYPE_RF_2T2R = 2;
        final int MIMO_TYPE_RF_2T3R = 3;
        final int MIMO_TYPE_RF_2T4R = 4;
        final int MIMO_TYPE_RF_3T3R = 5;
        final int MIMO_TYPE_RF_3T4R = 6;
        final int MIMO_TYPE_RF_4T4R = 7;
        switch (mimo_type) {
            case MIMO_TYPE_RF_1T1R:
                retStr = "RF_1T1R";
                break;

            case MIMO_TYPE_RF_1T2R:
                retStr = "RF_1T2R";
                break;

            case MIMO_TYPE_RF_2T2R:
                retStr = "RF_2T2R";
                break;

            case MIMO_TYPE_RF_2T3R:
                retStr = "RF_2T3R";
                break;

            case MIMO_TYPE_RF_2T4R:
                retStr = "RF_2T4R";
                break;

            case MIMO_TYPE_RF_3T3R:
                retStr = "RF_3T3R";
                break;

            case MIMO_TYPE_RF_3T4R:
                retStr = "RF_3T4R";
                break;

            case MIMO_TYPE_RF_4T4R:
                retStr = "RF_4T4R";
                break;

            default:
                retStr = "unknown";
                break;
        }

        return retStr;
    }

    public String getCpuTemperature() {
        HardwarePropertiesManager manager =
                (HardwarePropertiesManager) mContext.getSystemService(
                        Context.HARDWARE_PROPERTIES_SERVICE);
        float[] temps = manager.getDeviceTemperatures(
                HardwarePropertiesManager.DEVICE_TEMPERATURE_CPU,
                HardwarePropertiesManager.TEMPERATURE_CURRENT);
        if (temps.length > 0) {
            return String.valueOf((int) temps[0]);
        }
        return "0";
    }
}
