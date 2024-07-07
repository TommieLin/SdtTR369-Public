package com.sdt.diagnose.Device.USB;

import android.content.Context;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.text.TextUtils;

import com.sdt.annotations.Tr369Get;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.IProtocolArray;
import com.sdt.diagnose.common.ProtocolPathUtils;
import com.sdt.diagnose.common.log.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * This class for protocol:
 * Device.USB.USBHosts.Host.HostNumberOfEntries
 * Device.USB.USBHosts.Host.{i}.DeviceNumberOfEntries
 * Device.USB.USBHosts.Host.{i}.Enable
 * Device.USB.USBHosts.Host.{i}.Name
 * Device.USB.USBHosts.Host.{i}.Device.{i}.SerialNumber
 * Device.USB.USBHosts.Host.{i}.Device.{i}.Manufacturer
 * Device.USB.USBHosts.Host.{i}.Device.{i}.DeviceNumber
 * Device.USB.USBHosts.Host.{i}.Device.{i}.USBVersion
 * Device.USB.USBHosts.Host.{i}.Device.{i}.DeviceProtocol
 * Device.USB.USBHosts.Host.{i}.Device.{i}.ProductID
 * Device.USB.USBHosts.Host.{i}.Device.{i}.VendorID
 */
public class HostX implements IProtocolArray<UsbDevice> {
    private static final String TAG = "HostX";
    private final static String REFIX = "Device.USB.USBHosts.Host.";

    private String handleHostInfo(String path) {
        return ProtocolPathUtils.getInfoFromArray(REFIX, path, this);
    }

    @Tr369Get("Device.USB.USBHosts.Host.")
    public String SK_TR369_GetHostInfo(String path) {
        if ("Device.USB.USBHosts.Host.HostNumberOfEntries".equals(path)) {
            return getUSBHostNumberOfEntries();
        }
        return handleHostInfo(path);
    }

    @Override
    public List<UsbDevice> getArray() {
        UsbManager usbManager = (UsbManager) GlobalContext.getContext().getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
        List<UsbDevice> usbDeviceList = new ArrayList<>();
        if (deviceList != null && !deviceList.isEmpty()) {
            LogUtils.d(TAG, "deviceList.size(): " + deviceList.size());
            for (String key : deviceList.keySet()) {
                LogUtils.d(TAG, "key: " + key);
                String[] all = key.split("/");
                int index = Integer.parseInt(all[4]);
                LogUtils.d(TAG, "index: " + index);
                UsbDevice usbDevice = deviceList.get(key);
                if (usbDeviceList.size() == 0 && index == 2) {
                    usbDeviceList.add(null);
                    usbDeviceList.add(usbDevice);
                } else if (usbDeviceList.size() == 2 && index == 1) {
                    usbDeviceList.set(0, usbDevice);
                } else {
                    usbDeviceList.add(usbDevice);
                }
            }
        }
        return usbDeviceList;
    }

    @Override
    public String getValue(UsbDevice usbDevice, String[] paramsArr) {
        int index = toInt(paramsArr[0]);
        if (index < 1) return null;

        if (paramsArr.length == 2) {
            switch (paramsArr[1]) {
                case "DeviceNumberOfEntries":
                    return getUSBHostDeviceNumberOfEntries();
                case "Enable":
                    return Boolean.toString(true);
                case "Name":
                    return usbDevice.getDeviceName();
                default:
                    break;
            }
            return null;
        } else if (paramsArr.length > 3 && "Device".equals(paramsArr[1])) {
            int index2 = toInt(paramsArr[2]);
            String forthParam = paramsArr[3];
            return getHostDeviceInfo(forthParam, usbDevice);
        }
        return null;
    }

    private String getHostDeviceInfo(String forthParam, UsbDevice usbDevice) {
        if (TextUtils.isEmpty(forthParam)) {
            //Todo report error.
            return null;
        }
        switch (forthParam) {
            case "SerialNumber":
                return usbDevice.getSerialNumber();
            case "Manufacturer":
                return usbDevice.getManufacturerName();
            case "ProductClass":
                return usbDevice.getProductName();
//            case "DeviceNumber":
//                return usbDevice.getInterfaceCount();
            case "USBVersion":
                return usbDevice.getVersion();
            case "DeviceProtocol":
                return String.valueOf(usbDevice.getDeviceProtocol());
            case "ProductID":
                return String.valueOf(usbDevice.getProductId());
            case "VendorID":
                return String.valueOf(usbDevice.getVendorId());
            default:
                break;
        }
        return null;
    }

    private String getUSBHostNumberOfEntries() {
        return String.valueOf(2);
    }

    private String getUSBHostDeviceNumberOfEntries() {
        try {
            Context context = GlobalContext.getContext();
            if (null != context) {
                UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
                HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();
                if (deviceList != null && !deviceList.isEmpty()) {
                    return String.valueOf(deviceList.size());
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getUSBHostDeviceNumberOfEntries error, " + e.getMessage());
        }
        return String.valueOf(0);
    }

    private int toInt(String Number) {
        int index = 0;
        try {
            index = Integer.parseInt(Number);
        } catch (NumberFormatException e) {
            //Todo report error.
        }
        return index;
    }

}

