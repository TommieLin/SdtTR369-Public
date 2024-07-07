package com.sdt.diagnose.net;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.NetworkUtils;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.opentr369.OpenTR369Native;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wifi 和 路由器相关接口在这里
 */
public class NetworkApi {
    private static final String TAG = "NetworkApi";
    private DeviceWiFiScan mDeviceWiFiScan;

    private String getChannelByFrequency(int frequency) {

        String channel = "-1";
        switch (frequency) {
            case 2412:
                channel = "1";
                break;
            case 2417:
                channel = "2";
                break;
            case 2422:
                channel = "3";
                break;
            case 2427:
                channel = "4";
                break;
            case 2432:
                channel = "5";
                break;
            case 2437:
                channel = "6";
                break;
            case 2442:
                channel = "7";
                break;
            case 2447:
                channel = "8";
                break;
            case 2452:
                channel = "9";
                break;
            case 2457:
                channel = "10";
                break;
            case 2462:
                channel = "11";
                break;
            case 2467:
                channel = "12";
                break;
            case 2472:
                channel = "13";
                break;
            case 2484:
                channel = "14";
                break;
            case 5180:
                channel = "36";
                break;
            case 5200:
                channel = "40";
                break;
            case 5220:
                channel = "44";
                break;
            case 5240:
                channel = "48";
                break;
            case 5745:
                channel = "149";
                break;
            case 5765:
                channel = "153";
                break;
            case 5785:
                channel = "157";
                break;
            case 5805:
                channel = "161";
                break;
            case 5825:
                channel = "165";
                break;
        }
        return channel;
    }


    /***************************************************************
     * Device.GatewayInfo.ProductClass
     * Identifier of the product class of the associated Internet Gateway Device.  {{empty}} indicates either that  there is no associated Internet Gateway Device that has been detected, or the Internet Gateway Device does not support the use of the  product-class parameter.
     *
     * TR-181
     * readOnly
     ***************************************************************/
    public String getGatewayProduct() {
        //TODO:sys.gatewayinfo.productclass will be set by frameowrk if option 125 support later
        String productclass = SystemProperties.get("sys.gatewayinfo.productclass", "");
        LogUtils.i(TAG, "getGatewayProduct: " + productclass);
        return productclass;
    }

    /***************************************************************
     * Device.LAN.DefaultGateway
     *
     * readOnly
     ***************************************************************/
    public String getGatewayDefaultGateway(Context context) {
        return NetworkUtils.getDefaultGateway(context);
    }

    /***************************************************************
     * Device.GatewayInfo.SerialNumber
     * Serial number of the associated Internet Gateway Device.  {{empty}} indicates that there is no associated  Internet Gateway Device that has been detected.
     *
     * TR-181
     * readOnly
     ***************************************************************/
    public String getGatewaySerialNumber() {
        //TODO:sys.gatewayinfo.serialnumber will be set by frameowrk if option 125 support later
        String serialnumber = SystemProperties.get("sys.gatewayinfo.serialnumber", "");
        LogUtils.i(TAG, "getGatewaySerialNumber: " + serialnumber);
        return serialnumber;
    }

    /***************************************************************
     * Device.GatewayInfo.ManufacturerOUI
     *  Organizationally unique identifier of the associated Internet Gateway Device.  {{pattern}}
     * {{empty}} indicates that there is  no associated Internet Gateway Device that has been detected.
     *
     * TR-181
     * readOnly
     ***************************************************************/
    public String getGatewayManufacturerOUI() {
        //TODO:sys.gatewayinfo.manufactureroui will be set by frameowrk if option 125 support later
        String manufactureroui = SystemProperties.get("sys.gatewayinfo.manufactureroui", "");
        LogUtils.i(TAG, "getGatewayManufacturerOUI: " + manufactureroui);
        return manufactureroui;
    }

    /***************************************************************
     * Device.GatewayInfo.X_00604C_GATEWAY_MAC
     *
     * readOnly
     ***************************************************************/
    public String getGatewayMac(Context context) {
        return NetworkUtils.getGatewayMac(context);
    }

    public String getServerAddress(Context context) {
        return NetworkUtils.getRouteIPAddress(context);
    }

