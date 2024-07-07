package com.sdt.diagnose.Device.X_Skyworth.Log.utils;

import android.os.SystemProperties;
import android.text.TextUtils;

import com.sdt.diagnose.common.log.LogUtils;


/**
 * ClassName: SystemUtils
 *
 * <p>ClassDescription: SystemUtils
 *
 * <p>Author: ZHX Date: 2022/9/7
 *
 * <p>Editor: Outis Data: 2023/11/30
 */
public final class SystemUtils {
    private static final String TAG = "SystemUtils";
    public static final int SLOT_NAB = 0;
    public static final int SLOT_AB = 1;
    private static final String SKY_SCRIPT_NAME = "tr369_script";

    public static int getSlot() {
        String slot = SystemProperties.get("ro.boot.slot_suffix", "");
        if (!TextUtils.isEmpty(slot) && (slot.contains("_a") || slot.contains("_b"))) {
            return SLOT_AB;
        } else {
            return SLOT_NAB;
        }
    }

    /**
     * start script service
     */
    public static void startScriptService() {
        try {
            SystemProperties.set("ctl.start", SKY_SCRIPT_NAME);
            LogUtils.i(TAG, "set property ctl.start. " + SKY_SCRIPT_NAME + " success");
        } catch (Exception e) {
            LogUtils.e(TAG, "set property ctl.start. " + SKY_SCRIPT_NAME + " error: " + e.getMessage());
        }
    }

    /**
     * stop script service
     */
    public static void stopScriptService() {
        try {
            SystemProperties.set("ctl.stop", SKY_SCRIPT_NAME);
            LogUtils.i(TAG, "set property ctl.stop. " + SKY_SCRIPT_NAME + " success");
        } catch (Exception e) {
            LogUtils.e(TAG, "set property ctl.stop. " + SKY_SCRIPT_NAME + " error: " + e.getMessage());
        }
    }

    /**
     * set system property
     *
     * @param name:property  name
     * @param value:property value
     */
    public static void setSystemProperty(String name, String value) {
        SystemProperties.set(name, value);
    }

    /**
     * get system property
     *
     * @param name:property    name
     * @param defValue:default value
     * @return property value
     */
    public static String getSystemProperty(String name, String defValue) {
        return SystemProperties.get(name, defValue);
    }
}
