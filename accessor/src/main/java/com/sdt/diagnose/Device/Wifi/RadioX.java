package com.sdt.diagnose.Device.Wifi;

import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.ArrayMap;

import com.sdt.annotations.Tr369Get;
import com.sdt.annotations.Tr369Set;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.NetworkUtils;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.database.DbManager;
import com.sdt.diagnose.net.NetworkApiManager;

import java.util.Arrays;

public class RadioX {
    private static final String TAG = "RadioX";
    Context mContext = GlobalContext.getContext();
    private final WifiManager mWifiManager = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
    private final static String REFIX = "Device.WiFi.Radio.";
    private final static String REGEX = "\\.";

    @Tr369Get("Device.WiFi.Radio.")
    public String SK_TR369_GetRadioX(String path) {
        return handleRadioX(path);
    }

    private String handleRadioX(String path) {
        String result = null;
        String[] paramsArr = null;
        if (path.startsWith(REFIX)) {
            paramsArr = path.replace(REFIX, "").split(REGEX);
            int index = Integer.parseInt(paramsArr[0]);
            int resultNumber = NetworkApiManager.getInstance().getNetworkApi().getWiFiRadioNumberOfEntries();
            LogUtils.d(TAG, "handleRadioX: " + Arrays.toString(paramsArr));
            if (index <= 0 || index > resultNumber) {
                return result;
            }
            if (paramsArr.length == 2) {
                switch (paramsArr[1]) {
                    case "Enable":
                        result = String.valueOf(mWifiManager.isWifiEnabled());
                        break;
                    case "Status":
                        result = getRadioStatus(path);
                        break;
                    case "Alias":
                        result = DbManager.getDBParam(path);
                        break;
                    case "Name":
                        result = getRadioName();
                        break;
                    case "ChannelsInUse":
                        result = getRadioChannelsInUse(path);
                        break;
                    case "Channel":
                        result = getRadioChannel(path);
                        break;
                    case "TransmitPowerSupported":
                        break;
                    case "TransmitPower":
                        break;
                    case "MaxBitRate":
                        result = getRadioMaxBitRate(path);
                        break;
                    case "SupportedFrequencyBands":
                        break;
                    case "OperatingFrequencyBand":
                        result = operatingFrequencyBand(path);
                        break;
                    case "CurrentOperatingChannelBandwidth":
                        break;
                    default:
                        break;
                }
            } else if (paramsArr.length == 3) {
                if ("Stats".equalsIgnoreCase(paramsArr[1])) {
                    switch (paramsArr[2]) {
                        case "BytesSent":
                        case "BytesReceived":
                        case "PacketSend":
                        case "PacketReceived":
                        case "ErrorReceived":
                        case "RxErrors":
                            ArrayMap<String, String> arrayMap = NetworkApiManager.getInstance().getNetworkApi().getWiFiRadioStats(index);
                            result = arrayMap.get(paramsArr[2]);
                            break;
                        case "ConnectionUpTime":
                            break;
                        case "PhyRate":
                            break;
                        case "Noise":
                            break;
                        default:
                            break;
                    }
                }
            }
        }
        return result;
    }

    @Tr369Set("Device.WiFi.Radio.1.Enable")
    public boolean SK_TR369_SetRadioEnable(String path, String value) {
        boolean status = mWifiManager.isWifiEnabled();
        if (("false").equalsIgnoreCase(value)) {
            if (status) {
                return mWifiManager.setWifiEnabled(false);
            }
        } else if (("true").equalsIgnoreCase(value)) {
            if (!status) {
                return mWifiManager.setWifiEnabled(true);
            }
        }
        return false;
    }

    @Tr369Set("Device.WiFi.Radio.1.Alias")
    public boolean SK_TR369_SetSsidAlias(String path, String value) {
        LogUtils.d(TAG, "SetSsidAlias path: " + path + ", value: " + value);
        return (DbManager.setDBParam(path, value) == 0);
    }

    public String getRadioStatus(String path) {
        return NetworkUtils.getWiFiRadioStatus(GlobalContext.getContext());
    }

    public String getRadioChannelsInUse(String path) {
        LogUtils.d(TAG, "getRadioChannelsInUse path: " + path);
        return NetworkApiManager.getInstance().getNetworkApi().getWiFiRadioChannelsInUse(GlobalContext.getContext(), 0);
    }

    public String getRadioChannel(String path) {
        LogUtils.d(TAG, "getRadioChannel path: " + path);
        int value = NetworkApiManager.getInstance().getNetworkApi().getWiFiRadioChannel(GlobalContext.getContext(), 0);
        return String.valueOf(value);
    }

    public String getRadioName() {
        return NetworkApiManager.getInstance().getNetworkApi().getWiFiRadioName(0);
    }

    public String getRadioMaxBitRate(String path) {
        LogUtils.d(TAG, "getRadioMaxBitRate path: " + path);
        String[] status = NetworkApiManager.getInstance().getNetworkApi().getWiFiMaxBitRate(GlobalContext.getContext());
        if (status != null && status.length > 0) {
            return status[0];
        }
        return null;
    }

    public String operatingFrequencyBand(String path) {
        LogUtils.d(TAG, "operatingFrequencyBand path: " + path);
        return NetworkApiManager.getInstance().getNetworkApi().getWiFiRadioOperatingFrequencyBand(GlobalContext.getContext(), 0);
    }

}
