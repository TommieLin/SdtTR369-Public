package com.sdt.diagnose.common;

import static android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED;
import static android.net.NetworkCapabilities.NET_CAPABILITY_CAPTIVE_PORTAL;
import static android.net.NetworkCapabilities.NET_CAPABILITY_PARTIAL_CONNECTIVITY;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.EthernetManager;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.RouteInfo;
import android.net.StaticIpConfiguration;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.SystemProperties;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import com.android.internal.util.ArrayUtils;
import com.sdt.diagnose.common.bean.NetworkStatisticsInfo;
import com.sdt.diagnose.common.log.LogUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.RandomAccessFile;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

public class NetworkUtils {
    private static final String TAG = "NetworkUtils";
    public static final String WIFI_SECURITY_PSK = "PSK";
    public static final String WIFI_SECURITY_EAP = "EAP";
    public static final String WIFI_SECURITY_SAE = "SAE";
    public static final String WIFI_SECURITY_OWE = "OWE";
    public static final String WIFI_SECURITY_SUITE_B_192 = "SUITE_B_192";

    /*
     * NOTE: These constants for security and PSK types are saved to the bundle in saveWifiState,
     * and sent across IPC. The numeric values should remain stable, otherwise the changes will need
     * to be synced with other unbundled users of this library.
     */
    public static final int SECURITY_NONE = 0;
    public static final int SECURITY_WEP = 1;
    public static final int SECURITY_PSK = 2;
    public static final int SECURITY_EAP = 3;
    public static final int SECURITY_OWE = 4;
    public static final int SECURITY_SAE = 5;
    public static final int SECURITY_EAP_SUITE_B = 6;
    public static final int SECURITY_MAX_VAL = 7; // Has to be the last

    public static boolean isWifiEnabled(WifiManager mWifiManager) {
        return mWifiManager.getWifiState() == WifiManager.WIFI_STATE_ENABLED;
    }

