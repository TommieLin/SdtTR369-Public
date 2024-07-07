package com.sdt.diagnose.common.configuration;

import com.sdt.diagnose.common.log.LogUtils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigProperties {
    private static final String TAG = "ConfigProperties";
    private static Properties p = null;
    public static final String CONFIGFILE_PATH = "/vendor/etc/skyconfig/config.properties";
    private static ConfigProperties configInstance;

    private ConfigProperties() {
        if (p == null) {
            initConfigFile(getInputStream(CONFIGFILE_PATH));
        }
    }

    public static ConfigProperties getInstance() {
        synchronized (ConfigProperties.class) {
            if (configInstance == null) {
                synchronized (ConfigProperties.class) {
                    configInstance = new ConfigProperties();
                }
            }
        }
        return configInstance;
    }

    public void init() {
        initConfigFile(getInputStream(CONFIGFILE_PATH));
    }

    public String getString(String key) {
        return getString(key, "");
    }

    public String getString(String key, String def) {
        try {
            return (p == null) ? null : p.getProperty(key, def);
        } catch (Exception e) {
            LogUtils.e(TAG, "getString error, " + e.getMessage());
            return def;
        }
    }

    public int getInt(String key) {
        return getInt(key, Integer.MIN_VALUE);
    }

    public int getInt(String key, int def) {
        try {
            String value = ((p == null) ? null : p.getProperty(key));
            return (value == null) ? def : Integer.parseInt(value);
        } catch (Exception e) {
            LogUtils.e(TAG, "getInt error, " + e.getMessage());
            return def;
        }
    }

    public long getLong(String key) {
        return getLong(key, Long.MIN_VALUE);
    }

    public long getLong(String key, long def) {
        try {
            String value = ((p == null) ? null : p.getProperty(key));
            return (value == null) ? def : Long.parseLong(value);
        } catch (Exception e) {
            LogUtils.e(TAG, "getLong error, " + e.getMessage());
            return def;
        }
    }

    public boolean getBoolean(String key) {
        return getBoolean(key, false);
    }

    public boolean getBoolean(String key, boolean def) {
        try {
            String value = ((p == null) ? null : p.getProperty(key));
            return (value == null) ? def : Boolean.parseBoolean(value);
        } catch (Exception e) {
            LogUtils.e(TAG, "getBoolean error, " + e.getMessage());
            return def;
        }
    }

    public String[] getStringArray(String key, String[] def) {
        try {
            String value = ((p == null) ? null : p.getProperty(key));
            return (value == null) ? def : value.split(";");
        } catch (Exception e) {
            LogUtils.e(TAG, "getStringArray error, " + e.getMessage());
            return def;
        }
    }

    public String[] getStringArray(String key) {
        return getStringArray(key, new String[]{});
    }

    public Properties getConfigP() {
//        Iterator<Map.Entry<Object, Object>> iter  = p.entrySet().iterator(); //返回的属性键值对实体
//        while (iter.hasNext()) {
//            Map.Entry entry = iter.next();
//            String name = (String) entry.getKey();
//            String value = (String) entry.getValue();
//            ContentValues cv = new ContentValues();
//        }
        return p;
    }

    public void clear() {
        if (p != null) {
            p.clear();
        }
    }

    /**
     * 传入文件绝对路径，返回Properties对象（Map）
     * 文件中所有数据必须遵循 key=value 格式
     *
     * @param path 文件绝对路劲
     * @return
     */
    public Properties getProperties(String path) {
        Properties pp = new Properties();
        try {
            pp.load(getInputStream(path));
        } catch (IOException e) {
            LogUtils.e(TAG, "getProperties error, " + e.getMessage());
        }
        return pp;
    }

    public InputStream getInputStream(String path) {
        FileInputStream fis = null;
        String configFilePath = path;
        try {
            fis = new FileInputStream(configFilePath);//通过字节流获取
            return fis;
        } catch (FileNotFoundException e) {
            LogUtils.e(TAG, "getInputStream error, " + e.getMessage());
        }
        return null;
    }

    private Properties initConfigFile(InputStream in) {
        if (in == null) return null;
        try {
            long start = System.currentTimeMillis();
            LogUtils.d(TAG, "initConfigFile start: " + start);
            p = new Properties();
            p.load(in);
            LogUtils.d(TAG, "initConfigFile end, p: " + p.toString()
                    + ", cost time: " + (System.currentTimeMillis() - start));
        } catch (IOException e) {
            LogUtils.e(TAG, "initConfigFile error, " + e.getMessage());
        }
        return p;
    }
}
