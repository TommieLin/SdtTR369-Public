package com.sdt.diagnose.Device.Wifi;

import com.sdt.annotations.Tr369Get;
import com.sdt.annotations.Tr369Set;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.NetworkUtils;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.net.NetworkApiManager;

public class SsidX {
    private static final String TAG = "SsidX";

    @Tr369Get("Device.WiFi.SSID.1.Enable")
    public String SK_TR369_GetSsidEnable() {
        boolean ret = NetworkApiManager.getInstance().getNetworkApi().getWiFiSSIDEnable(GlobalContext.getContext(), 0);
        return String.valueOf(ret);
    }

    @Tr369Get("Device.WiFi.SSID.1.Status")
    public String SK_TR369_GetSsidStatus() {
        return NetworkUtils.getWiFiRadioStatus(GlobalContext.getContext());
    }

    @Tr369Get("Device.WiFi.SSID.1.Name")
    public String SK_TR369_GetSsidName() {
        return SK_TR369_GetSsidSSID();
    }

    @Tr369Get("Device.WiFi.SSID.1.BSSID")
    public String SK_TR369_GetSsidBSSID(String path) {
        LogUtils.d(TAG, "GetSsidBSSID path: " + path);
        return NetworkApiManager.getInstance().getNetworkApi().getWiFiBssid(GlobalContext.getContext());
    }

    @Tr369Get("Device.WiFi.SSID.1.SSID")
    public String SK_TR369_GetSsidSSID() {
        return NetworkApiManager.getInstance().getNetworkApi().getWiFiSsid(GlobalContext.getContext());
    }

    @Tr369Get("Device.WiFi.SSID.1.MACAddress")
    public String SK_TR369_GetSsidMACAddress() {
        return NetworkApiManager.getInstance().getNetworkApi().getWiFiSSIDMACAddress(GlobalContext.getContext(), 0);
    }

    @Tr369Set("Device.WiFi.SSID.1.Enable")
    public boolean SK_TR369_SetSsidEnable(String path, String value) {
        LogUtils.d(TAG, "SetSsidEnable path: " + path + ", value: " + value);
        return NetworkApiManager.getInstance().getNetworkApi().setWiFiSSIDEnable(GlobalContext.getContext(), 0, Boolean.parseBoolean(value));
    }

}
