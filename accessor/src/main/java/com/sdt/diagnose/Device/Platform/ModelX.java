package com.sdt.diagnose.Device.Platform;

import com.sdt.diagnose.common.DeviceInfoUtils;
import com.sdt.diagnose.common.log.LogUtils;

public class ModelX {
    private static final String TAG = "ModelX";

    public static Type getPlatform() {
        String platform = DeviceInfoUtils.getHardware();
        LogUtils.d(TAG, "platform is [" + platform + "]");
        if (platform.startsWith("aml")) {
            return Type.Amlogic;
        } else if (platform.startsWith("rtd")) {
            return Type.Realtek;
        } else {
            LogUtils.e(TAG, "Unrecognized content for platform: " + platform);
            return Type.Default;
        }
    }

    public enum Type {
        Amlogic,
        Realtek,
        Default
    }

}