    /***************************************************************
     * Device.Ethernet.Interface.{i}.MaxBitRate
     *
     *
     * readWrite
     ***************************************************************/
    public String[] getEthernetMaxBitRate() {
        String[] arr = new String[1];
        LogUtils.i(TAG, "getEthernetMaxBitRate: " + arr[0]);
        return arr;
    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.MaxBitRate
     *
     * readOnly
     ***************************************************************/
    public String[] getWiFiMaxBitRate(Context context) {
        String[] arr = new String[1];
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        if (null != mWifiInfo) {
            arr[0] = String.valueOf(mWifiInfo.getMaxSupportedRxLinkSpeedMbps());
        }
        if (null != arr[0]) {
            LogUtils.i(TAG, "getWiFiMaxBitRate: " + arr[0]);
        }
        return arr;
    }

    /***************************************************************
     * Device.X_00604C_wifi.1.SSID
     * WifiManager->getConnectionInfo->getSSID
     *
     * readOnly ?
     ***************************************************************/
    public String getWiFiSsid(Context context) {
        if (!NetworkUtils.isWifiConnected(context)) return null;
        String ssid = "";
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        if (null != mWifiInfo) {
            ssid = mWifiInfo.getSSID();
        }
        if (!TextUtils.isEmpty(ssid)) {
            LogUtils.i(TAG, "getWiFiSsid: " + ssid);
            if (ssid.contains("<")) {
                ssid = ssid.replace("<", "");
            }
            if (ssid.contains(">")) {
                ssid = ssid.replace(">", "");
            }
        }
        return ssid;
    }

    /***************************************************************
     * Device.X_00604C_wifi.1.BSSID
     * WifiManager->getConnectionInfo->getBSSID
     *
     * readOnly ?
     ***************************************************************/
    public String getWiFiBssid(Context context) {
        String bssid = "";
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        if (null != mWifiInfo) {
            bssid = mWifiInfo.getBSSID();
        }
        if (null != bssid) {
            LogUtils.i(TAG, "getWiFiBssid: " + bssid);
        }
        return bssid;
    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.PossibleChannels
     *
     * As discussed earlier, this API has been abandoned
     * get it from wpa_cli, and then write to the database
     * for example: wpa_cli  get_capability channels
     *			Mode[G] Channels: 1 2 3 4 5 6 7 8 9 10 11 12 13 14
     *			Mode[A] Channels: 36 40 44 48 52 56 60 64 100 104 108 112 116 120 124 128 132 136 140 144 149 153 157 161 165 0 171 172 173 174 175 176 177 178 179 0 0 0 0 0
     *			Mode[B] Channels: 1 2 3 4 5 6 7 8 9 10 11 12 13 14
     * readOnly
     ***************************************************************/
    public String[] getWiFiChannel(Context context) {
        String[] arr = new String[1];
        int Frequency = 0;
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        if (null != mWifiInfo) {
            Frequency = mWifiInfo.getFrequency();
        }
        arr[0] = getChannelByFrequency(Frequency);
        if (null != arr[0]) {
            LogUtils.i(TAG, "getWiFiChannel: " + arr[0]);
        }
        return arr;
    }

    /***************************************************************
     * Device.X_00604C_wifi.1.bss.{i}.MaxRate
     *
     *
     * readOnly
     ***************************************************************/
    public int[] getWiFiBssMaxRate(Context context) {
        int[] arr = new int[1];
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        if (null != mWifiInfo) {
            arr[0] = mWifiInfo.getLinkSpeed();
        }
        LogUtils.i(TAG, "getWiFiMaxBitRate: " + arr[0]);
        return arr;
    }

    /***************************************************************
     * Device.X_00604C_wifi.1.Key
     *
     * As discussed earlier, this API has been abandoned
     * it can be get and update by Broadcast:WifiManager.CONFIGURED_NETWORKS_CHANGED_ACTION and Extra WifiManager.EXTRA_WIFI_CONFIGURATION_PSK
     *
     * readOnly ?
     ***************************************************************/
    public String getWiFiPwd() {
        return null;
    }


    /***************************************************************
     * Device.WiFi.RadioNumberOfEntries
     * The number of entries in the Radio table.
     *
     * TR-181
     *
     * readOnly
     *
     * @return Entrie number here return default value 1
     *    -1 mean failure
     *    -2 mean not support
     ***************************************************************/
    public int getWiFiRadioNumberOfEntries() {
        int entries = 1;
        LogUtils.i(TAG, "getWiFiRadioNumberOfEntries: " + entries);
        return entries;
    }

    /***************************************************************
     * Device.WiFi.SSIDNumberOfEntries
     * The number of entries in the SSID table.
     *
     * TR-181
     *
     * readOnly
     *
     * @return Entrie number same as Device.WiFi.RadioNumberOfEntries
     *    -1 mean failure
     *    -2 mean not support
     ***************************************************************/
    public int getWiFiSSIDNumberOfEntries() {
        int entries = 1;
        LogUtils.i(TAG, "getWiFiSSIDNumberOfEntries: " + entries);
        return entries;
    }

    /***************************************************************
     * Device.WiFi.AccessPointNumberOfEntries
     * The number of entries in the AccessPoint table.
     *
     * TR-181
     *
     * readOnly
     *
     * @return Entrie number
     *    -1 mean failure
     *    -2 mean not support
     ***************************************************************/
    public int getWiFiAccessPointNumberOfEntries() {
        int entries = 0;
        LogUtils.i(TAG, "getWiFiAccessPointNumberOfEntries: " + entries);
        return entries;
    }

    /***************************************************************
     * Device.WiFi.EndPointNumberOfEntries
     * The number of entries in the EndPoint table.
     *
     * TR-181
     *
     * readOnly
     *
     * @return Entrie number
     *    -1 mean failure
     *    -2 mean not support
     ***************************************************************/
    public int getWiFiEndPointNumberOfEntries() {
        int entries = -2;
        LogUtils.i(TAG, "getWiFiEndPointNumberOfEntries: " + entries + "((not support))");
        return entries;
    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.Enable
     * Enables or disables the radio.
     *
     * TR-181
     *
     * readWrite
     *
     * @return status enable or disable
     ***************************************************************/
    public boolean getWiFiRadioEnable(Context context, int index) {
        boolean enable = false;
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (null == mWifiManager) {
            return false;
        }
        enable = mWifiManager.isWifiEnabled();
        LogUtils.i(TAG, "getWiFiRadioEnable get: " + enable);
        return enable;
    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.Enable
     * Enables or disables the radio.
     *
     * TR-181
     *
     * readWrite
     *
     * @return set status sucess or failure
     ***************************************************************/
    public boolean getWiFiRadioEnable(Context context, int index, boolean enable) {
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (null == mWifiManager) {
            return false;
        }

        if (mWifiManager.isWifiEnabled() == enable) {
            LogUtils.i(TAG, "getWiFiRadioEnable set: at same status no need to set");
            return true;
        }
        LogUtils.i(TAG, "getWiFiRadioEnable set[" + index + "]: " + enable);
        return mWifiManager.setWifiEnabled(enable);

    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.Alias
     * A non-volatile handle used to reference this instance. Alias provides a mechanism for an ACS to label this instance for future reference..
     *
     * TR-181
     *
     * readWrite
     *
     * @return return alias
     ***************************************************************/
    public String getWiFiRadioAlias(int index) {
        String key_name = String.format("persist.sys.wifi.radio.%s.alias", index);
        String key_value = SystemProperties.get(key_name, null);
        LogUtils.i(TAG, "getWiFiRadioAlias get[" + index + "]: " + key_value);
        return key_value;
    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.Alias
     * A non-volatile handle used to reference this instance. Alias provides a mechanism for an ACS to label this instance for future reference..
     *
     * TR-181
     *
     * readWrite
     *
     * @return
     ***************************************************************/
    public void getWiFiRadioAlias(int index, String Alias) {
        String key_name = String.format("persist.sys.wifi.radio.%s.alias", index);
        LogUtils.i(TAG, "getWiFiRadioAlias set[" + index + "] " + key_name + " to " + Alias);
        SystemProperties.set(key_name, Alias);
    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.Name
     * The textual name of the radio as assigned by the CPE.
     *
     * TR-181
     *
     * readOnly
     *
     * @return return Name
     ***************************************************************/
    public String getWiFiRadioName(int index) {
        String key_value = SystemProperties.get("wifi.interface", "wlan0");
        LogUtils.i(TAG, "getWiFiRadioName[" + index + "]: " + key_value);
        return key_value;
    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.ChannelsInUse
     * Comma-separated list (maximum list length 1024) of strings. List items represent channels that the radio determines to be currently in use (including any that it is using itself).
     *
     * TR-181
     *
     * readOnly
     ***************************************************************/
    public String getWiFiRadioChannelsInUse(Context context, int index) {
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (null == mWifiManager) {
            return "-1";
        }
        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        if (null == mWifiInfo) {
            return "-1";
        }
        int Frequency = mWifiInfo.getFrequency();
        String Channels = getChannelByFrequency(Frequency);
        LogUtils.i(TAG, "getWiFiRadioChannelsInUse Frequency: " + Frequency +
                "[" + index + "]: " + Channels);
        return Channels;
    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.Channel
     * The current radio channel used by the connection. To request automatic channel selection, set AutoChannelEnable to true.
     *
     * TR-181
     *
     * readWrite but readOnly here
     ***************************************************************/
    public int getWiFiRadioChannel(Context context, int index) {
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (null == mWifiManager) {
            return -1;
        }

        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        if (null == mWifiInfo) {
            return -1;
        }
        int Frequency = mWifiInfo.getFrequency();
        String Channels = getChannelByFrequency(Frequency);
        LogUtils.i(TAG, "getWiFiRadioChannel[" + index + "] : " + Channels);
        return Integer.parseInt(Channels);
    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.TransmitPowerSupported
     * TComma-separated list (maximum list length 64) of integers (value -1 to 100). List items represent supported transmit power levels as percentage of full power. For example, "0,25,50,75,100".
     *
     * TR-181
     *
     * readOnly
     * can not get, default value
     ***************************************************************/
    public String getWiFiRadioTransmitPowerSupported(int index) {
        LogUtils.i(TAG, "getWiFiRadioTransmitPowerSupported[" + index + "] : 0,25,50,75,100");
        return "0,25,50,75,100";
    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.TransmitPower
     * Indicates the current transmit power level as a percentage of full power. The value MUST be one of the values reported by the TransmitPowerSupported parameter. A value of -1 indicates auto mode (automatic decision by CPE).
     *
     * TR-181
     *
     * readOnly
     * can not get, default value
     ***************************************************************/
    public String getWiFiRadioTransmitPower(int index) {
        LogUtils.i(TAG, "getWiFiRadioTransmitPower[" + index + "] : -1 as auto");
        return "-1";
    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.SupportedFrequencyBands
     * Comma-separated list of strings. List items indicate the frequency bands at which the radio can operate
     *
     * TR-181
     *
     * readOnly
     ***************************************************************/
    public String getWiFiRadioSupportedFrequencyBands(int index) {
        LogUtils.i(TAG, "getWiFiRadioSupportedFrequencyBands[" + index + "] : 2.4GHz,5GHz");
        return "2.4GHz,5GHz";
    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.OperatingFrequencyBand
     * The value MUST be a member of the list reported by the SupportedFrequencyBands parameter. Indicates the frequency band at which the radio is operating.
     *
     * TR-181
     *
     * readOnly
     ***************************************************************/
    public String getWiFiRadioOperatingFrequencyBand(Context context, int index) {
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (null == mWifiManager) {
            return "-1";
        }

        WifiInfo mWifiInfo = mWifiManager.getConnectionInfo();
        if (null == mWifiInfo) {
            return "-1";
        }

        int mFrequency = mWifiInfo.getFrequency();

        if (mWifiInfo.is24GHz()/*is24GHzWifi(mFrequency)*/) {
            LogUtils.i(TAG, "getWiFiRadioOperatingFrequencyBand[" + index + "] : 2.4GHz");
            return "2.4GHz";
        }

        if (mWifiInfo.is5GHz()/*is5GHzWifi(mFrequency)*/) {
            LogUtils.i(TAG, "getWiFiRadioOperatingFrequencyBand[" + index + "] : 5GHz");
            return "5GHz";
        }

        return "-1";
    }

    /**
     * 判断是否2.4Gwifi
     *
     * @param frequency
     * @return
     */
    public boolean is24GHzWifi(int frequency) {
        return frequency > 2400 && frequency < 2500;
    }

    /**
     * 判断是否5Gwifi
     *
     * @param frequency
     * @return
     */
    public boolean is5GHzWifi(int frequency) {
        return frequency > 4900 && frequency < 5900;
    }


    /***************************************************************
     * Device.WiFi.SSID.{i}.Enable
     * Enables or disables the SSID entry.
     *
     * TR-181
     *
     * readWrite
     *
     * @return status enable or disable
     ***************************************************************/
    public boolean getWiFiSSIDEnable(Context context, int index) {
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        boolean enable = false;

        if (null == mWifiManager) {
            return false;
        }
        enable = mWifiManager.isWifiEnabled();
        LogUtils.i(TAG, "getWiFiSSIDEnable get: " + enable);
        return enable;
    }

    /***************************************************************
     * Device.WiFi.SSID.{i}.Enable
     * Enables or disables the SSID entry.
     *
     * TR-181
     *
     * readWrite
     *
     * @return set status sucess or failure
     ***************************************************************/
    public boolean setWiFiSSIDEnable(Context context, int index, boolean enable) {
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (null == mWifiManager) {
            return false;
        }

        if (mWifiManager.isWifiEnabled() == enable) {
            LogUtils.i(TAG, "getWiFiSSIDEnable set: at same status no need to set");
            return true;
        }
        LogUtils.i(TAG, "getWiFiSSIDEnable set[" + index + "]: " + enable);
        return mWifiManager.setWifiEnabled(enable);
    }

    /***************************************************************
     * Device.WiFi.SSID.{i}.Status
     * see getWiFiRadioStatus
     *
     * TR-181
     *
     * readOnly
     ***************************************************************/
    public String getWiFiSSIDStatus(int index) {
        return NetworkUtils.getWiFiRadioStatus(GlobalContext.getContext());
    }

    /***************************************************************
     * Device.WiFi.SSID.{i}.Alias
     * A non-volatile handle used to reference this instance. Alias provides a mechanism for an ACS to label this instance for future reference.
     *
     * TR-181
     *
     * readWrite
     *
     * @return return alias
     ***************************************************************/
    public String getWiFiSSIDAlias(int index) {
        String key_name = String.format("persist.sys.wifi.SSID.%s.alias", index);
        String key_value = SystemProperties.get(key_name, null);
        LogUtils.i(TAG, "getWiFiSSIDAlias get[" + index + "]: " + key_value);
        return key_value;
    }

    /***************************************************************
     * Device.WiFi.SSID.{i}.Alias
     * A non-volatile handle used to reference this instance. Alias provides a mechanism for an ACS to label this instance for future reference.
     *
     * TR-181
     *
     * readWrite
     *
     * @return
     ***************************************************************/
    public void getWiFiSSIDAlias(int index, String Alias) {
        String key_name = String.format("persist.sys.wifi.SSID.%s.alias", index);
        LogUtils.i(TAG, "getWiFiRadioAlias set[" + index + "] " + key_name + " to " + Alias);
        SystemProperties.set(key_name, Alias);
    }

    /***************************************************************
     * Device.WiFi.SSID.{i}.Name
     * The textual name of the SSID entry as assigned by the CPE.
     *
     * TR-181
     *
     * readOnly
     ***************************************************************/
    public String getWiFiSSIDName(int index) {
        if (NetworkUtils.isWifiConnected(GlobalContext.getContext())) {
            WifiInfo wifiInfo = NetworkUtils.getConnectedWifiInfo(GlobalContext.getContext());
            if (wifiInfo == null) return null;
            String ssid = wifiInfo.getSSID();
            LogUtils.i(TAG, "getWiFiRadioName[" + index + "] : " + ssid);
            return ssid;
        }
        return null;
    }

    /***************************************************************
     * Device.WiFi.SSID.{i}.MACAddress
     * The MAC address of this interface.
     *
     * TR-181
     *
     * readOnly
     ***************************************************************/
    public String getWiFiSSIDMACAddress(Context context, int index) {
        return NetworkUtils.getWifiMacAddress();
    }

    /***************************************************************
     * Device.WiFi.EndPoint.{i}.Stats.SignalStrength
     * An indicator of radio signal strength of the downlink from the access point to the end point, measured in dBm, as an average of the last 100 packets received from the device.
     *
     * TR-181
     *
     * readOnly
     *
     * @return Entrie number
     *    -1 mean failure
     *    -2 mean not support
     ***************************************************************/
    public int getWiFiEndPointStatsSignalStrength(int index) {
        LogUtils.i(TAG, "getWiFiEndPointStatsSignalStrength: -2(not support)");
        return -2;
    }

    /***************************************************************
     * Device.WiFi.EndPoint.{i}.Stats.Retransmissions
     * The number of packets that had to be re-transmitted, from the last 100 packets sent to the access point. Multiple re-transmissions of the same packet count as one..
     *
     * TR-181
     *
     * readOnly
     *
     * @return Entrie number
     *    -1 mean failure
     *    -2 mean not support
     ***************************************************************/
    public int getWiFiEndPointStatsRetransmissions(int index) {
        LogUtils.i(TAG, "getWiFiEndPointStatsRetransmissions: -2(not support)");
        return -2;
    }

    public class DeviceWiFiScan {
        public class DeviceWiFiScanResult {
            public String Radio;
            public String SSID;
            public String BSSID;
            public String SecurityModeEnabled;
            public String EncryptionMode;
            public String OperatingFrequencyBand;
            public String SupportedStandards;
            public String OperatingStandards;
            public String OperatingChannelBandwidth;
            public int Noise;
            public int Channel;
            public int SignalStrength;

            public DeviceWiFiScanResult() {
                Radio = "802.11 ";
                SupportedStandards = "NA";
                OperatingStandards = "NA";
                OperatingFrequencyBand = "2.4GHz";
                SSID = " ";
                BSSID = " ";
                SupportedStandards = " ";
                SupportedStandards = " ";
                SecurityModeEnabled = " ";
                EncryptionMode = " ";
                OperatingChannelBandwidth = " ";
                Noise = 0;
            }

            @Override
            public String toString() {
                StringBuffer sb = new StringBuffer();
                String none = "<none>";

                sb.append("SSID: ").append(SSID).
                        append(", BSSID: ").append(BSSID).
                        append(", Radio: ").append(Radio).
                        append(", SecurityModeEnabled: ").append(SecurityModeEnabled).
                        append(", EncryptionMode: ").append(EncryptionMode).
                        append(", OperatingFrequencyBand: ").append(OperatingFrequencyBand).
                        append(", OperatingChannelBandwidth: ").append(OperatingChannelBandwidth).
                        append(", Channel: ").append(Channel).
                        append(", SignalStrength: ").append(SignalStrength);
                return sb.toString();
            }

        }

        //        public DeviceWiFiScanResult mDeviceWiFiScanResult[];
        private List<DeviceWiFiScanResult> deviceWiFiScanResults;

        public ArrayList<DeviceWiFiScanResult> getDeviceWiFiScanResults() {
            return (ArrayList<DeviceWiFiScanResult>) deviceWiFiScanResults;
        }

        public int getResultNumberOfEntries() {
            if (deviceWiFiScanResults != null) {
                return deviceWiFiScanResults.size();
            } else {
                return 0;
            }
        }

        public DeviceWiFiScan(WifiManager mWifiManager) {
            int i = 0;
            List<ScanResult> results = mWifiManager.getScanResults();
            if (null == results) {
                return;
            }
//            mDeviceWiFiScanResult = new DeviceWiFiScanResult[results.size()];
            deviceWiFiScanResults = new ArrayList<DeviceWiFiScanResult>();

            for (ScanResult result : results) {
                boolean A = false;
                boolean G = false;
                boolean N = false;
                boolean AC = false;
                boolean AES = false;
                boolean TKIP = false;

                if (result.level < -75) continue;

                DeviceWiFiScanResult mScanResult = new DeviceWiFiScanResult();
                mScanResult.BSSID = result.BSSID;
                mScanResult.SSID = result.SSID;
                mScanResult.SignalStrength = result.level;
                mScanResult.OperatingChannelBandwidth = Integer.toString(result.channelWidth);
                mScanResult.Channel = Integer.parseInt(getChannelByFrequency(result.frequency));

                if (/*is5GHzWifi(result.frequency)*/result.is5GHz()) {
                    A = true;
                    mScanResult.OperatingFrequencyBand = "5GHz";
                }

                for (int j = 0; j < result.informationElements.length; j++) {
                    if (result.informationElements[j].id == ScanResult.InformationElement.EID_HT_CAPABILITIES) {
                        N = true;
                    } else if (result.informationElements[j].id == ScanResult.InformationElement.EID_VHT_CAPABILITIES) {
                        AC = true;
                    } else {
                        G = true;
                    }
                }

                if (A) {
                    mScanResult.Radio += "a";
                    if (N) {
                        mScanResult.Radio += "/n";
                    }
                    if (AC) {
                        mScanResult.Radio += "/ac";
                    }
                } else {
                    mScanResult.Radio += "b";
                    if (G) {
                        mScanResult.Radio += "/g";
                    }
                    if (N) {
                        mScanResult.Radio += "/n";
                    }
                }

                do {
                    if (result.capabilities.contains("WPA2-PSK-WPA3-SAE")) {
                        mScanResult.SecurityModeEnabled = "WPA2-PSK-WPA3-SAE";
                        break;
                    }
                    if (result.capabilities.contains("WPA-WPA2-Enterprise")) {
                        mScanResult.SecurityModeEnabled = "WPA-WPA2-Enterprise";
                        break;
                    }
                    if (result.capabilities.contains("WPA3-Enterprise")) {
                        mScanResult.SecurityModeEnabled = "WPA3-Enterprise";
                        break;
                    }
                    if (result.capabilities.contains("WPA2-Enterprise")) {
                        mScanResult.SecurityModeEnabled = "WPA2-Enterprise";
                        break;
                    }
                    if (result.capabilities.contains("WPA-Enterprise")) {
                        mScanResult.SecurityModeEnabled = "WPA-Enterprise";
                        break;
                    }
                    if (result.capabilities.contains("WPA-WPA2")) {
                        mScanResult.SecurityModeEnabled = "WPA-WPA2";
                        break;
                    }
                    if (result.capabilities.contains("WPA3-SAE")) {
                        mScanResult.SecurityModeEnabled = "WPA3-SAE";
                        break;
                    }
                    if (result.capabilities.contains("WPA2")) {
                        mScanResult.SecurityModeEnabled = "WPA2";
                        break;
                    }
                    if (result.capabilities.contains("WPA")) {
                        mScanResult.SecurityModeEnabled = "WPA";
                        break;
                    }
                    if (result.capabilities.contains("WEP")) {
                        mScanResult.SecurityModeEnabled = "WEP";
                        break;
                    }
                    mScanResult.SecurityModeEnabled = "None";
                } while (false);

                if (result.capabilities.contains("TKIP")) {
                    TKIP = true;
                }
                if (result.capabilities.contains("CCMP")) {
                    AES = true;
                }

                if (TKIP && AES) {
                    mScanResult.EncryptionMode = "TKIP/AES";
                } else if (TKIP) {
                    mScanResult.EncryptionMode = "TKIP";
                } else if (AES) {
                    mScanResult.EncryptionMode = "AES";
                }

                LogUtils.i(TAG, "deviceWiFiScanResults[" + i + "]: " + mScanResult.toString());
                deviceWiFiScanResults.add(i++, mScanResult);
//                mDeviceWiFiScanResult[i++] = mScanResult;
            }
//            mDeviceWiFiScanResult = (DeviceWiFiScanResult[]) deviceWiFiScanResults.toArray();
        }
    }

    /***************************************************************
     * Device.WiFi.NeighboringWiFiDiagnostic.Result.{i}.
     *
     *    ResultNumberOfEntries
     *    Radio
     *    SSID
     *    BSSID
     *    Channel
     *    SignalStrength
     *    SecurityModeEnabled
     *    EncryptionMode
     *    OperatingFrequencyBand
     *    SupportedStandards
     *    OperatingStandards
     *    OperatingChannelBandwidth
     *    Noise
     *
     * Neighboring SSID table. This table models the other WiFi SSIDs that this device is able to receive.
     *
     * TR-181
     *
     * readOnly
     *
     * @return Entrie info
     ***************************************************************/
    public synchronized DeviceWiFiScan getDeviceWiFiScan(Context context) {

        if (mDeviceWiFiScan == null) {
            WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (null == mWifiManager) {
                return null;
            }
            mDeviceWiFiScan = new DeviceWiFiScan(mWifiManager);
            LogUtils.i(TAG, "mResultNumber = " + mDeviceWiFiScan.getResultNumberOfEntries());
        }

        return mDeviceWiFiScan;
    }

    /***************************************************************
     * Device.WiFi.NeighboringWiFiDiagnostic.ResultNumberOfEntries
     * TR-181
     * readOnly
     *
     * @return Entrie number here return default value 1
     *    -1 mean failure
     *    -2 mean not support
     ***************************************************************/
    public int getResultNumberOfEntries(Context context) {
        int entries = getDeviceWiFiScan(context).getResultNumberOfEntries();
        LogUtils.i(TAG, "getResultNumberOfEntries: " + entries);
        return entries;
    }

    /***************************************************************
     * Device.WiFi.NeighboringWiFiDiagnostic.DiagnosticsState.
     *
     * Indicates the availability of diagnostics data. Enumeration of:
     *         <enumeration value="None" access="readOnly"/>
     *         <enumeration value="Requested"/>
     *         <enumeration value="Canceled" optional="true"/>
     *         <enumeration value="Complete" access="readOnly"/>
     *         <enumeration value="Error" access="readOnly" optional="true"/>
     *         <enumeration value="Completed" access="readOnly" status="deprecated"/>
     *
     * TR-181
     *
     * readWrite
     *
     * @return DiagnosticsState
     ***************************************************************/
    public String getWiFiNeighboringWiFiDiagnosticDiagnosticsState(Context context) {
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (null == mWifiManager) {
            return "Error";
        }
        DeviceWiFiScan mDeviceWiFiScan = getDeviceWiFiScan(context);
        if (0 == mDeviceWiFiScan.getResultNumberOfEntries()) {
            return "None";
        }
        return "Complete";
    }

    public boolean getWiFiNeighboringWiFiDiagnosticDiagnosticsState(Context context, String DiagnosticsState) {
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (null == mWifiManager) {
            return false;
        }
        if (!DiagnosticsState.equals("Requested")) {
            return false;
        }
        return mWifiManager.startScan();
    }

    public class DeviceWiFiRadioStats {
        public String mName;
        public int mBytesSent;
        public int mBytesReceived;
        public int mPacketSend;
        public int mPacketReceived;
        public int mErrorReceived;
        public int mRxErrors;

        public DeviceWiFiRadioStats() {
            mBytesSent = -1;
            mBytesReceived = -1;
            mPacketSend = -1;
            mPacketReceived = -1;
            mErrorReceived = -1;
            mRxErrors = -1;
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Name: ").append(mName).
                    append(", BytesSent: ").append(mBytesSent).
                    append(", BytesReceived: ").append(mBytesReceived).
                    append(", PacketSend: ").append(mPacketSend).
                    append(", PacketReceived: ").append(mPacketReceived).
                    append(", ErrorReceived: ").append(mErrorReceived).
                    append(", RxErrors: ").append(mRxErrors);
            return sb.toString();
        }

    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.Stats.
     *
     *    BytesSent
     *    BytesReceived
     *    PacketSend
     *    PacketReceived
     *    ErrorReceived
     *    RxErrors
     *
     * TR-181
     *
     * read
     *
     * @return DeviceWiFiRadioStats
     ***************************************************************/
    public /*DeviceWiFiRadioStats*/ArrayMap<String, String> getWiFiRadioStats(int index) {

        ArrayMap<String, String> radioStatsMap = new ArrayMap();
        DeviceWiFiRadioStats mDeviceWiFiRadioStats = new DeviceWiFiRadioStats();

        String statBuffer = OpenTR369Native.GetNetInterfaceStatus("wlan0");

        LogUtils.i(TAG, "getWiFiWiFiStats statBuffer: " + statBuffer.toString());
        String result[] = statBuffer.split(";");

        if (6 <= result.length) {
            radioStatsMap.put("BytesSent", result[0]);
            radioStatsMap.put("BytesReceived", result[1]);
            radioStatsMap.put("PacketSend", result[2]);
            radioStatsMap.put("PacketReceived", result[3]);
            radioStatsMap.put("ErrorReceived", result[4]);
            radioStatsMap.put("RxErrors", result[5]);
//            mDeviceWiFiRadioStats.nBytesSent = Integer.parseInt(result[0]);
//            mDeviceWiFiRadioStats.nBytesReceived = Integer.parseInt(result[1]);
//            mDeviceWiFiRadioStats.nPacketSend = Integer.parseInt(result[2]);
//            mDeviceWiFiRadioStats.nPacketReceived = Integer.parseInt(result[3]);
//            mDeviceWiFiRadioStats.nErrorReceived = Integer.parseInt(result[4]);
//            mDeviceWiFiRadioStats.nRxErrors = Integer.parseInt(result[5]);
        }

        LogUtils.i(TAG, "getWiFiWiFiStats: " + mDeviceWiFiRadioStats);
        return radioStatsMap;

    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.Stats.Noise
     *
     *
     * TR-181
     *
     * read
     *
     * @return Noise
     ***************************************************************/
    public int getWiFiRadioNoise(int index) {
        int nNoise = OpenTR369Native.GetWirelessNoise("wlan0");
        LogUtils.i(TAG, "getWiFiRadioNoise: " + nNoise);
        return nNoise;

    }

    public int getRssi(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager != null) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null) {
                return wifiInfo.getRssi();
            }
        }
        return -100;
    }

    private DeviceWiFiRadioStats getRadioStats(String param, int index) {
//        String readStr = FileUtils.readFileToStr("/proc/net/dev");
//        LogUtils.d(TAG, "getRadioStats readStr = " + readStr);
        DeviceWiFiRadioStats deviceWiFiRadioStats = new DeviceWiFiRadioStats();

        String line = null;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/net/dev"));
            while (null != (line = reader.readLine())) {
                LogUtils.d(TAG, "line = " + line);
                String regex = ":";
                if (line.contains(regex)) {
                    String[] lineArr = line.trim().split(regex);
//                    LogUtils.d(TAG, "getRadioStats lineArr = " + Arrays.toString(lineArr));
                    if (lineArr.length > 1 && lineArr[0].equalsIgnoreCase(param)) {
                        String content = lineArr[1];
                        String[] params = content.trim().split("\\s+");
                        String formatter = "link[%s]: face:%s Receive[bytes:%s packets:%s errs:%s"
                                + " drop:%s fifo:%s frame:%s compressed:%s multicast:%s] "
                                + "Transmit[bytes:%s packets:%s";
                        String result = String.format(formatter, param, lineArr[0], params[0], params[1], params[2], params[3], params[4]
                                , params[5], params[6], params[7], params[8], params[9]);
                        LogUtils.d(TAG, "getRadioStats params: " + Arrays.toString(params));
                        LogUtils.d(TAG, "getRadioStats result: " + result);

                        deviceWiFiRadioStats.mName = param;
                        deviceWiFiRadioStats.mBytesSent = Integer.parseInt(params[8]);
                        deviceWiFiRadioStats.mBytesReceived = Integer.parseInt(params[0]);
                        deviceWiFiRadioStats.mPacketSend = Integer.parseInt(params[9]);
                        deviceWiFiRadioStats.mPacketReceived = Integer.parseInt(params[1]);
                        deviceWiFiRadioStats.mErrorReceived = Integer.parseInt(params[3]);
                        deviceWiFiRadioStats.mRxErrors = Integer.parseInt(params[2]);
                        return deviceWiFiRadioStats;
                    }
                }
            }
        } catch (IOException e) {
            LogUtils.d(TAG, "getRadioStats error, " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    LogUtils.d(TAG, "getRadioStats finally error, " + e.getMessage());
                }
            }
        }
        return null;
    }

}
