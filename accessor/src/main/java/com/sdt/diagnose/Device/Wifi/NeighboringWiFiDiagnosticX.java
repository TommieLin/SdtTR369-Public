package com.sdt.diagnose.Device.Wifi;

import com.sdt.annotations.Tr369Get;
import com.sdt.diagnose.Device.Wifi.NeighboringWiFiDiagnostic.Result;
import com.sdt.diagnose.common.bean.ScanedWifiInfo;

import java.util.List;

public class NeighboringWiFiDiagnosticX {

    @Tr369Get("Device.WiFi.NeighboringWiFiDiagnostic.ResultNumberOfEntries")
    public String SK_TR369_GetResultNumberOfEntries() {
        int size = -1;
        List<ScanedWifiInfo> scans = Result.getScanWifiInfos();
        if (scans != null) {
            size = scans.size();
        }
        return String.valueOf(size);
    }

    @Tr369Get("Device.WiFi.NeighboringWiFiDiagnostic.DiagnosticsState")
    public String SK_TR369_GetDiagnosticsState() {
        if (Integer.parseInt(SK_TR369_GetResultNumberOfEntries()) >= 0) {
            return "Complete";
        } else return "Error";
    }

}
