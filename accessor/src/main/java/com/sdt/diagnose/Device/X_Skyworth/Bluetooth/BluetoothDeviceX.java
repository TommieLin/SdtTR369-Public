package com.sdt.diagnose.Device.X_Skyworth.Bluetooth;

import android.text.TextUtils;

import com.sdt.annotations.Tr369Get;
import com.sdt.annotations.Tr369Set;
import com.sdt.diagnose.common.BluetoothDeviceManager;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.IProtocolArray;
import com.sdt.diagnose.common.ProtocolPathUtils;
import com.sdt.diagnose.common.bean.BluetoothDeviceInfo;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.database.DbManager;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * This class for protocol:
 * Device.X_Skyworth.BluetoothDevice.{i}.Name
 * Device.X_Skyworth.BluetoothDevice.{i}.Mac
 * Device.X_Skyworth.BluetoothDevice.{i}.Type
 * Device.X_Skyworth.BluetoothDevice.{i}.Status
 * Device.X_Skyworth.BluetoothDevice.{i}.Enable
 * Device.X_Skyworth.BluetoothDevice.{i}.BatteryLevel
 * Device.X_Skyworth.BluetoothDevice.{i}.RcuVersion
 */
public class BluetoothDeviceX implements IProtocolArray<BluetoothDeviceInfo> {
    private static final String TAG = "BluetoothDeviceX";
    private final static String REFIX = "Device.X_Skyworth.BluetoothDevice.";
    private static BluetoothDeviceManager mBluetoothDevices = null;

    @Tr369Get("Device.X_Skyworth.BluetoothDevice.")
    public String SK_TR369_GetBluetoothDeviceInfo(String path) {
        return handleBluetoothX(path);
    }

    @Tr369Set("Device.X_Skyworth.BluetoothDevice.1.ConnectionStatus")
    public boolean SK_TR369_SetConnectionStatus(String path, String val) {
        LogUtils.d(TAG, "SetConnectionStatus");
        boolean ret = false;
        if (mBluetoothDevices == null) {
            mBluetoothDevices = new BluetoothDeviceManager(GlobalContext.getContext());
        }
        if (val.equals("0")) {
            ArrayList<BluetoothDeviceInfo> list = mBluetoothDevices.getList();
            for (BluetoothDeviceInfo deviceInfo : list) {
                if (deviceInfo.isConnected()) {
                    deviceInfo.disConnectGatt();
                }
            }
            ret = true;
        }
        return ret;
    }

    private String handleBluetoothX(String path) {
        return ProtocolPathUtils.getInfoFromArray(REFIX, path, this);
    }

    @Override
    public List<BluetoothDeviceInfo> getArray() {
        if (mBluetoothDevices == null) {
            mBluetoothDevices = new BluetoothDeviceManager(GlobalContext.getContext());
        }
        return mBluetoothDevices.getList();
    }

    @Override
    public String getValue(BluetoothDeviceInfo deviceInfo, @NotNull String[] paramsArr) {
        if (paramsArr.length < 2) {
            return null;
        }
        String secondParam = paramsArr[1];
        if (TextUtils.isEmpty(secondParam)) {
            //Todo report error.
            return null;
        }
        switch (secondParam) {
            case "Type":
                return deviceInfo.getType();
            case "Mac":
                return deviceInfo.getMac();
            case "Name":
                return deviceInfo.getName();
            case "ConnectionStatus":
                return String.valueOf(deviceInfo.isConnected() ? 1 : 0);
            case "Power":
                if (deviceInfo.getBatteryLevel() <= 0) {
                    return "null";
                }
                return String.valueOf(deviceInfo.getBatteryLevel());
            case "RcuVersion":
                return deviceInfo.getRcuVersion();
            case "Remove":
                return String.valueOf(deviceInfo.isBound());
            default:
                break;
        }
        return null;
    }

    public static void updateBluetoothList() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            LogUtils.e(TAG, "updateBluetoothList error, " + e.getMessage());
        }

        if (mBluetoothDevices != null) {
            if (!mBluetoothDevices.isEmpty()) {
                mBluetoothDevices.clear();
            }
            mBluetoothDevices = null;
        }

        mBluetoothDevices = new BluetoothDeviceManager(GlobalContext.getContext());
        int size = mBluetoothDevices.getList().size();
        LogUtils.d(TAG, "Get the number of Bluetooth devices: " + size);
        if (size > 0) {
            DbManager.updateMultiObject("Device.X_Skyworth.BluetoothDevice", size);
        } else {
            DbManager.delMultiObject("Device.X_Skyworth.BluetoothDevice");
        }
    }
}
