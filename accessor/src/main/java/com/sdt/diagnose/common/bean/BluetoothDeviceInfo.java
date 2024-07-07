package com.sdt.diagnose.common.bean;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;

import com.sdt.diagnose.common.log.LogUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BluetoothDeviceInfo {
    private static final String TAG = "BluetoothDeviceInfo";
    private final String mName;
    private final String mMac;
    private String mType;
    private final boolean mBound = true; // 默认是已绑定状态
    private final boolean isConnected;
    private int mBatteryLevel;     // 剩余电量
    private String mRcuVersion;    // Sky 蓝牙遥控器固件版本号

    public void setType(String type) {
        this.mType = type;
    }

    public void setBatteryLevel(int level) {
        mBatteryLevel = level;
    }

    public void setSkyRcuVersion(String version) {
        mRcuVersion = version;
    }

    public String getName() {
        return mName;
    }

    public String getMac() {
        return mMac;
    }

    public String getType() {
        return mType;
    }

    public boolean isBound() {
        return mBound;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public int getBatteryLevel() {
        return mBatteryLevel;
    }

    public String getRcuVersion() {
        return mRcuVersion;
    }

    public boolean disConnect() {
        return false;
    }

    @NonNull
    @Override
    public String toString() {
        return "{BluetoothDeviceInfo:" + mName
                + ", mac:" + mMac
                + ", type:" + mType
                + ", connected:" + isConnected
                + ", battery:" + mBatteryLevel
                + ", rcuVersion:" + mRcuVersion
                + "}";
    }

    private static final int MINOR_DEVICE_CLASS_POINTING = Integer.parseInt("0000010000000", 2);
    private static final int MINOR_DEVICE_CLASS_JOYSTICK = Integer.parseInt("0000000000100", 2);
    private static final int MINOR_DEVICE_CLASS_GAMEPAD = Integer.parseInt("0000000001000", 2);
    private static final int MINOR_DEVICE_CLASS_KEYBOARD = Integer.parseInt("0000001000000", 2);
    private static final int MINOR_DEVICE_CLASS_REMOTE = Integer.parseInt("0000000001100", 2);
    private final BluetoothDevice mBluetoothDevice;
    private static BluetoothGatt mDeviceGatt;

    public BluetoothDeviceInfo(Context context, BluetoothDevice device) {
        this.mBluetoothDevice = device;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
            this.mName = device.getAlias();
        } else {
            this.mName = device.getAliasName();
        }
        this.mMac = device.getAddress();
        this.isConnected = device.isConnected();

        this.parseType(mBluetoothDevice);

        int battery = device.getBatteryLevel(); // -1 :表示不支持读电量
        if (battery != -1) {
            setBatteryLevel(battery);
        }

        this.connectGatt(context, mBluetoothDevice);
    }

    private void parseType(BluetoothDevice device) {
        final BluetoothClass bluetoothClass = device.getBluetoothClass();
        LogUtils.d(TAG, "bluetoothClass: " + bluetoothClass);
        int devClass = -1;
        if (bluetoothClass != null) {
            devClass = bluetoothClass.getDeviceClass();
        }

        LogUtils.d(TAG, "devClass: " + devClass);

        String type = "";
        if (devClass == BluetoothClass.Device.AUDIO_VIDEO_WEARABLE_HEADSET) {
            type = "headset_mic";
        } else if (devClass == BluetoothClass.Device.AUDIO_VIDEO_HEADPHONES
                || devClass == BluetoothClass.Device.AUDIO_VIDEO_LOUDSPEAKER
                || devClass == BluetoothClass.Device.AUDIO_VIDEO_PORTABLE_AUDIO
                || devClass == BluetoothClass.Device.AUDIO_VIDEO_HIFI_AUDIO) {
            type = "headset";
        } else if ((devClass & MINOR_DEVICE_CLASS_POINTING) != 0) {
            type = "mouse";
        } else if ((devClass & MINOR_DEVICE_CLASS_JOYSTICK) != 0) {
            type = "Joystick";
        } else if ((devClass & MINOR_DEVICE_CLASS_GAMEPAD) != 0) {
            type = "Gamepad";
        } else if ((devClass & MINOR_DEVICE_CLASS_KEYBOARD) != 0) {
            type = "keyboard";
        } else if ((devClass & MINOR_DEVICE_CLASS_REMOTE) != 0) {
            type = "RemoteControl";
        }
        setType(type);
    }

    public void disConnectGatt() {
        LogUtils.d(TAG, "disConnectGatt");
        mDeviceGatt.disconnect();
        mDeviceGatt.getDevice().removeBond();
        mDeviceGatt.close();
        refresh();
    }

    private void refresh() {
        try {
            Method method = this.mDeviceGatt.getClass().getMethod("refresh",
                    new Class[0]);
            method.setAccessible(true);
            method.invoke(this.mDeviceGatt);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            LogUtils.e(TAG, "refresh error, " + e.getMessage());
        }
    }

    private void connectGatt(Context context, BluetoothDevice device) {
        int devType = device.getType();
        if (devType == BluetoothDevice.DEVICE_TYPE_LE ||
                devType == BluetoothDevice.DEVICE_TYPE_DUAL) {
            if (mDeviceGatt != null) {
                mDeviceGatt.close();
            }
            mDeviceGatt = device.connectGatt(context, true, new BluetoothDeviceInfo.GattBatteryCallbacks());
            LogUtils.d(TAG, "connectGatt Code: " + mDeviceGatt.hashCode());
            // device.connectGatt 后，会通过AIDL运行在系统进程，此线程未等GattBatteryCallbacks回调就会走完.
            // 就会导致获取不到蓝牙遥控器的版本号和电量。
            // 另一种方案:加锁。此方案风险在于在何时解锁；如果是第三方遥控器也执行到此处，也加锁了怎么办?
            try {
                Thread.sleep(2000);
            } catch (Exception e) {
                LogUtils.e(TAG, "connectGatt error, " + e.getMessage());
            }
        }
    }

    private static final UUID GATT_BATTERY_SERVICE_UUID =
            UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb");
    private static final UUID GATT_BATTERY_LEVEL_CHARACTERISTIC_UUID =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");

    //get rcu version
    private static String HID_SERVICE = "00001812-0000-1000-8000-00805f9b34fb";
    private final static UUID UUID_HID_SERVICE = UUID.fromString(HID_SERVICE);
    private final static String CHARACTER_VENDOR_OUT_NAME = UUID_HID_SERVICE.toString() + "VendorOut";
    private final static String CHARACTER_VENDOR_IN_NAME = UUID_HID_SERVICE.toString() + "VendorIn";
    private final static String CHARACTER_HID_IN_NAME = UUID_HID_SERVICE.toString() + "HidIn";
    private final byte BLE_VERSION_ASW = (byte) 0xf9;
    private final byte BLE_ACK = (byte) 0x5c;
    private HashMap<String, BluetoothGattCharacteristic> mGattCharacteristicMap = new HashMap<>();
    private byte[] recBuff = new byte[22];
    private boolean isWriteThread = false;

    private class GattBatteryCallbacks extends BluetoothGattCallback {

        GattBatteryCallbacks() {
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            LogUtils.d(TAG, "Connection status: " + status + ", state: " + newState);
            LogUtils.d(TAG, "onConnectionStateChange: " + Thread.currentThread().getId());
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothGatt.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (status == 22 && newState == BluetoothGatt.STATE_DISCONNECTED) {
                gatt.close();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            LogUtils.d(TAG, "onServicesDiscovered: " + Thread.currentThread().getId());
            if (status != BluetoothGatt.GATT_SUCCESS) {
                LogUtils.e(TAG, "Service discovery failure on " + gatt);
                return;
            }

            final BluetoothGattService battService = gatt.getService(GATT_BATTERY_SERVICE_UUID);
            if (battService == null) {
                LogUtils.e(TAG, "No battery service");
                return;
            }

            final BluetoothGattCharacteristic battLevel =
                    battService.getCharacteristic(GATT_BATTERY_LEVEL_CHARACTERISTIC_UUID);
            if (battLevel == null) {
                LogUtils.e(TAG, "No battery level");
                return;
            }

            gatt.readCharacteristic(battLevel);

            //get remote version
            LogUtils.d(TAG, "rcu version service");
            addGattCharacteristic(findGattServiceCharacter(mDeviceGatt));
            setCharacteristicNotification(CHARACTER_VENDOR_IN_NAME, true);
            setCharacteristicNotification(CHARACTER_HID_IN_NAME, true);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            LogUtils.d(TAG, "onCharacteristicRead: " + Thread.currentThread().getId());
            if (status != BluetoothGatt.GATT_SUCCESS) {
                LogUtils.e(TAG, "Read characteristic failure on " + gatt + ".. " + characteristic);
                return;
            }

            if (GATT_BATTERY_LEVEL_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) {
                final int batteryLevel =
                        characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                LogUtils.d(TAG, "batteryLevel: " + batteryLevel);
                setBatteryLevel(batteryLevel);
            }

            if (!isWriteThread) {
                writeCharacteristic(CHARACTER_VENDOR_OUT_NAME, readRCUVersion());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            LogUtils.d(TAG, "onCharacteristicChanged: " + Thread.currentThread().getId());
            super.onCharacteristicChanged(gatt, characteristic);
            if (!characteristic.getUuid().toString().equals("00002a4d-0000-1000-8000-00805f9b34fb")) {
                LogUtils.d(TAG, "on not care CharacteristicChanged");
                return;
            }
            if (characteristic.getValue().length == 8) {
                LogUtils.d(TAG, "key input");
                return;
            }

            System.arraycopy(characteristic.getValue(), 0, recBuff, 0, characteristic.getValue().length);

            if (BLE_ACK != recBuff[0]) {
                return;
            }

            if (recBuff[1] == BLE_VERSION_ASW) {
                String rcuVersion = byteToHexStr(recBuff[5]) + byteToHexStr(recBuff[4]);
                LogUtils.d(TAG, "rcuVersion: " + rcuVersion);
                setSkyRcuVersion(rcuVersion);
                setType("RemoteControl");
            }
        }
    }

    /**
     * get version buf
     */
    private byte[] readRCUVersion() {
        return new byte[]{0x5c, (byte) 0x82, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
    }


    /**
     * get rcu verson
     */
    private void addGattCharacteristic(HashMap<String, BluetoothGattCharacteristic> map) {
        for (Map.Entry<String, BluetoothGattCharacteristic> stringBluetoothGattCharacteristicEntry : map.entrySet()) {
            Map.Entry entry = (Map.Entry) stringBluetoothGattCharacteristicEntry;
            if (mGattCharacteristicMap == null) {
                mGattCharacteristicMap = new HashMap<>();
            }
            mGattCharacteristicMap.put((String) entry.getKey(), (BluetoothGattCharacteristic) entry.getValue());
        }
    }

    private BluetoothGattCharacteristic getGattCharacteristic(String characteristicKey) {
        if (mGattCharacteristicMap == null) {
            LogUtils.d(TAG, "mGattCharacteristicMap is null.");
            return null;
        }
        return mGattCharacteristicMap.get(characteristicKey);
    }

    private void writeCharacteristic(String characterKey, byte[] buf) {
        LogUtils.d(TAG, "writeCharacteristic");
        final String key = characterKey;
        final byte[] bufs = buf;

        new Thread(() -> {
            BluetoothGattCharacteristic characteristic = getGattCharacteristic(key);
            if ((null == characteristic) || (null == mDeviceGatt)) {
                return;
            }
            characteristic.setValue(bufs);
            isWriteThread = mDeviceGatt.writeCharacteristic(characteristic);
            LogUtils.d(TAG, "writeCharacteristic isWriteThread: " + isWriteThread);
        }).start();
    }

    private HashMap<String, BluetoothGattCharacteristic> findGattServiceCharacter(BluetoothGatt gatt) {
        HashMap<String, BluetoothGattCharacteristic> map = new HashMap<String, BluetoothGattCharacteristic>();
        LogUtils.d(TAG, "findGattServiceCharacter");
        BluetoothGattService hidService = gatt.getService(UUID_HID_SERVICE);
        if (hidService != null) {
            List<BluetoothGattCharacteristic> hidCharList = hidService.getCharacteristics();
            int gattCharacterSize = hidCharList.size();
            if (gattCharacterSize >= 5) {
                map.put(CHARACTER_HID_IN_NAME, hidCharList.get(gattCharacterSize - 1));
                map.put(CHARACTER_VENDOR_OUT_NAME, hidCharList.get(gattCharacterSize - 2));
                map.put(CHARACTER_VENDOR_IN_NAME, hidCharList.get(gattCharacterSize - 3));
            }
        }
        return map;
    }

    private boolean setCharacteristicNotification(String characteristicKey, boolean enable) {
        BluetoothGattCharacteristic characteristic = getGattCharacteristic(characteristicKey);
        if (null == characteristic) {
            return false;
        }
        boolean ret = mDeviceGatt.setCharacteristicNotification(characteristic, enable);
        return true;
    }

    private static String byteToHexStr(byte data) {
        return String.format("%02X", data);
    }

}
