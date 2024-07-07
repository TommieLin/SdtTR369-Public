package com.sdt.diagnose.Device;

import com.sdt.annotations.Tr369Get;
import com.sdt.annotations.Tr369Set;
import com.sdt.diagnose.common.DeviceInfoUtils;

public class UserInterfaceX {

    @Tr369Get("Device.UserInterface.CurrentLanguage")
    public String SK_TR369_GetCurrentLocalLanguage() {
        return DeviceInfoUtils.getLanguage();
    }

    @Tr369Set("Device.UserInterface.CurrentLanguage")
    public boolean SK_TR369_ChangeSystemLanguage(String path, String value) {
        return DeviceInfoUtils.changeSystemLanguage(value);
    }

    @Tr369Get("Device.UserInterface.AutoUpdateServer")
    public String SK_TR369_GetAutoUpdateServer() {
        // TODO 暂不支持
        return "";
    }

    @Tr369Set("Device.UserInterface.AutoUpdateServer")
    public boolean SK_TR369_SetAutoUpdateServer(String path, String value) {
        // TODO 暂不支持
        return false;
    }
}
