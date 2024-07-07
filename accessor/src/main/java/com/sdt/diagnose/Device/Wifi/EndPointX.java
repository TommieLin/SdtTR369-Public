package com.sdt.diagnose.Device.Wifi;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.sdt.annotations.Tr369Get;
import com.sdt.diagnose.Device.Wifi.NeighboringWiFiDiagnostic.Result;
import com.sdt.diagnose.Device.X_Skyworth.SystemDataStat;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.NetworkUtils;
import com.sdt.diagnose.common.ProtocolPathUtils;
import com.sdt.diagnose.common.bean.NetworkStatisticsInfo;
import com.sdt.diagnose.common.bean.ScanedWifiInfo;
import com.sdt.diagnose.common.log.LogUtils;

import java.util.List;

/**
 * 盒子当前连接上的wifi
 * This class for protocol:
 * Device.WiFi.EndPoint.1.Alias                         - WIFI  SSID
 * Device.WiFi.EndPoint.1.ProfileReference              - WIFI  BSSID
 * Device.WiFi.EndPoint.1.SSIDReference                 - WIFI  BSSID
 * Device.WiFi.EndPoint.1.Enable                        - WIFI  BSSID
 * Device.WiFi.EndPoint.1.Status                        - WIFI  BSSID
 * Device.WiFi.EndPoint.1.ProfileNumberOfEntries              - 默认是1
 * <p>
 * Device.WiFi.EndPoint.1.Stats
 * Device.WiFi.EndPoint.1.Stats.1.LastDataDownlinkRate      - WIFI 下行速度
 * Device.WiFi.EndPoint.1.Stats.1.LastDataUplinkRate             - WIFI  上行速度
 * Device.WiFi.EndPoint.1.Stats.1.SignalStrength              - WIFI 信号强度
 * Device.WiFi.EndPoint.1.Stats.1.Retransmissions              -
 * <p>
 * Device.WiFi.EndPoint.1.Security.ModesSupported              - WIFI  加密模式
 * <p>
 * Device.WiFi.EndPoint.1.Profile              - WIFI  配置
 * Device.WiFi.EndPoint.1.Profile.1.Enable              - 默认true
 * Device.WiFi.EndPoint.1.Profile.1.Status                - 默认 active
 * Device.WiFi.EndPoint.1.Profile.1.Alias              - WIFI  BSSID
 * Device.WiFi.EndPoint.1.Profile.1.SSID              - WIFI  BSSID
 * Device.WiFi.EndPoint.1.Profile.1.Location              - 默认Neighbor House
 * Device.WiFi.EndPoint.1.Profile.1.Priority              - WIFI  优先级
 * <p>
 * <p>
 * Device.WiFi.EndPoint.1.AC.1.Stats.BytesSent              - WIFI 发送的字节数
 * Device.WiFi.EndPoint.1.AC.1.Stats.BytesReceived              - WIFI  接收的字节数
 * Device.WiFi.EndPoint.1.AC.1.Stats.PacketsSent              - WIFI  发送的包数量
 * Device.WiFi.EndPoint.1.AC.1.Stats.PacketsReceived              - WIFI  接收的包数量
 * Device.WiFi.EndPoint.1.AC.1.Stats.ErrorsSent              -
 * Device.WiFi.EndPoint.1.AC.1.Stats.ErrorsReceived              -
 * Device.WiFi.EndPoint.1.AC.1.Stats.DiscardPacketsSent              - WIFI 丢弃的发送包数量
 * Device.WiFi.EndPoint.1.AC.1.Stats.DiscardPacketsReceived              - WIFI  丢弃的接收包数量
 * Device.WiFi.EndPoint.1.AC.1.Stats.RetransCount              - WIFI 重试报文的总数
 * Device.WiFi.EndPoint.1.AC.1.Stats.OutQLenHistogram              -
 */
public class EndPointX {
    private static final String TAG = "EndPointX";
    private final static String REFIX = "Device.WiFi.EndPoint.";

    @RequiresApi(api = Build.VERSION_CODES.Q)
    @Tr369Get("Device.WiFi.EndPoint.")
    public String SK_TR369_GetEndPointInfo(String path) {
        String[] params = ProtocolPathUtils.parse(REFIX, path);
        if (params == null || params.length < 1) {
            return null;
        }
        int index = 0;
        try {
            index = Integer.parseInt(params[0]);
            if (index != 1) {
                //Todo report error.
                return null;
            }
        } catch (NumberFormatException e) {
            //Todo report error.
            return null;
        }

        if (params.length < 2) {
            return null;
        }
        String secondParam = params[1];
        if (TextUtils.isEmpty(secondParam)) {
            //Todo report error.
            return null;
        }
        Context context = GlobalContext.getContext();
        switch (secondParam) {
            case "Alias":
            case "ProfileReference":
            case "SSIDReference":
                WifiInfo wifiInfo = NetworkUtils.getConnectedWifiInfo(context);
                if (wifiInfo != null) {
                    if (wifiInfo.getSSID().equals("<unknown ssid>")) {
                        return "unknown ssid";
                    }
                    return wifiInfo.getSSID();
                }
                return null;
            case "Enable":
                return "true";
            case "Status":
                return "Enabled";
            case "ProfileNumberOfEntries":
                return "1";
            case "Stats":
                return getEndpointStatsParam(context, params);
            case "Security":
                return getEndpointSecurityParam(this.getActiveScanedWifiInfo(), params);
            case "Profile":
                return getEndpointProfileParam(context, params);
            case "AC":
                return getEndpointACParam(params);
            case "WifiStandard":
                return getWifiStandard(context);
            default:
                break;
        }
        return String.valueOf(0);
    }

