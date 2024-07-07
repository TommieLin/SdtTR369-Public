package com.sdt.diagnose.common.bean;

import android.content.Intent;

import androidx.annotation.NonNull;

import com.sdt.diagnose.common.DeviceInfoUtils;
import com.sdt.diagnose.database.DbManager;

import java.util.HashMap;

/**
 * @Author Outis
 * @Date 2023/11/30 18:32
 * @Version 1.0
 */
public class AppUpgradeResponseBean {
    private final String planId;
    private final String deviceId;
    private final String operatorCode;
    private final String status;
    private final String msg;

    public AppUpgradeResponseBean(Intent intent) {
        int flag = intent.getIntExtra("status", -1);
        status = ((flag != 0) ? "false" : "true");
        deviceId = DeviceInfoUtils.getSerialNumber();
        planId = intent.getStringExtra("planId");
        operatorCode = intent.getStringExtra("operatorCode");
        msg = intent.getStringExtra("msg");
    }

    public void setResponseDBParams() {
        DbManager.setDBParam("Device.X_Skyworth.UpgradeResponse.PlanId", planId);
        DbManager.setDBParam("Device.X_Skyworth.UpgradeResponse.DeviceId", deviceId);
        DbManager.setDBParam("Device.X_Skyworth.UpgradeResponse.OperatorCode", operatorCode);
        DbManager.setDBParam("Device.X_Skyworth.UpgradeResponse.Status", status);
        DbManager.setDBParam("Device.X_Skyworth.UpgradeResponse.Message", msg);
    }

    public static HashMap<String, String> getResponseDBParams() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("planId", DbManager.getDBParam("Device.X_Skyworth.UpgradeResponse.PlanId"));
        hashMap.put("deviceId", DbManager.getDBParam("Device.X_Skyworth.UpgradeResponse.DeviceId"));
        hashMap.put("operatorCode", DbManager.getDBParam("Device.X_Skyworth.UpgradeResponse.OperatorCode"));
        hashMap.put("status", DbManager.getDBParam("Device.X_Skyworth.UpgradeResponse.Status"));
        hashMap.put("msg", DbManager.getDBParam("Device.X_Skyworth.UpgradeResponse.Message"));
        return hashMap;
    }

    public static void clearResponseDBParams() {
        DbManager.setDBParam("Device.X_Skyworth.UpgradeResponse.Enable", "");
        DbManager.setDBParam("Device.X_Skyworth.UpgradeResponse.PlanId", "");
        DbManager.setDBParam("Device.X_Skyworth.UpgradeResponse.DeviceId", "");
        DbManager.setDBParam("Device.X_Skyworth.UpgradeResponse.OperatorCode", "");
        DbManager.setDBParam("Device.X_Skyworth.UpgradeResponse.Status", "");
        DbManager.setDBParam("Device.X_Skyworth.UpgradeResponse.Message", "");
    }

    public String getPlanId() {
        return planId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public String getOperatorCode() {
        return operatorCode;
    }

    public String getStatus() {
        return status;
    }

    public String getMsg() {
        return msg;
    }

    public HashMap<String, String> toHashMap() {
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("planId", planId);
        hashMap.put("deviceId", deviceId);
        hashMap.put("operatorCode", operatorCode);
        hashMap.put("status", status);
        hashMap.put("msg", msg);
        return hashMap;
    }

    @NonNull
    public String toString() {
        return "AppUpgradeResponseBean {"
                + "planId='"
                + planId
                + '\''
                + ", deviceId='"
                + deviceId
                + '\''
                + ", operatorCode='"
                + operatorCode
                + '\''
                + ", status='"
                + status
                + '\''
                + ", msg='"
                + msg
                + '\''
                + '}';
    }
}
