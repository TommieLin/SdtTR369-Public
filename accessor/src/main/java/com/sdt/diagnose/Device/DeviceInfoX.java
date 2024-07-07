package com.sdt.diagnose.Device;

import android.os.Build;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.text.TextUtils;

import com.sdt.annotations.Tr369Get;
import com.sdt.annotations.Tr369Set;
import com.sdt.diagnose.Device.DeviceInfo.LocationX;
import com.sdt.diagnose.common.DeviceInfoUtils;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.database.DbManager;

import java.text.SimpleDateFormat;

public class DeviceInfoX {

    @Tr369Get("Device.DeviceInfo.Manufacturer")
    public String SK_TR369_GetManufacturer() {
        return DeviceInfoUtils.getManufacturer();
    }

    @Tr369Get("Device.DeviceInfo.ManufacturerOUI")
    public String SK_TR369_GetManufacturerOUI() {
        return DbManager.getDBParam("Device.DeviceInfo.ManufacturerOUI");
    }

    @Tr369Get("Device.DeviceInfo.SerialNumber")
    public String SK_TR369_GetSerialNumber() {
        return DeviceInfoUtils.getSerialNumber(); // about - Status - Serial Number
    }

    @Tr369Get("Device.DeviceInfo.ChipID")
    public String SK_TR369_GetChipID() {
        return DeviceInfoUtils.getChipID();
    }

    @Tr369Get("Device.DeviceInfo.ModelName,Device.DeviceInfo.ModelID,Device.DeviceInfo.ProductClass")
    public String SK_TR369_GetModelName() {
        return DeviceInfoUtils.getDeviceModel(); // about - Model
    }

    @Tr369Get("Device.DeviceInfo.TvName")
    public String SK_TR369_GetDeviceName() {
        return DeviceInfoUtils.getDeviceName(GlobalContext.getContext());
    }

    @Tr369Get("Device.DeviceInfo.ActiveFirmwareImage")
    public String SK_TR369_GetActiveFirmwareImage() {
        return SystemProperties.get("ro.boot.slot_suffix", "");
    }

    @Tr369Get("Device.DeviceInfo.BootFirmwareImage")
    public String SK_TR369_GetBootFirmwareImage() {
        String active = SystemProperties.get("ro.boot.slot_suffix", "");
        return active.isEmpty()
                ? DbManager.getDBParam("Device.DeviceInfo.BootFirmwareImage")
                : active;
    }

    @Tr369Get("Device.DeviceInfo.HardwareVersion")
    public String SK_TR369_GetHardwareVersion() {
        return Build.HARDWARE;
    }

    @Tr369Get("Device.DeviceInfo.SoftwareVersion")
    public String SK_TR369_GetSoftwareVersion() {
        long utc = SystemProperties.getLong("ro.build.date.utc", 1631947123L);
        return String.valueOf(utc * 1000L);
    }

    @Tr369Get("Device.DeviceInfo.DeviceStatus")
    public String SK_TR369_GetDeviceStatus() {
        return "Up";
    }

    @Tr369Get("Device.DeviceInfo.UpTime")
    public String SK_TR369_GetUpTime() {
        long time_s = SystemClock.elapsedRealtime() / 1000;
        return String.valueOf(time_s);
    }

    @Tr369Get("Device.DeviceInfo.FirstUseDate")
    public String SK_TR369_GetFirstUseDate() {
        String firstUseDate = DbManager.getDBParam("Device.DeviceInfo.FirstUseDate");
        if (TextUtils.isEmpty(firstUseDate)) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            firstUseDate = df.format(System.currentTimeMillis());
            DbManager.setDBParam("Device.DeviceInfo.FirstUseDate", firstUseDate);
            return firstUseDate;
        }
        return firstUseDate;
    }

    @Tr369Get("Device.DeviceInfo.Location.1.")
    public String SK_TR369_GetLocationInfo(String path) {
        if (path.contains("DataObject")) {
            return LocationX.getInstance().getIpInfoIoJson();
        }
        return DbManager.getDBParam(path);
    }

    @Tr369Set("Device.DeviceInfo.Location.1.")
    public boolean SK_TR369_SetLocationInfo(String path, String value) {
        return (DbManager.setDBParam(path, value) == 0);
    }

}
