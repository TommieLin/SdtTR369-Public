package com.sdt.diagnose.Device.DeviceInfo;

import android.os.SystemProperties;
import android.text.TextUtils;

import com.sdt.annotations.Tr369Get;
import com.sdt.diagnose.common.ProtocolPathUtils;
import com.sdt.diagnose.common.log.LogUtils;

import java.util.ArrayList;

/**
 * IBootControl 在Q上没有权限，先注释
 */
public class FirmwareImageX {
    private static final String TAG = "FirmwareImageX";
    private final static String REFIX = "Device.DeviceInfo.FirmwareImage.";

    @Tr369Get("Device.DeviceInfo.FirmwareImage.")
    public String SK_TR369_GetFirmwareImageInfo(String path) {
        if (!isSlot()) {
            return "--";
        }
        String[] params = ProtocolPathUtils.parse(REFIX, path);
        if (params == null || params.length < 1) {
            return null;
        }
        int index = 0;
        try {
            index = Integer.parseInt(params[0]);
            switch (params[1]) {
                case "Alias":
                    return "";
                case "Name":
                    return getFirmwareImageName(index);
                case "Available":
                    return getFirmwareImageAvailable(index);
                case "Status":
                    return getFirmwareImageStatus(index);
                case "BootFailureLog":
                    return getBootFailureLog(index);
                case "Version":
                    return getFirmwareImageVersion(index);
                default:
                    break;
            }
        } catch (NumberFormatException e) {
            //Todo report error.
            return null;
        }
        return "";
    }

    public String getFirmwareImageName(int index) {
        ArrayList<String> list = new ArrayList();
        list.add("_a");
        list.add("_b");
        String active = SystemProperties.get("ro.boot.slot_suffix", "_a");
        LogUtils.d(TAG, "getFirmwareImageName active: " + active);

        if (index == 1) {
            return active;
        }
        list.remove(active);
        return list.get(0);
    }

    public String getFirmwareImageVersion(int index) {
        if (index == 1) {
            return SystemProperties.get("ro.build.version.incremental", "null");
        }
        return "";
    }

    public String getFirmwareImageAvailable(int index) {
        if (index == 1) {
            return "True";
        }
        return "False";
    }

    public String getFirmwareImageStatus(int index) {
        if (index == 1) {
            return "Available";
        }
        return "ActivationFailed";
    }

    public String getBootFailureLog(int index) {
        if (index == 1) {
            return "Successfully";
        }
        return "";
    }

    private boolean isSlot() {
        String active = SystemProperties.get("ro.boot.slot_suffix", "");
        return !TextUtils.isEmpty(active);
    }

}