    /**
     * 返回网络连接类型，Ethernet、WIFI
     *
     * @param context
     * @return
     */
    public static String getActiveNetworkType(Context context) {
        String type = "None";
        ConnectivityManager mConnectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network net = mConnectivityManager.getActiveNetwork();
        if (net == null) {
            return type;
        }
        NetworkInfo networkInfo = mConnectivityManager.getNetworkInfo(net);
        if (networkInfo != null) {
            if (networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                type = "Ethernet";
            } else if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                type = "WiFi";
            }
        }
        return type;
    }

    public static boolean isEthernetConnected(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                if (capabilities != null) {
                    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET);
                }
            }
        }
        return false;
    }

    public static boolean isEthernetAvailable(Context context) {
        boolean ret = false;
        try {
            if (context != null) {
                NetworkInfo mWiFiNetworkInfo = null;
                ConnectivityManager mConnectivityManager =
                        (ConnectivityManager) context.getSystemService(
                                Context.CONNECTIVITY_SERVICE);
                Network[] networks = mConnectivityManager.getAllNetworks();
                for (Network network : networks) {
                    mWiFiNetworkInfo = mConnectivityManager.getNetworkInfo(network);
                    if (null != mWiFiNetworkInfo) {
                        if (mWiFiNetworkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                            ret = mWiFiNetworkInfo.isAvailable();
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "isEthernetAvailable call failed, " + e.getMessage());
        }
        return ret;
    }

    /**
     * 获取当前连接网络的IP
     *
     * @param context
     * @return
     */
    public static String getIpv4Address(Context context) {
        String Ipv4Addr;
        if (isWifiConnected(context)) {
            Ipv4Addr = getWifiIPv4Address(context);
        } else {
            Ipv4Addr = getEthernetIPv4Address(context);
        }
        LogUtils.d(TAG, "getIpv4Address: " + Ipv4Addr);
        return (Ipv4Addr != null && !Ipv4Addr.isEmpty()) ? Ipv4Addr : "0.0.0.0";
    }

    public static String getIpv6Address(Context context) {
        String Ipv6Addr;
        if (isWifiConnected(context)) {
            Ipv6Addr = getWifiIPv6Address(context);
        } else {
            Ipv6Addr = getEthernetIPv6Address(context);
        }
        LogUtils.d(TAG, "getIpv6Address: " + Ipv6Addr);
        return (!Ipv6Addr.isEmpty()) ? Ipv6Addr : "0.0.0.0";
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            Network network = connectivityManager.getActiveNetwork();
            if (network != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
                if (capabilities != null) {
                    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
                }
            }
        }
        return false;
    }

    public static String getWifiIPv4Address(Context context) {
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            WifiManager wifiManager = context.getSystemService(WifiManager.class);
            Network network = wifiManager.getCurrentNetwork();

            final LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            List<LinkAddress> linkAddressList = linkProperties.getLinkAddresses();
            for (LinkAddress linkAddress : linkAddressList) {
                InetAddress inetAddress = linkAddress.getAddress();
                LogUtils.d(TAG, "getWifiIPv4Address inetAddress: " + inetAddress
                        + ", isIpv4: " + linkAddress.isIpv4());
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    return inetAddress.getHostAddress();
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getWifiIPv4Address call failed, " + e.getMessage());
        }
        return "0.0.0.0";
    }

    public static String getWifiIPv6Address(Context context) {
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            WifiManager wifiManager = context.getSystemService(WifiManager.class);
            Network network = wifiManager.getCurrentNetwork();
            final StringBuilder sb = new StringBuilder();
            boolean gotAddress = false;

            final LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            List<LinkAddress> linkAddressList = linkProperties.getLinkAddresses();
            for (LinkAddress linkAddress : linkAddressList) {
                InetAddress inetAddress = linkAddress.getAddress();
                LogUtils.d(TAG, "getWifiIPv6Address inetAddress: " + inetAddress
                        + ", isIpv6: " + linkAddress.isIpv6());
                if (/*!inetAddress.isLoopbackAddress() &&*/ inetAddress instanceof Inet6Address) {
//                    return inetAddress.getHostAddress();
                    if (gotAddress) {
                        sb.append(" ");
                    }
                    String hostAddress = inetAddress.getHostAddress();
                    sb.append(hostAddress);
                    LogUtils.d(TAG, "getWifiIPv6Address hostAddress: " + hostAddress);
                    gotAddress = true;
                }
            }
            return (gotAddress) ? sb.toString() : "";
        } catch (Exception e) {
            LogUtils.e(TAG, "getWifiIPv6Address call failed, " + e.getMessage());
        }
        return "";
    }

    public static String getEthernetIPv4Address(Context context) {
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final Network network = getFirstEthernet(context);
            final LinkProperties linkProperties = connectivityManager.getLinkProperties(network);

            List<LinkAddress> linkAddressList = linkProperties.getLinkAddresses();
            for (LinkAddress linkAddress : linkAddressList) {
                InetAddress inetAddress = linkAddress.getAddress();
                LogUtils.d(TAG, "getEthernetIPv4Address inetAddress: " + inetAddress
                        + ", isIpv4: " + linkAddress.isIpv4());
                if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                    return inetAddress.getHostAddress();
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getEthernetIPv4Address call failed, " + e.getMessage());
        }
        return "0.0.0.0";
    }

    public static String getEthernetIPv6Address(Context context) {
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final Network network = getFirstEthernet(context);
            final LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
            final StringBuilder sb = new StringBuilder();
            boolean gotAddress = false;

            List<LinkAddress> linkAddressList = linkProperties.getLinkAddresses();
            for (LinkAddress linkAddress : linkAddressList) {
                InetAddress inetAddress = linkAddress.getAddress();
                LogUtils.d(TAG, "getEthernetIPv6Address inetAddress: " + inetAddress
                        + ", isIpv6: " + linkAddress.isIpv6());
                if (/*!inetAddress.isLoopbackAddress() &&*/ inetAddress instanceof Inet6Address) {
//                    return inetAddress.getHostAddress();
                    if (gotAddress) {
                        sb.append(" ");
                    }
                    String hostAddress = inetAddress.getHostAddress();
                    sb.append(hostAddress);
                    LogUtils.d(TAG, "getEthernetIPv6Address hostAddress: " + hostAddress);
                    gotAddress = true;
                }
            }
            return (gotAddress) ? sb.toString() : "";
        } catch (Exception e) {
            LogUtils.e(TAG, "getEthernetIPv6Address call failed, " + e.getMessage());
        }
        return "";
    }

    private static Network getFirstEthernet(Context context) {
        try {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            final Network[] networks = connectivityManager.getAllNetworks();
            for (final Network network : networks) {
                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                if (networkInfo != null
                        && networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET) {
                    return network;
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getFirstEthernet call failed, " + e.getMessage());
        }
        return null;
    }

    /**
     * 获取当前网络的mac，不分WiFi还是Ethernet都可以用
     *
     * @param networkType
     * @return
     */
    public static String getNetworkMac(String networkType) {
        try {
//            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            NetworkInterface networkInterface = NetworkInterface.getByName(networkType);
//            LogUtils.d(TAG, "NetworkInterfaces has: " + all.size());
            if (networkInterface == null) return "00:00:00:00:00:00";
            byte[] macBytes = networkInterface.getHardwareAddress();
            StringBuilder res1 = new StringBuilder();
            for (byte b : macBytes) {
                res1.append(String.format("%02X:", b));
            }
            if (res1.length() > 0) {
                res1.deleteCharAt(res1.length() - 1);
            }
            LogUtils.d(TAG, "getNetworkMac type: " + networkType + " mac: " + res1);
            return res1.toString().toLowerCase();
        } catch (Exception e) {
            LogUtils.e(TAG, "getNetworkMac call failed, " + e.getMessage());
        }
        return "00:00:00:00:00:00";
    }

    public static String getWifiMacAddress() {
        return getNetworkMac("wlan0");
    }

    public static String getEthernetMacAddress() {
        String macAddress = getMacAddress();
        if (macAddress.length() == 0) {
            LogUtils.e(TAG, "Unable to read ethernet mac address from system properties");
            macAddress = getEthernetMac();
            if (macAddress == null || macAddress.length() == 0) {
                LogUtils.e(TAG, "Unable to read ethernet mac address from ConnectivityManager");
                macAddress = getNetworkMac("eth0");
            }
        }
        return macAddress;
    }

    private static String getMacAddress() {
        return SystemProperties.get("ro.boot.mac", "").toLowerCase();
    }

    private static String getEthernetMac() {
        String ethMac = "";
        ConnectivityManager mConnectivityManager = (ConnectivityManager) GlobalContext.getContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = mConnectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
        LogUtils.e(TAG, "Get ethernet mac address from ConnectivityManager");
        if (info != null) {
            ethMac = info.getExtraInfo();
            LogUtils.d(TAG, "Get ethernet mac: " + ethMac);
        } else {
            LogUtils.e(TAG, "NetworkInfo is null!");
        }
        return ethMac;
    }

    /**
     * android.permission.LOCAL_MAC_ADDRESS
     */
    public static String getWifiMac(Context context) {
        String wifiMac = "";
        if (context != null) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            try {
                wifiMac = wifiManager.getConnectionInfo().getMacAddress();
                LogUtils.d(TAG, "wifiMac: " + wifiMac);
            } catch (Exception e) {
                LogUtils.e(TAG, "getWifiMac call failed, " + e.getMessage());
            }
        }
        return wifiMac;
    }

    public static WifiInfo getConnectedWifiInfo(Context context) {
        WifiInfo wifiInfo = null;
        if (context != null) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            try {
                wifiInfo = wifiManager.getConnectionInfo();
                LogUtils.d(TAG, "wifiInfo: " + wifiInfo);
            } catch (Exception e) {
                LogUtils.e(TAG, "getConnectedWifiInfo call failed, " + e.getMessage());
            }
        }
        return wifiInfo;
    }

    /**
     * 获取Ethernet的MAC地址
     */
    public static String getEthernetMac(Context context) {
        String ethMac = "";
        if (context != null) {
            ConnectivityManager manager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo info = manager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET);
            if (info != null) {
                ethMac = info.getExtraInfo();   //这个ExtraInfo就是以太网的mac地址
                LogUtils.d(TAG, "ethernet mac: " + ethMac);
            } else {
                LogUtils.d(TAG, "info is null!");
            }
        }
        return ethMac;
    }

    /**
     * Generates an AccessPoint key for a given scan result
     *
     * @param wifiManager
     * @param result      Scan result
     * @return AccessPoint key
     */
    public static String getKey(WifiManager wifiManager, ScanResult result) {
        return getKey(result.SSID, result.BSSID, getSecurity(wifiManager, result));
    }

    /**
     * Returns the AccessPoint key for a normal non-Passpoint network by ssid/bssid and security.
     */
    private static String getKey(String ssid, String bssid, int security) {
        StringBuilder builder = new StringBuilder();
        builder.append("AP:");
        if (TextUtils.isEmpty(ssid)) {
            builder.append(bssid);
        } else {
            builder.append(ssid);
        }
        builder.append(',').append(security);
        return builder.toString();
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    public static int getSecurity(WifiManager wifiManager, ScanResult result) {
        final boolean isWep = result.capabilities.contains("WEP");
        final boolean isSae = result.capabilities.contains("SAE");
        final boolean isPsk = result.capabilities.contains("PSK");
        final boolean isEapSuiteB192 = result.capabilities.contains("EAP_SUITE_B_192");
        final boolean isEap = result.capabilities.contains("EAP");
        final boolean isOwe = result.capabilities.contains("OWE");
        final boolean isOweTransition = result.capabilities.contains("OWE_TRANSITION");

        if (isSae && isPsk) {
            return wifiManager.isWpa3SaeSupported() ? SECURITY_SAE : SECURITY_PSK;
        }
        if (isOweTransition) {
            return wifiManager.isEnhancedOpenSupported() ? SECURITY_OWE : SECURITY_NONE;
        }

        if (isWep) {
            return SECURITY_WEP;
        } else if (isSae) {
            return SECURITY_SAE;
        } else if (isPsk) {
            return SECURITY_PSK;
        } else if (isEapSuiteB192) {
            return SECURITY_EAP_SUITE_B;
        } else if (isEap) {
            return SECURITY_EAP;
        } else if (isOwe) {
            return SECURITY_OWE;
        }
        return SECURITY_NONE;
    }

    public static int getSecurity(WifiConfiguration config) {
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.SAE)) {
            return SECURITY_SAE;
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
            return SECURITY_PSK;
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.SUITE_B_192)) {
            return SECURITY_EAP_SUITE_B;
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_EAP) ||
                config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.IEEE8021X)) {
            return SECURITY_EAP;
        }
        if (config.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.OWE)) {
            return SECURITY_OWE;
        }
        return (config.wepKeys[0] != null) ? SECURITY_WEP : SECURITY_NONE;
    }

    public static NetworkStatisticsInfo getWlanStatisticsInfo() {
        return getRadioStatisticsInfo("wlan0");
    }

    public static NetworkStatisticsInfo getEthStatisticsInfo() {
        return getRadioStatisticsInfo("eth0");
    }


    /**
     * line =Inter-|   Receive                                                |  Transmit
     * line = face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed(NetworkUtil.java:268)
     * line =    lo:  748441    1336    0    0    0     0          0         0   748441    1336    0    0    0     0       0          0(NetworkUtil.java:268)
     * line =ip_vti0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0(NetworkUtil.java:268)
     * line = wlan0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0(NetworkUtil.java:268)
     * line =ip6_vti0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0(NetworkUtil.java:268)
     * line =ip6tnl0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0(NetworkUtil.java:268)
     * line =  p2p0:       0       0    0    0    0     0          0         0        0       0    0    0    0     0       0          0(NetworkUtil.java:268)
     * line =  eth0: 38283109   41503    0    0    0     0          0         0  4748853   19808    0    0    0     0       0          0(NetworkUtil.java:268)
     */
    public static NetworkStatisticsInfo getRadioStatisticsInfo(String param) {
        NetworkStatisticsInfo statisticsInfo = new NetworkStatisticsInfo();
        String line;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader("/proc/net/dev"));
            while (null != (line = reader.readLine())) {
                String regex = ":";
                if (line.contains(regex)) {
                    String[] lineArr = line.trim().split(regex);
                    if (lineArr.length > 1 && lineArr[0].equalsIgnoreCase(param)) {
                        String content = lineArr[1];
                        String[] params = content.trim().split("\\s+");
                        LogUtils.d(TAG, "Obtained parameters: " + ArrayUtils.deepToString(params));

                        statisticsInfo.mName = param;
                        statisticsInfo.mReceiveBytes = params[0];
                        statisticsInfo.mReceivePacket = params[1];
                        statisticsInfo.mReceiveErrors = params[2];
                        statisticsInfo.mReceiveDropped = params[3];

                        statisticsInfo.mTransmitBytes = params[8];
                        statisticsInfo.mTransmitPacket = params[9];
                        statisticsInfo.mTransmitErrors = params[10];
                        statisticsInfo.mTransmitDropped = params[11];
                        return statisticsInfo;
                    }
                }
            }
        } catch (IOException e) {
            LogUtils.e(TAG, "getRadioStatisticsInfo call failed, " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (Exception e) {
                    LogUtils.e(TAG, "getRadioStatisticsInfo finally failed, " + e.getMessage());
                }
            }
        }

        return statisticsInfo;
    }

    /**
     * 获取dns
     *
     * @param context
     * @return
     */
    public static String getDns(Context context) {
        /**
         * 获取dns
         */
        String[] dnsServers = getDnsFromCommand();
        if (dnsServers.length == 0) {
            dnsServers = getDnsFromConnectionManager(context);
        }
        /**
         * 组装
         */
        StringBuilder sb = new StringBuilder();
        boolean gotServer = false;
        for (String dnsServer : dnsServers) {
            if (gotServer) {
                sb.append(" / ");
            }
            sb.append(dnsServer);
            gotServer = true;
        }

        return sb.toString();
    }

    private static String[] getDnsFromConnectionManager(Context context) {
        LinkedList<String> dnsServers = new LinkedList<>();
        if (context != null) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo != null) {
                    for (Network network : connectivityManager.getAllNetworks()) {
                        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                        if (networkInfo != null
                                && networkInfo.getType() == activeNetworkInfo.getType()) {
                            LinkProperties lp = connectivityManager.getLinkProperties(network);
                            for (InetAddress addr : lp.getDnsServers()) {
                                dnsServers.add(addr.getHostAddress());
                            }
                        }
                    }
                }
            }
        }
        return dnsServers.isEmpty()
                ? new String[0]
                : dnsServers.toArray(new String[dnsServers.size()]);
    }


    //通过 getprop 命令获取
    private static String[] getDnsFromCommand() {
        LinkedList<String> dnsServers = new LinkedList<>();
        try {
            Process process = Runtime.getRuntime().exec("getprop");
            InputStream inputStream = process.getInputStream();
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(inputStream));
            String line = null;
            while ((line = lnr.readLine()) != null) {
                int split = line.indexOf("]: [");
                if (split == -1) continue;
                String property = line.substring(1, split);
                String value = line.substring(split + 4, line.length() - 1);
                if (property.endsWith(".dns")
                        || property.endsWith(".dns1")
                        || property.endsWith(".dns2")
                        || property.endsWith(".dns3")
                        || property.endsWith(".dns4")) {
                    InetAddress ip = InetAddress.getByName(value);
                    if (ip == null) continue;
                    value = ip.getHostAddress();
                    if (value == null) continue;
                    if (value.length() == 0) continue;
                    dnsServers.add(value);
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getDnsFromCommand call failed, " + e.getMessage());
        }
        return dnsServers.isEmpty()
                ? new String[0]
                : dnsServers.toArray(new String[dnsServers.size()]);
    }

    /**
     * wifi的节点：/sys/class/net/wlan0/address
     * ethernet的节点：/sys/class/net/eth0/address
     * 有权限问题
     */
    public static String getWifiMacFromNode(String path) {
        String wifiMac = "";
        RandomAccessFile f = null;
        try {
            f = new RandomAccessFile(path, "r");
            f.seek(0);
            wifiMac = f.readLine().trim();
            f.close();
            LogUtils.d(TAG, "getWifiMacFromNode: " + wifiMac);
            return wifiMac;
        } catch (IOException e) {
            LogUtils.e(TAG, "getWifiMacFromNode call failed, " + e.getMessage());
            return wifiMac;
        } finally {
            if (f != null) {
                try {
                    f.close();
                    f = null;
                } catch (IOException e) {
                    LogUtils.e(TAG, "getWifiMacFromNode finally failed, " + e.getMessage());
                }
            }
        }
    }

    public static String getCurrentNetworkInterfaceName(Context context) {
        if (context != null) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo != null) {
                    for (Network network : connectivityManager.getAllNetworks()) {
                        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                        if (networkInfo != null
                                && networkInfo.getType() == activeNetworkInfo.getType()) {
                            LinkProperties lp = connectivityManager.getLinkProperties(network);
                            LogUtils.d(TAG, "getCurrentNetworkInterfaceName: " + lp.toString());
                            return lp.getInterfaceName();
                        }
                    }
                }
            }
        }
        return "";
    }

    ;

    /***************************************************************
     * Device.LAN.AddressingType
     *
     * This object contains parameters relating to IP-based LAN connectivity of a device.
     * This object relates only to IP-layer LAN capabilities.  Lower-layer aspects of LAN connectivity are not considered part of the common data model defined in this specification.
     * For a device that contains multiple IP interfaces, the scope of this object is limited to the default IP interface.  Data that might be associated with other interfaces is not considered part of the common data model defined in this specification.
     *
     * <enumeration value="DHCP"/>
     * <enumeration value="Static"/>
     *
     * readOnly
     ***************************************************************/
    public static String getAddressingType(Context context) {
        if (isEthernetConnected(context)) {
            return getEthIpAssignment(context);
        } else if (isWifiConnected(context)) {
            return getWifiIpAssignment(context);
        }
        return IpConfiguration.IpAssignment.UNASSIGNED.name();
    }

    ;

    public static WifiConfiguration getCurrentConfig() {
        WifiManager mWifiManager =
                ((WifiManager) GlobalContext.getContext().getSystemService(Context.WIFI_SERVICE));
        WifiInfo connectionInfo = mWifiManager.getConnectionInfo();  //得到连接的wifi网络
        WifiConfiguration wifiConfig = null;
        List<WifiConfiguration> configuredNetworks = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration conf : configuredNetworks) {
            if (conf.networkId == connectionInfo.getNetworkId()) {
                wifiConfig = conf;
                break;
            }
        }
        return wifiConfig;
    }

    //操作 盒子WiFi 开关的方法 false 表示关闭  true 表示打开
    public static boolean wifiSwitch(Context context, boolean open) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled() && !open) {
            wifiManager.setWifiEnabled(false);
        } else if (!wifiManager.isWifiEnabled() && open) {
            wifiManager.setWifiEnabled(true);
        }
        return true;
    }

    public static boolean canAccessInternet(Context context) {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        Network network = connectivityManager.getActiveNetwork();
        NetworkCapabilities networkCapabilities =
                connectivityManager.getNetworkCapabilities(network);

        if (networkCapabilities != null) {
            return !networkCapabilities.hasCapability(NET_CAPABILITY_CAPTIVE_PORTAL)
                    && !networkCapabilities.hasCapability(NET_CAPABILITY_PARTIAL_CONNECTIVITY)
                    && networkCapabilities.hasCapability(NET_CAPABILITY_VALIDATED);
        }

        return false;
    }

    //设置盒子上网路由方式: Static/DHCP
    public static void setStaticIP(Context context, String type, String ip) {
        EthernetManager mEthManager =
                (EthernetManager) context.getSystemService(Context.ETHERNET_SERVICE);
        WifiManager wifiManager =
                (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        switch (type) {
            case "Static":
                StaticIpConfiguration mStaticIpConfiguration = new StaticIpConfiguration();
                String[] split = getDns(GlobalContext.getContext()).split(" / ");
                for (int i = 0; i < split.length && !TextUtils.isEmpty(split[i]); i++) {
                    InetAddress inetAddress =
                            android.net.NetworkUtils.numericToInetAddress(split[i]);
                    mStaticIpConfiguration.dnsServers.add(inetAddress);
                }
                Inet4Address inetAddr = NetUtils.getInet4Address(ip);
                if (inetAddr == null) return;
                int prefixLength = NetUtils.maskStr2InetMask(
                        NetworkUtils.getLanMask(GlobalContext.getContext()));
                InetAddress gatewayAddr = NetUtils.getInet4Address(
                        NetworkUtils.getDefaultGateway(GlobalContext.getContext()));
                if (Arrays.toString(inetAddr.getAddress()).isEmpty() || prefixLength == 0) {
                    return;
                }

                mStaticIpConfiguration.ipAddress = new LinkAddress(ip + "/" + prefixLength);

                mStaticIpConfiguration.gateway = gatewayAddr;
                switch (NetworkUtils.getActiveNetworkType(GlobalContext.getContext())) {
                    case "Ethernet":
                        IpConfiguration mIpConfiguration = new IpConfiguration(
                                IpConfiguration.IpAssignment.STATIC,
                                IpConfiguration.ProxySettings.NONE,
                                mStaticIpConfiguration,
                                null);
                        mEthManager.setConfiguration("eth0", mIpConfiguration);
                        break;
                    case "WiFi":
                        IpConfiguration wifiIpConfiguration = new IpConfiguration(
                                IpConfiguration.IpAssignment.STATIC,
                                IpConfiguration.ProxySettings.NONE,
                                mStaticIpConfiguration,
                                null);
                        WifiConfiguration wifiConfiguration = getCurrentConfig();
                        wifiConfiguration.setIpConfiguration(wifiIpConfiguration);
                        wifiManager.setWifiApConfiguration(wifiConfiguration);
                        wifiManager.updateNetwork(wifiConfiguration);
                        new Thread() {
                            @Override
                            public void run() {
                                super.run();
                                int netId = wifiManager.addNetwork(wifiConfiguration);
                                wifiManager.disableNetwork(netId);
                                wifiManager.enableNetwork(netId, true);
                            }
                        }.start();
                        break;
                }
                break;
            case "DHCP":
                switch (NetworkUtils.getActiveNetworkType(GlobalContext.getContext())) {
                    case "Ethernet":
                        IpConfiguration ipConfiguration = new IpConfiguration(
                                IpConfiguration.IpAssignment.DHCP,
                                IpConfiguration.ProxySettings.NONE,
                                null,
                                null);
                        mEthManager.setConfiguration("eth0", ipConfiguration);
                        break;
                    case "WiFi":
                        IpConfiguration wifiIpConfiguration = new IpConfiguration(
                                IpConfiguration.IpAssignment.DHCP,
                                IpConfiguration.ProxySettings.NONE,
                                null,
                                null);
                        WifiConfiguration wifiConfiguration = getCurrentConfig();
                        wifiConfiguration.setIpConfiguration(wifiIpConfiguration);
                        wifiManager.setWifiApConfiguration(wifiConfiguration);
                        wifiManager.updateNetwork(wifiConfiguration);
                        new Thread() {
                            @Override
                            public void run() {
                                super.run();
                                int netId = wifiManager.addNetwork(wifiConfiguration);
                                wifiManager.disableNetwork(netId);
                                wifiManager.enableNetwork(netId, true);
                            }
                        }.start();
                        break;
                }
                break;
        }
    }

    /**
     * /data/misc/wifi/WifiConfigStore.xml
     *
     * @param context
     * @return
     */
    public static String getWifiIpAssignment(Context context) {
        ConnectivityManager connectivityManager =
                context.getSystemService(ConnectivityManager.class);
        final Network activeNetwork = connectivityManager.getActiveNetwork();
        NetworkInfo active = connectivityManager.getActiveNetworkInfo();

        if (activeNetwork != null && (active.getType() == ConnectivityManager.TYPE_WIFI)) {
            WifiManager wifiManager =
                    (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                LogUtils.e(TAG, "Failed to get WIFI_SERVICE, wifiManager is empty.");
                return IpConfiguration.IpAssignment.UNASSIGNED.name();
            }

            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            String ssid = wifiInfo.getSSID();
            LogUtils.d(TAG, "getWifiIpAssignment ssid: " + ssid);
            List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration wifiConfiguration : list) {
                if (!TextUtils.isEmpty(ssid) && ssid.equals(wifiConfiguration.SSID)) {
                    return wifiConfiguration.getIpAssignment().name();
                }
            }
        }
        LogUtils.d(TAG, "IpAssignment of WifiConfiguration is not assigned.");
        return IpConfiguration.IpAssignment.UNASSIGNED.name();
    }

    /**
     * /data/misc/ethernet/ipconfig.txt
     *
     * @param context
     * @return
     */
    public static String getEthIpAssignment(Context context) {
        ConnectivityManager connectivityManager =
                context.getSystemService(ConnectivityManager.class);
        final Network activeNetwork = connectivityManager.getActiveNetwork();
        NetworkInfo active = connectivityManager.getActiveNetworkInfo();

        if (activeNetwork != null && (active.getType() == ConnectivityManager.TYPE_ETHERNET)) {
            EthernetManager ethernetManager =
                    (EthernetManager) context.getSystemService(Context.ETHERNET_SERVICE);
            if (ethernetManager == null) {
                LogUtils.e(TAG, "Failed to get ETHERNET_SERVICE, ethernetManager is empty.");
                return IpConfiguration.IpAssignment.UNASSIGNED.name();
            }

            String[] interfaces = ethernetManager.getAvailableInterfaces();
            if (interfaces.length <= 0) {
                LogUtils.e(TAG, "Unable to get available interfaces.");
                return IpConfiguration.IpAssignment.UNASSIGNED.name();
            }
            LogUtils.d(TAG, "getEthIpAssignment ifaces: " + Arrays.toString(interfaces));

            LinkProperties defLinkProperties =
                    connectivityManager.getLinkProperties(activeNetwork);
            if (defLinkProperties == null) {
                LogUtils.e(TAG, "Failed to get LinkProperties.");
                return IpConfiguration.IpAssignment.UNASSIGNED.name();
            }
            LogUtils.d(TAG, "getEthIpAssignment defLinkProperties: " + defLinkProperties);

            IpConfiguration ipConfig =
                    ethernetManager.getConfiguration(defLinkProperties.getInterfaceName());
            if (ipConfig != null) {
                // getIpAssignment需要有setIpAssignment调用才会有变化，否则值始终为UNASSIGNED
                return (!ipConfig.getIpAssignment().equals(IpConfiguration.IpAssignment.UNASSIGNED))
                        ? ipConfig.getIpAssignment().name()
                        : IpConfiguration.IpAssignment.DHCP.name();
            }
        }
        LogUtils.d(TAG, "IpAssignment of EthernetManager is not assigned.");
        return IpConfiguration.IpAssignment.UNASSIGNED.name();
    }

    public static String getLinkDownstreamBandwidthKbps(Context context) {
        if (context != null) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo != null) {
                    for (Network network : connectivityManager.getAllNetworks()) {
                        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                        if (networkInfo != null
                                && networkInfo.getType() == activeNetworkInfo.getType()) {
                            NetworkCapabilities networkCapabilities =
                                    connectivityManager.getNetworkCapabilities(network);
                            LogUtils.d(TAG, "getLinkDownstreamBandwidthKbps: "
                                    + networkCapabilities.toString());
                            return String.valueOf(
                                    networkCapabilities.getLinkDownstreamBandwidthKbps());
                        }
                    }
                }
            }
        }
        return null;
    }

    public static String getLinkUpstreamBandwidthKbps(Context context) {
        if (context != null) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo != null) {
                    for (Network network : connectivityManager.getAllNetworks()) {
                        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                        if (networkInfo != null
                                && networkInfo.getType() == activeNetworkInfo.getType()) {
                            NetworkCapabilities networkCapabilities =
                                    connectivityManager.getNetworkCapabilities(network);
                            LogUtils.d(TAG, "getLinkUpstreamBandwidthKbps: "
                                    + networkCapabilities.toString());
                            return String.valueOf(
                                    networkCapabilities.getLinkUpstreamBandwidthKbps());
                        }
                    }
                }
            }
        }
        return null;
    }

    public static LinkProperties getLinkProperties(Context context, String ifaceName) {
        if (context != null) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                for (Network network : connectivityManager.getAllNetworks()) {
                    NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                    LogUtils.d(TAG, "networkInfo: " + networkInfo.toString());
                    LinkProperties lp = connectivityManager.getLinkProperties(network);
                    if (lp.getInterfaceName().equals(ifaceName)) {
                        return lp;
                    }
                }
            }
        }
        return null;
    }

    public static String getIPvCapable(Context context, boolean isIPv4) {
        if (context != null) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                LogUtils.d(TAG, "activeNetworkInfo: " + activeNetworkInfo.toString());
                for (Network network : connectivityManager.getAllNetworks()) {
                    NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                    LogUtils.d(TAG, "networkInfo: " + networkInfo.toString());
                    if (networkInfo.getType() == activeNetworkInfo.getType()) {
                        LinkProperties lp = connectivityManager.getLinkProperties(network);
                        LogUtils.d(TAG, "LinkProperties: " + lp.toString());
                        List<LinkAddress> linkAddressList = lp.getLinkAddresses();
                        for (LinkAddress linkAddress : linkAddressList) {
                            if (isIPv4 && (linkAddress.isIpv4())) {
                                return String.valueOf(linkAddress.isIpv4());
                            } else if (!isIPv4 && linkAddress.isIpv6()) {
                                return String.valueOf(linkAddress.isIpv6());
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static String getIPvEnable(Context context, boolean isIPv4) {
        if (context != null) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                LogUtils.d(TAG, "activeNetworkInfo: " + activeNetworkInfo.toString());
                for (Network network : connectivityManager.getAllNetworks()) {
                    NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                    LogUtils.d(TAG, "networkInfo: " + networkInfo.toString());
                    if (networkInfo.getType() == activeNetworkInfo.getType()) {
                        LinkProperties lp = connectivityManager.getLinkProperties(network);
                        List<LinkAddress> linkAddressList = lp.getLinkAddresses();
                        for (LinkAddress linkAddress : linkAddressList) {
                            InetAddress inetAddress = linkAddress.getAddress();
                            if (isIPv4 && linkAddress.isIpv4()
                                    && (!inetAddress.isLoopbackAddress())) {
                                return String.valueOf(linkAddress.isIpv4());
                            } else if ((!isIPv4) && linkAddress.isIPv6()
                                    && (!inetAddress.isLoopbackAddress())) {
                                return String.valueOf(linkAddress.isIpv6());
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    public static String getLanMask(Context context) {
        if (context != null) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo != null) {
                    for (Network network : connectivityManager.getAllNetworks()) {
                        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                        if (networkInfo != null
                                && networkInfo.getType() == activeNetworkInfo.getType()) {
                            LinkProperties lp = connectivityManager.getLinkProperties(network);
                            List<LinkAddress> linkAddressList = lp.getLinkAddresses();
                            for (LinkAddress linkAddress : linkAddressList) {
                                InetAddress inetAddress = linkAddress.getAddress();
                                if (linkAddress.isIpv4() && (!inetAddress.isLoopbackAddress())) {
                                    String maskAddress =
                                            calcMaskByPrefixLength(
                                                    linkAddress.getNetworkPrefixLength());
                                    LogUtils.d(TAG, "getLanMask: " + maskAddress);
                                    return maskAddress;
                                }
                            }
                        }
                    }
                }
            }
        }
        return "0.0.0.0";
    }

    public static String getGateway(Context context) {
        if (context != null) {
            ConnectivityManager connectivityManager =
                    (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
                if (activeNetworkInfo != null) {
                    for (Network network : connectivityManager.getAllNetworks()) {
                        NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                        if (networkInfo != null
                                && networkInfo.getType() == activeNetworkInfo.getType()) {
                            LinkProperties lp = connectivityManager.getLinkProperties(network);
                            List<LinkAddress> linkAddressList = lp.getLinkAddresses();
                            for (LinkAddress linkAddress : linkAddressList) {
                                InetAddress inetAddress = linkAddress.getAddress();
                                if (linkAddress.isIpv4() && (!inetAddress.isLoopbackAddress())) {
                                    String maskAddress =
                                            calcMaskByPrefixLength(
                                                    linkAddress.getNetworkPrefixLength());
                                    String gateway =
                                            calcSubnetAddress(
                                                    inetAddress.getHostAddress(), maskAddress);
                                    LogUtils.d(TAG, "getGateway: " + maskAddress);
                                    return gateway;
                                }
                            }
                        }
                    }
                }
            }
        }
        return "0.0.0.0";
    }

    /***************************************************************
     * Device.LAN.DefaultGateway
     *
     * readOnly
     ***************************************************************/
    public static String getDefaultGateway(Context context) {
        ConnectivityManager mConnectivityManager =
                context.getSystemService(ConnectivityManager.class);
        final Network defaultNetwork = mConnectivityManager.getActiveNetwork();
        LinkProperties defLinkProperties = null;
        if (defaultNetwork != null) {
            defLinkProperties = mConnectivityManager.getLinkProperties(defaultNetwork);
        }

        final StringBuilder sb = new StringBuilder();
        boolean gotGateway = false;
        if (defLinkProperties != null) {
            for (RouteInfo route : defLinkProperties.getRoutes()) {
                if (route.isDefaultRoute()) {
                    if (gotGateway) {
                        sb.append(" / ");
                    }
                    String gateway = route.getGateway().getHostAddress();
                    sb.append(gateway);
                    LogUtils.d(TAG, "Get default gateway: " + gateway);
                    gotGateway = true;
                }
            }
        }
        return (gotGateway) ? sb.toString() : "0.0.0.0";
    }

    /***************************************************************
     * Device.GatewayInfo.X_00604C_GATEWAY_MAC
     *
     * readOnly
     ***************************************************************/
    public static String getGatewayMac(Context context) {
        String gateway = null;
        String gatewayMac = null;
        ConnectivityManager mConnectivityManager =
                context.getSystemService(ConnectivityManager.class);
        final Network defaultNetwork = mConnectivityManager.getActiveNetwork();
        LinkProperties defLinkProperties = null;
        if (defaultNetwork != null) {
            defLinkProperties = mConnectivityManager.getLinkProperties(defaultNetwork);
        }

        if (null != defLinkProperties) {
            for (RouteInfo route : defLinkProperties.getRoutes()) {
                if (route.isDefaultRoute()) {
                    gateway = route.getGateway().getHostAddress();
                    break;
                }
            }
        }
        if (null != gateway) {
            gatewayMac = getArpByTable(gateway);
            LogUtils.i(TAG, "getGatewayMac: " + gatewayMac);
            return gatewayMac;
        }

        return "00:00:00:00:00:00";
    }

    /**
     * 获取路由器MAC要用到
     *
     * @param dtsip
     * @return
     */
    private static String getArpByTable(String dtsip) {
        BufferedReader reader = null;
        String mac = "";
        try {
            reader = new BufferedReader(new FileReader("/proc/net/arp"));
            String line = "";
            String ip = "";
            while ((line = reader.readLine()) != null) {
                try {
                    line = line.trim();
                    if (line.length() < 63) continue;
                    if (line.contains("IP")) continue;
                    ip = line.substring(0, 17).trim();
                    if (ip.equals(dtsip)) {
                        mac = line.substring(41, 63).trim();
                        break;
                    }
                } catch (Exception e) {
                    LogUtils.e(TAG, "getArpByTable read failed, " + e.getMessage());
                }
            }

        } catch (Exception e) {
            LogUtils.e(TAG, "getArpByTable call failed, " + e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    LogUtils.e(TAG, "getArpByTable finally failed, " + e.getMessage());
                }
            }
        }
        LogUtils.i(TAG, "getArpByTable ip: " + dtsip + " mac: " + mac);
        return mac;
    }

    /***************************************************************
     * Device.Ethernet.Interface.{i}.Status
     * Enables or disables the interface.
     * This parameter is based on ''ifAdminStatus'' from {{bibref|RFC2863}}.
     *
     * <enumeration value="Up"/>
     * <enumeration value="Down"/>
     * <enumeration value="Unknown"/>
     * <enumeration value="Dormant"/>
     * <enumeration value="NotPresent"/>
     * <enumeration value="LowerLayerDown"/>
     * <enumeration value="Error" optional="true"/>
     *
     * protocol:TR-181i2|Section 4.2.2
     * readOnly
     ***************************************************************/
    public static String getEthernetInterfaceStatus(Context context) {
        return getNetworkInterfaceStatus(context, "eth0");
    }

    /***************************************************************
     * Device.WiFi.Radio.{i}.Status
     *
     * When {{param|Enable}} is {{false}} then {{param}} SHOULD normally be {{enum|Down}} (or {{enum|NotPresent}} or {{enum|Error}} if there is a fault condition on the interface).
     * When {{param|Enable}} is changed to {{true}} then {{param}} SHOULD change to {{enum|Up}} if and only if the interface is able to transmit and receive network traffic;
     * it SHOULD change to {{enum|Dormant}} if and only if the interface is operable but is waiting for external actions before it can transmit and receive network traffic (and subsequently change to {{enum|Up}} if still operable when the expected actions have completed);
     * it SHOULD change to {{enum|LowerLayerDown}} if and only if the interface is prevented from entering the {{enum|Up}} state because one or more of the interfaces beneath it is down
     * it SHOULD remain in the {{enum|Error}} state if there is an error or other fault condition detected on the interface;
     * it SHOULD remain in the {{enum|NotPresent}} state if the interface has missing (typically hardware) components;
     * it SHOULD change to {{enum|Unknown}} if the state of the interface can not be determined for some reason.
     *
     * <enumeration value="Up"/>
     * <enumeration value="Down"/>
     * <enumeration value="Unknown"/>
     * <enumeration value="Dormant"/>
     * <enumeration value="NotPresent"/>
     * <enumeration value="LowerLayerDown"/>
     * <enumeration value="Error" optional="true"/>
     *
     * TR-181i2|Section 4.2.2
     * readOnly
     ***************************************************************/
    public static String getWiFiRadioStatus(Context context) {
        return getNetworkInterfaceStatus(context, "wlan0");
    }

    public static int getNetworkInterfaceNumberOfEntries(Context context) {
//        try {
//            List<NetworkInterface> nifs = Collections.list(NetworkInterface.getNetworkInterfaces());
//            if (nifs != null) {
//                return nifs.size();
//            }
//        } catch (SocketException e) {
//            LogUtils.e(TAG, "getNetworkInterfaceNumberOfEntries call failed, " + e.getMessage());
//        }
        return 2;
    }

    public static int getIPv4AddressNumberOfEntries(Context context, String ifaceName) {
        try {
            int count = 0;
            Enumeration<NetworkInterface> nifs = NetworkInterface.getNetworkInterfaces();
            if (nifs.hasMoreElements()) {
                NetworkInterface networkCard = nifs.nextElement();
                List<InterfaceAddress> addressList = networkCard.getInterfaceAddresses();
                for (InterfaceAddress infAddress : addressList) {
                    InetAddress inetAddress = infAddress.getAddress();
                    if (inetAddress instanceof Inet4Address) {
                        count++;
                    }
                }
                return count;
            }
        } catch (SocketException e) {
            LogUtils.e(TAG, "getIPv4AddressNumberOfEntries call failed, " + e.getMessage());
        }
        return 0;
    }

    public static String getNetworkInterfaceStatus(Context context, String ifaceName) {
        try {
            NetworkInterface networkInterface = NetworkInterface.getByName(ifaceName);
            if (networkInterface == null) return null;
            return networkInterface.isUp() ? "Up" : "Down";
        } catch (SocketException e) {
            LogUtils.e(TAG, "getNetworkInterfaceStatus call failed, " + e.getMessage());
            return "Error";
        }
    }


    public static NetworkInterface getNetworkInterface(Context context, String ifaceName) {
        try {
            return NetworkInterface.getByName(ifaceName);
        } catch (SocketException e) {
            LogUtils.e(TAG, "getNetworkInterface call failed, " + e.getMessage());
        }
        return null;
    }

    public static boolean isSupport5GHz(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager != null && wifiManager.is5GHzBandSupported();
    }

    public static String calcMaskByPrefixLength(int length) {
        int mask = 0xffffffff << (32 - length);
        int partsNum = 4;
        int bitsOfPart = 8;
        int[] maskParts = new int[partsNum];
        int selector = 0x000000ff;

        for (int i = 0; i < maskParts.length; i++) {
            int pos = maskParts.length - 1 - i;
            maskParts[pos] = (mask >> (i * bitsOfPart)) & selector;
        }

        String result = "";
        result = result + maskParts[0];
        for (int i = 1; i < maskParts.length; i++) {
            result = result + "." + maskParts[i];
        }
        return result;
    }

    public static String calcSubnetAddress(String ip, String mask) {
        StringBuilder result = new StringBuilder();
        try {
            // calc sub-net IP
            InetAddress ipAddress = InetAddress.getByName(ip);
            InetAddress maskAddress = InetAddress.getByName(mask);

            byte[] ipRaw = ipAddress.getAddress();
            byte[] maskRaw = maskAddress.getAddress();

            int unsignedByteFilter = 0x000000ff;
            int[] resultRaw = new int[ipRaw.length];
            for (int i = 0; i < resultRaw.length; i++) {
                resultRaw[i] = (ipRaw[i] & maskRaw[i] & unsignedByteFilter);
            }

            // make result string
            result.append(resultRaw[0]);
            for (int i = 1; i < resultRaw.length; i++) {
                result.append(".").append(resultRaw[i]);
            }
        } catch (UnknownHostException e) {
            LogUtils.e(TAG, "calcSubnetAddress call failed, " + e.getMessage());
        }

        return result.toString();
    }

    /***************************** add by pengdeping ***********************************/

    public static String getMacAddressFromIp(Context context) {
        String mac_s = "";
        StringBuilder buf = new StringBuilder();
        try {
            byte[] mac;
            InetAddress inetAddress = InetAddress.getByName(getIpv4Address(context));
            NetworkInterface ne = NetworkInterface.getByInetAddress(inetAddress);
            mac = ne.getHardwareAddress();
            for (byte b : mac) {
                buf.append(String.format("%02X:", b));
            }
            if (buf.length() > 0) {
                buf.deleteCharAt(buf.length() - 1);
            }
            mac_s = buf.toString();
        } catch (Exception e) {
            LogUtils.e(TAG, "getMacAddressFromIp call failed, " + e.getMessage());
        }

        return mac_s;
    }

    static InetAddress intToInetAddress(int hostAddress) {
        byte[] addressBytes = {
                (byte) (hostAddress & 0xff),
                (byte) (hostAddress >> 8 & 0xff),
                (byte) (hostAddress >> 16 & 0xff),
                (byte) (hostAddress >> 24 & 0xff)
        };

        try {
            return InetAddress.getByAddress(addressBytes);
        } catch (UnknownHostException e) {
            LogUtils.e(TAG, "intToInetAddress call failed, " + e.getMessage());
            throw new AssertionError();
        }
    }

    /**
     * 这个接口只有连接WiFi时才能获取到数据
     *
     * @param context
     * @return
     */
    public static String getServerAddress(Context context) {
        if (context != null) {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();
                if (dhcpInfo != null) {
                    LogUtils.d(TAG, "dhcpInfo.toString(): " + dhcpInfo.toString());
                    return intToInetAddress(dhcpInfo.serverAddress).getHostAddress();
                }
            }
        }
        return "0.0.0.1";
    }

    public static String getRouteIPAddress(Context context) {
        String gateway = "0.0.0.0";
        ConnectivityManager mConnectivityManager =
                context.getSystemService(ConnectivityManager.class);
        final Network defaultNetwork = mConnectivityManager.getActiveNetwork();
        LinkProperties defLinkProperties = null;
        if (defaultNetwork != null) {
            defLinkProperties = mConnectivityManager.getLinkProperties(defaultNetwork);
        }

        if (null != defLinkProperties) {
            for (RouteInfo route : defLinkProperties.getRoutes()) {
                if (route.isDefaultRoute()) {
                    gateway = route.getGateway().getHostAddress();
                    break;
                }
            }
        }

        return gateway;
    }

    public static int MaxMTUSize(Context context) {
        ConnectivityManager mConnectivityManager =
                context.getSystemService(ConnectivityManager.class);
        final Network defaultNetwork = mConnectivityManager.getActiveNetwork();
        LinkProperties defLinkProperties = null;
        if (defaultNetwork != null) {
            defLinkProperties = mConnectivityManager.getLinkProperties(defaultNetwork);
            return defLinkProperties.getMtu();
        }
        return 0;
    }
}
