package com.sdt.diagnose;

import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.sdt.annotations.processor.MethodProperty;
import com.sdt.annotations.policy.Tr369GetPolicy;
import com.sdt.annotations.policy.Tr369SetPolicy;
import com.sdt.diagnose.common.log.LogUtils;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

public class Tr369PathInvoke {
    private static final String TAG = "Tr369PathInvoke";
    final static int TYPE_TR369_SET = 1;
    final static int TYPE_TR369_GET = 2;
    private final ArrayMap<String, MethodProperty> mapBuildSet, mapBuildGet;
    private final ArrayMap<String, CallInfo> mapRunGet, mapRunSet;
    private static Tr369PathInvoke instance = null;

    private Tr369PathInvoke() {
        Tr369SetPolicy policySet = new Tr369SetPolicy();
        Tr369GetPolicy policyGet = new Tr369GetPolicy();

        this.mapBuildSet = new ArrayMap<>();
        this.mapBuildSet.putAll(policySet.mTr369SetMap);
        this.mapBuildGet = new ArrayMap<>();
        this.mapBuildGet.putAll(policyGet.mTr369GetMap);

        this.mapRunGet = createMap(TYPE_TR369_GET, mapBuildGet);
        this.mapRunSet = createMap(TYPE_TR369_SET, mapBuildSet);
    }

    private void filterParams(ArrayMap<String, MethodProperty> arrayMap) {
        Set<Map.Entry<String, MethodProperty>> entries = arrayMap.entrySet();
        for (Map.Entry<String, MethodProperty> next : entries) {
            String key = next.getKey();
            LogUtils.d(TAG, "filterParams key: " + key);
            if (!TextUtils.isEmpty(key) && key.startsWith("Device.X_Skyworth.")) {
                arrayMap.remove(key);
            }
        }
    }

    public static Tr369PathInvoke getInstance() {
        synchronized (Tr369PathInvoke.class) {
            if (instance == null) {
                instance = new Tr369PathInvoke();
            }
        }
        return instance;
    }

    private ArrayMap<String, CallInfo> createMap(int type, ArrayMap<String, MethodProperty> mapBuild) {
        ArrayMap<String, CallInfo> mapRet = new ArrayMap<String, CallInfo>();
        LogUtils.d(TAG, "createMap type: " + type);
        for (Map.Entry<String, MethodProperty> entry : mapBuild.entrySet()) {
            CallInfo value = new CallInfo();
            String key = entry.getKey();
            try {
                value.mProperty = mapBuild.get(key);
                if (value.mProperty == null) continue;
                value.mClass = Class.forName(value.mProperty.mPackageName + "." + value.mProperty.mClassName);
                value.mObject = value.mClass.newInstance();
                value.mWithParam = false;
            } catch (Exception e) {
                LogUtils.i(TAG, "createMap error, " + e.getMessage());
                continue;
            }

            if (type == TYPE_TR369_GET) {
                try {
                    value.mMethod =
                            value.mObject.getClass().getDeclaredMethod(value.mProperty.mMethodName);
                } catch (NoSuchMethodException e) {
                    try {
                        value.mMethod =
                                value.mObject.getClass().getDeclaredMethod(
                                        value.mProperty.mMethodName, String.class);
                        value.mWithParam = true;
                    } catch (NoSuchMethodException e1) {
                        LogUtils.i(TAG, "createMap getDeclaredMethod error, 1: " + e1.getMessage());
                        continue;
                    }
                    LogUtils.i(TAG, "createMap getDeclaredMethod error, 2: " + e.getMessage());
                }
            } else if (type == TYPE_TR369_SET) {
                try {
                    value.mMethod =
                            value.mObject.getClass().getDeclaredMethod(
                                    value.mProperty.mMethodName, String.class, String.class);
                } catch (NoSuchMethodException e) {
                    LogUtils.i(TAG, "createMap error, " + e.getMessage());
                    continue;
                }
            }
            mapRet.put(key, value);
        }
        return mapRet;
    }

    public String getString(String path) {
        if (TextUtils.isEmpty(path)) return null;
//        LogUtils.d(TAG, "getString path: " + path);
        if (mapRunGet == null) return null;
        //优先查询全路径
        CallInfo info = mapRunGet.get(path);
        if (info == null) {
            //然后模糊查询
            for (String key : mapRunGet.keySet()) {
                if (path.contains(key)) {
                    info = mapRunGet.get(key);
                    break;
                }
            }
        }

        if (info == null) return null;
        try {
            if (info.mWithParam) {
                return (String) info.mMethod.invoke(info.mObject, path);
            } else {
                return (String) info.mMethod.invoke(info.mObject);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getString error, " + e.getMessage());
        }
        return null;
    }

    public boolean setString(String path, String value) {
        if (TextUtils.isEmpty(path)) return false;
        LogUtils.d(TAG, "setString path: " + path);
        if (mapRunSet == null) return false;
        //优先查询全路径
        CallInfo info = mapRunSet.get(path);
        if (info == null) {
            //然后模糊查询
            for (String key : mapRunSet.keySet()) {
                if (path.contains(key)) {
                    info = mapRunSet.get(key);
                    break;
                }
            }
        }

        if (info == null) return false;
        try {
            return (Boolean) info.mMethod.invoke(info.mObject, path, value);
        } catch (Exception e) {
            LogUtils.e(TAG, "setString error, " + e.getMessage());
        }
        return false;
    }

    public String getAttribute(int what, String path) {
        if (what == OpenTR369CommandEnum.OpenTR369CommandGetProperty.toInt()) {
            return SystemProperties.get(path, "");
        }
        return getString(path);
    }

    public boolean setAttribute(int what, String path, String value) {
        if (what == OpenTR369CommandEnum.OpenTR369CommandSetProperty.toInt()) {
            SystemProperties.set(path, value);
            return true;
        }
        return setString(path, value);
    }

    static class CallInfo {
        MethodProperty mProperty;
        Class mClass;
        Method mMethod;
        Object mObject;
        boolean mWithParam;
    }

    public enum OpenTR369CommandEnum {
        OpenTR369CommandMin(0),
        OpenTR369CommandGet(1),
        OpenTR369CommandSet(2),
        OpenTR369CommandGetDatabaseStr(3),
        OpenTR369CommandSetProperty(4),
        OpenTR369CommandGetProperty(5),
        OpenTR369CommandUnknown(6);
        private final int value;

        OpenTR369CommandEnum(int i) {
            value = i;
        }

        public int toInt() {
            return value;
        }

        public static OpenTR369CommandEnum valueOf(int value) {
            return (value >= 0 && value <= 6)
                    ? OpenTR369CommandEnum.values()[value]
                    : OpenTR369CommandUnknown;
        }
    }
}