    String[] wifiStandard =
            new String[]{
                    "unknown",
                    "802.11a/b/g",
                    "",
                    "",
                    "802.11n",
                    "802.11ac",
                    "802.11ax"
            };

    public String getWifiStandard(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        try {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            int index = wifiInfo.getWifiStandard();
             LogUtils.d(TAG, "The WiFi standard is: " + wifiStandard[index] + ", index: " + index);
            return wifiStandard[index];
        } catch (Exception e) {
            return wifiStandard[0];
        }
    }

    private String getEndpointSecurityParam(ScanedWifiInfo scanedWifiInfo, String[] params) {
        if (params.length < 3) {
            return null;
        }
        String thirdParam = params[2];
        if (TextUtils.equals(thirdParam, "ModesSupported")) {
            return scanedWifiInfo != null ? scanedWifiInfo.securityModeEnabled : "null";
        }
        return null;
    }

    private ScanedWifiInfo getActiveScanedWifiInfo() {
        if (!NetworkUtils.isWifiConnected(GlobalContext.getContext())) return null;
        List<ScanedWifiInfo> scans = Result.getScanWifiInfos();
        ScanedWifiInfo scanedWifiInfo = null;
        if (scans != null && scans.size() > 0) {
            for (ScanedWifiInfo scan : scans) {
                if (scan.isActive()) {
                    scanedWifiInfo = scan;
                    break;
                }
            }
        }
        return scanedWifiInfo;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private String getEndpointStatsParam(Context context, String[] params) {
        if (params == null || params.length < 3 || context == null) return null;
        String thirdParam = params[2];
        // 默认就1个
        if (!TextUtils.equals(params[0], "1"))
            return null;
        WifiInfo info = NetworkUtils.getConnectedWifiInfo(context);
        switch (thirdParam) {
            case "LastDataDownlinkRate":
                int rx = info.getRxLinkSpeedMbps();
                if (rx < 0)
                    rx = 0;
                return String.valueOf(rx * 1000); // MB 转 KB
            case "LastDataUplinkRate":
                int tx = info.getTxLinkSpeedMbps();
                if (tx < 0)
                    return "0";
                return String.valueOf(tx * 1000);// MB 转 KB
            case "SignalStrength":
                return String.valueOf(info.getRssi());
            case "Retransmissions":
                return "0";
            case "X_SKYW_SNR":
                return String.valueOf(SystemDataStat.getWifiSNR("wlan0"));
            default:
                break;
        }
        return null;
    }

    private String getEndpointProfileParam(Context context, String[] params) {
        if (params.length < 4 || context == null) {
            return null;
        }
        String thirdParam = params[2];
        // 默认就1个
        if (!TextUtils.equals(thirdParam, "1"))
            return null;
        String forthParam = params[3];
        switch (forthParam) {
            case "Enable":
                return "true";
            case "Status":
                return "Active";
            case "SSID":
            case "Alias":
                WifiInfo wifiInfo = NetworkUtils.getConnectedWifiInfo(context);
                return (wifiInfo != null) ? wifiInfo.getSSID() : null;
            case "Priority":
                ScanedWifiInfo scanedWifiInfo = getActiveScanedWifiInfo();
                if (scanedWifiInfo == null) return "0";
                return scanedWifiInfo.mConfig != null ? String.valueOf(scanedWifiInfo.mConfig.priority) : "0";
            case "Location":
                return "Neighbor House";
            default:
                break;
        }
        return null;
    }

    private String getEndpointACParam(String[] params) {
        if (params.length < 3) {
            return null;
        }
        String thirdParam = params[2];
        // 默认就1个
        if (!TextUtils.equals(thirdParam, "1"))
            return null;
        String forthParam = params[3];
        // 默认就1个
        if (!TextUtils.equals(forthParam, "Stats"))
            return null;
        NetworkStatisticsInfo statisticsInfo = NetworkUtils.getWlanStatisticsInfo();
        String fifthParam = params[4];
        switch (fifthParam) {
            case "BytesSent":
                return statisticsInfo.mTransmitBytes;
            case "BytesReceived":
                return statisticsInfo.mReceiveBytes;
            case "PacketsSent":
                return statisticsInfo.mTransmitPacket;
            case "PacketsReceived":
                return statisticsInfo.mReceivePacket;
            case "ErrorsSent":
                return statisticsInfo.mTransmitErrors;
            case "ErrorsReceived":
                return statisticsInfo.mReceiveErrors;
            case "DiscardPacketsSent":
                return statisticsInfo.mTransmitDropped;
            case "DiscardPacketsReceived":
                return statisticsInfo.mReceiveDropped;
            case "RetransCount":
                return "0"; // String.valueOf(scanedWifiInfo.mInfo.getRetriedTxPacketsPerSecond())
            case "OutQLenHistogram":
                return "NA";
            default:
                break;
        }
        return null;
    }

}
