package com.sdt.opentr369;

import androidx.annotation.NonNull;

import android.util.Log;


public class OpenTR369Native {
    static {
        System.loadLibrary("sk_tr369_jni");
    }

    private final static String TAG = "OpenTR369Native";
    static IOpenTr369Listener mListener;

    public interface IOpenTr369Listener {
        String openTR369GetAttr(int what, String path);
        boolean openTR369SetAttr(int what, String path, String value);
        void openTR369Start();
    }

    public static void SetListener(@NonNull IOpenTr369Listener mNotify) {
        mListener = mNotify;
    }

    public static String OpenTR369CallbackGet(int what, String name, String val) {
        String ret = mListener.openTR369GetAttr(what, name);
        String str = "<-- what: " + what;
        if (name != null) {
            str = str + ", name: " + name;
        }
        if (val != null) {
            str = str + ", value: " + val;
        }
        str = str + ", result: " + ret;
        Log.d(TAG, "J OpenTR369CallbackGet " + str);

        return ret;
    }

    public static int OpenTR369CallbackSet(int what, String name, String val, String str3) {
        String str = "--> what: " + what;
        if (name != null) {
            str = str + ", name: " + name;
        }
        if (val != null) {
            str = str + ", value: " + val;
        }
        if (str3 != null) {
            str = str + ", param: " + str3;
        }
        Log.d(TAG, "J OpenTR369CallbackSet " + str);

        return mListener.openTR369SetAttr(what, name, val) ? 0 : -1;
    }

    public static String OpenTR369CallbackGetAttr(String path, String method) {
        String reply = mListener.openTR369GetAttr(0, path);
        String str = "-->: reply: " + reply;
        if (path != null) {
            str = str + ", path: " + path;
        }
        if (method != null) {
            str = str + ", method: " + method;
            Log.d(TAG, "J OpenTR369CallbackGetAttr" + str);
        }
        return reply;
    }

    public static int OpenTR369CallbackSetAttr(String path, String method, String value) {
        String str = "--> ";
        if (path != null) {
            str = str + "path: " + path;
        }
        if (method != null) {
            str = str + ", method: " + method;
        }
        if (value != null) {
            str = str + ", value: " + value;
        }
        Log.d(TAG, "J OpenTR369CallbackSetAttr " + str);
        return mListener.openTR369SetAttr(0, path, value) ? 0 : -1;
    }

    public static void OpenTR369CallbackStart() {
        Log.d(TAG, "J OpenTR369CallbackStart: initial service");
        mListener.openTR369Start();
    }

    public static native String stringFromJNI();
    public static native int OpenTR369Init(String path);
    public static native int SetDefaultModelPath(String default_path);
    public static native String GetDefaultModelPath();
    public static native String GetMqttServerUrl();
    public static native int SetMqttServerUrl(String mqttServer);
    public static native int SetMqttClientId(String clientId);
    public static native int SetMqttUsername(String username);
    public static native int SetMqttPassword(String password);
    public static native int SetMqttCaCertContext(String caCertContext);
    public static native int SetMqttClientPrivateKey(String clientPrivateKey);
    public static native int SetMqttClientCertContext(String clientCertContext);
    public static native void SetUspLogLevel(int level);
    public static native int GetUspLogLevel();
    public static native String GetDBParam(String path);
    public static native int SetDBParam(String path, String value);
    public static native int AddMultiObject(String path, int num);
    public static native int DelMultiObject(String path);
    public static native int UpdateMultiObject(String path, int num);
    public static native int ShowData(String cmd);
    public static native String GetXAuthToken(String mac, String id);
    public static native String GetCACertString();
    public static native String GetDevCertString();
    public static native String GetDevKeyString();
    public static native String GetNetDevInterfaceStatus(String name);
    public static native int GetWirelessNoise(String name);
    public static native String GetNetInterfaceStatus(String name);

}
