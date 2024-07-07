package com.sdt.diagnose.Device;

import android.app.AlarmManager;
import android.provider.Settings;

import com.sdt.annotations.Tr369Get;
import com.sdt.annotations.Tr369Set;
import com.sdt.diagnose.common.DeviceInfoUtils;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.database.DbManager;

public class TimeX {
    private static final int DISABLED = 1;
    private static final int UNSYNCHRONIZED = 2;
    private static final int SYNCHRONIZED = 3;
    private static final int ERROR_FAILEDSYNC = 4;
    private static final int ERROR = 5;

    @Tr369Get("Device.Time.Status")
    public String SK_TR369_GetTimeStatus() {
        String timeStatus = "Error";
        int value = DeviceInfoUtils.getAutoDateTimeType(GlobalContext.getContext());
        if (value > 0) {
            return "Synchronized";
        } else {
            return "Unsynchronized";
        }
    }

    @Tr369Set("Device.Time.NTPServer1,Device.Time.NTPServer2,Device.Time.NTPServer3,Device.Time.NTPServer4,Device.Time.NTPServer5")
    public boolean SK_TR369_SetNtpServer(String path, String value) {
        boolean result = Settings.Global.putString(GlobalContext.getContext().getContentResolver(),
                Settings.Global.NTP_SERVER, value);
        if (result) DbManager.setDBParam(path, value);
        return result;
    }

    @Tr369Get("Device.Time.CurrentLocalTime")
    public String SK_TR369_GetCurrentLocalTime() {
        return DeviceInfoUtils.getTime();
    }

    @Tr369Get("Device.Time.LocalTimeZone")
    public String SK_TR369_GetTimeZone() {
        return DeviceInfoUtils.getTimeZone(GlobalContext.getContext());// about - Date & time - set time zone
    }

    @Tr369Set("Device.Time.LocalTimeZone")
    public boolean SK_TR369_SetTimeZone(String path, String value) {
        AlarmManager alarmManager = GlobalContext.getContext().getSystemService(AlarmManager.class);
        alarmManager.setTimeZone(value);
        return true;
    }
}
