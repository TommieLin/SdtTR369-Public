package com.sdt.diagnose.common.configuration;

import android.content.Context;

import java.util.Map;

public class Config {
    private static Context mContext = null;

    private Config() {
    }

    public static void initContext(Context context) {
        if (context == null) return;
        mContext = context.getApplicationContext();
    }

    public static String getString(String key) {
        return getString(key, "");
    }

    public static String getString(String key, String def) {
        return ConfigProperties.getInstance().getString(key, def);
    }

    public static int getInt(String key) {
        return getInt(key, Integer.MIN_VALUE);
    }

    public static int getInt(String key, int def) {
        return ConfigProperties.getInstance().getInt(key, def);
    }

    public static long getLong(String key) {
        return getLong(key, Long.MIN_VALUE);
    }

    public static long getLong(String key, long def) {
        return ConfigProperties.getInstance().getLong(key, def);
    }

    public static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public static boolean getBoolean(String key, boolean def) {
        return ConfigProperties.getInstance().getBoolean(key, def);
    }

    public static String[] getStringArray(String key, String[] def) {
        return ConfigProperties.getInstance().getStringArray(key, def);
    }

    public static String[] getStringArray(String key) {
        return getStringArray(key, null);
    }

    public static Map cacheAllData() {
        return ConfigProperties.getInstance().getConfigP();
    }

}
