package com.sdt.diagnose.Device;

import android.os.Build;

import com.sdt.annotations.Tr369Get;

public class GatewayInfoX {

    @Tr369Get("Device.GatewayInfo.ProductClass")
    public String SK_TR369_GetProductClass() {
        return Build.MODEL;
    }

    @Tr369Get("Device.GatewayInfo.SerialNumber")
    public String SK_TR369_GetSerialNumber() {
        return Build.getSerial();
    }

}
