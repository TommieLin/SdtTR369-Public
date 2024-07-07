package com.sdt.diagnose.Device.Wifi.NeighboringWiFiDiagnostic;

import android.text.TextUtils;

import com.sdt.annotations.Tr369Get;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.IProtocolArray;
import com.sdt.diagnose.common.ProtocolPathUtils;
import com.sdt.diagnose.common.ScanWifiInfosManager;
import com.sdt.diagnose.common.bean.ScanedWifiInfo;

import java.util.List;

/**
 * 盒子扫描到的WIFI列表。
 * This class for protocol:
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.SSID               - WIFI  SSID
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.BSSID              - WIFI  BSSID
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.Radio              - wifi 标准。 802.11a/b/g
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.Mode               - WIFI Radio模式。默认 AdHoc 模式
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.Channel            - 频道
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.SignalStrength      - 信号强度
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.SecurityModeEnabled   - 加密类型 . TKIP/AES
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.EncryptionMode               - 加密类型
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.OperatingFrequencyBand            - 频段。 2.4G / 5G
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.SupportedStandards       - 支持的802.11标准.2.4GHz网络从b,g,n,ax中选择.5GHz网络从a,n,ac,ax中选择.
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.OperatingStandards       - 运行的802.11标准
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.OperatingChannelBandwidth    - 运行的带宽
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.Noise    - 平均噪声强度指标(dBm)
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.BasicDataTransferRates    - 数据传输速率。比如"1,2"代表基本速率为1mbps和2mbps
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.SupportedDataTransferRates    - 支持传输的速率。比如"1,2,5.5"只允许1mbps、2mbps和5.5 Mbps的连接
 * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.DTIMPeriod    - xxxxx
 */
public class Result implements IProtocolArray<ScanedWifiInfo> {
    private final static String REFIX = "Device.WiFi.NeighboringWiFiDiagnostic.Result.";
    private static ScanWifiInfosManager mScanWifiManager = null;

    @Tr369Get("Device.WiFi.NeighboringWiFiDiagnostic.Result.")
    public String SK_TR369_GetNeighboringWiFiPath(String path) {
        return handleNeighboringWiFiPath(path);
    }

    private String handleNeighboringWiFiPath(String path) {
        return ProtocolPathUtils.getInfoFromArray(REFIX, path, this);
    }

    public static List<ScanedWifiInfo> getScanWifiInfos() {
        if (mScanWifiManager != null && !mScanWifiManager.isEmpty()) {
            return mScanWifiManager.getList();
        }
        mScanWifiManager = new ScanWifiInfosManager(GlobalContext.getContext());
        return mScanWifiManager.getList();
    }

    @Override
    public List<ScanedWifiInfo> getArray() {
        return getScanWifiInfos();
    }

    @Override
    public String getValue(ScanedWifiInfo scanedWifiInfo, String[] paramsArr) {
        if (paramsArr.length < 2) {
            return null;
        }
        String secondParam = paramsArr[1];
        if (TextUtils.isEmpty(secondParam)) {
            //Todo report error.
            return null;
        }
        switch (secondParam) {
            case "Radio":
                return scanedWifiInfo.radio;
            case "SSID":
                return scanedWifiInfo.ssid;
            case "BSSID":
                return scanedWifiInfo.bssid;
            case "Mode":
                return scanedWifiInfo.mode;
            case "Channel":
                return String.valueOf(scanedWifiInfo.channel);
            case "SignalStrength":
                return String.valueOf(scanedWifiInfo.signalStrength);
            case "SecurityModeEnabled":
                return scanedWifiInfo.securityModeEnabled;
            case "EncryptionMode":
                return scanedWifiInfo.encryptionMode;
            case "OperatingFrequencyBand":
                return scanedWifiInfo.operatingFrequencyBand;
            case "OperatingChannelBandwidth":
                return scanedWifiInfo.operatingChannelBandwidth;
            case "SupportedStandards":
                return scanedWifiInfo.supportedStandards;
            case "OperatingStandards":
                return scanedWifiInfo.operatingStandards;
            case "Noise":
                return String.valueOf(scanedWifiInfo.noise);
            case "BasicDataTransferRates":
                return String.valueOf(scanedWifiInfo.basicDataTransferRates);
            case "SupportedDataTransferRates":
                return String.valueOf(scanedWifiInfo.supportedDataTransferRates);
            case "DTIMPeriod":
                return String.valueOf(scanedWifiInfo.DTIMPeriod);
            default:
                break;
        }

        return null;
    }
}
