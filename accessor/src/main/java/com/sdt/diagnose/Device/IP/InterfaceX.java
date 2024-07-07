package com.sdt.diagnose.Device.IP;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.IpConfiguration;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.net.wifi.WifiManager;

import com.sdt.annotations.Tr369Get;
import com.sdt.annotations.Tr369Set;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.NetworkUtils;
import com.sdt.diagnose.common.log.LogUtils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.List;

/**
 * 上报wln0，eth0接口的信息
 */
public class InterfaceX {
    private static final String TAG = "InterfaceX";
    private final static String REFIX = "Device.IP.Interface.";
    private final static String REGEX = "\\.";
    private final String[] interfaces = {"wlan0", "eth0"};
    private final static int INDEX_TYPE_WIFI = 0;
    private final static int INDEX_TYPE_ETH = 1;

    @Tr369Set("Device.IP.Interface.1.Enable")
    public boolean SK_TR369_SetWifiEnable(String path, String value) {
        WifiManager mWifiManager =
                (WifiManager) GlobalContext.getContext().getSystemService(Context.WIFI_SERVICE);
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

    @Tr369Get("Device.IP.Interface.")
    public String SK_TR369_GetInterfaceX(String path) {
        return handleInterfaceGet(path);
    }

    private String handleInterfaceGet(String path) {
        String result = null;
        String[] paramsArr = null;
        if (path.startsWith(REFIX)) {
            paramsArr = path.replace(REFIX, "").split(REGEX);
            int index = Integer.parseInt(paramsArr[0]) - 1;

            if (paramsArr.length > 3 && "IPv4Address".equals(paramsArr[1])) {
                if (paramsArr.length == 4) {
                    switch (paramsArr[3]) {
                        case "Enable":
                            return getIPv4AddressEnable(index);
                        case "Status":
                            return getIPv4Status(index);
                        case "AddressingType":
                            return getAddressingType(index);
                        case "IPAddress":
                            return getIPv4Address(index);
                        case "SubnetMask":
                            return getIPv4SubnetMask(index);
                        default:
                            break;
                    }
                }
            } else if (paramsArr.length == 2) {
                switch (paramsArr[1]) {
                    case "Status":
                        return getStatus(index);
                    case "Enable":
                        return getEnable(index);
                    case "Name":
                        return getName(index);
                    case "Type":
                        return getType(index);
                    case "MaxMTUSize":
                        return getMaxMTUSize(index);
                    case "IPv4Enable":
                        return getIPv4Enable(index);
                    case "IPv6Enable":
                        return getIPv6Enable(index);
                    case "IPv4AddressNumberOfEntries":
                        return getIPv4AddressNumberOfEntries(index);
                    default:
                        break;
                }
            }
        }
        return result;
    }

    ;

    public String getEnable(int index) {
        if (index == INDEX_TYPE_WIFI) {
            return String.valueOf(NetworkUtils.isWifiConnected(GlobalContext.getContext()));
        } else if (index == INDEX_TYPE_ETH) {
            return String.valueOf(NetworkUtils.isEthernetConnected(GlobalContext.getContext()));
        }
        return String.valueOf(false);
    }

    /**
     * Disabled
     * Enabled
     * Error_Misconfigured
     * Error (OPTIONAL)
     */
    public String getStatus(int index) {
        return getIPv4Status(index);
    }

    public String getName(int index) {
        return interfaces[index];
    }

    /**
     * Normal
     * Loopback
     * Tunnel (Only used with legacy (Tunnel,Tunneled) IP interface pairs)
     * Tunneled (Only used with legacy (Tunnel,Tunneled) IP interface pairs)
     */
    public String getType(int index) {
        try {
            NetworkInterface nif = NetworkUtils.getNetworkInterface(GlobalContext.getContext(), interfaces[index]);
            if (nif != null && nif.isLoopback()) {
                return "Loopback";
            }
        } catch (SocketException e) {
            LogUtils.e(TAG, "getType error, " + e.getMessage());
        }
        return "Normal";
    }

    public String getMaxMTUSize(int index) {
        try {
            NetworkInterface nif = NetworkUtils.getNetworkInterface(GlobalContext.getContext(), interfaces[index]);
            if (nif != null) {
                int mtu = nif.getMTU();
                return String.valueOf(mtu);
            }
        } catch (SocketException e) {
            LogUtils.e(TAG, "getMaxMTUSize error, " + e.getMessage());
        }
        return String.valueOf(-1);
    }

    /**
     * 盒子只有一个IPv4, 路由器设备才有多个对象
     *
     * @param index
     * @return
     */
    public String getIPv4AddressNumberOfEntries(int index) {
        return String.valueOf(1);
    }

    public String getIPv4Enable(int index) {
        LinkProperties lp = getLinkProperties(GlobalContext.getContext(), interfaces[index]);
        if (lp == null) return Boolean.toString(false);
        List<LinkAddress> linkAddressList = lp.getLinkAddresses();
        for (LinkAddress linkAddress : linkAddressList) {
            InetAddress inetAddress = linkAddress.getAddress();
            if (linkAddress.isIpv4() && (!inetAddress.isLoopbackAddress())) {
                return String.valueOf(linkAddress.isIpv4());
            }
        }
        return Boolean.toString(false);
    }

    public String getIPv6Enable(int index) {
        LinkProperties lp = getLinkProperties(GlobalContext.getContext(), interfaces[index]);
        if (lp == null) return null;
        List<LinkAddress> linkAddressList = lp.getLinkAddresses();
        for (LinkAddress linkAddress : linkAddressList) {
            InetAddress inetAddress = linkAddress.getAddress();
            if (linkAddress.isIpv6() && (!inetAddress.isLoopbackAddress())) {
                return String.valueOf(linkAddress.isIpv6());
            }
        }
        return Boolean.toString(false);
    }

    public String getIPv4AddressEnable(int index) {
        boolean result = "Enabled".equals(getIPv4Status(index));
        return result ? getIPv4Enable(index) : Boolean.toString(false);
    }

    /**
     * Disabled
     * Enabled
     * Error_Misconfigured
     * Error (OPTIONAL)
     *
     * @return
     */
    public String getIPv4Status(int index) {
        NetworkInterface nif = NetworkUtils.getNetworkInterface(GlobalContext.getContext(), interfaces[index]);
        if (nif != null) {
            try {
                return nif.isUp() ? "Enabled" : "Disabled";
            } catch (SocketException e) {
                LogUtils.e(TAG, "getIPv4Status error, " + e.getMessage());
            }
        }
        return "Error";
    }

    public String getAddressingType(int index) {
        if (index == INDEX_TYPE_WIFI) {
            return NetworkUtils.getWifiIpAssignment(GlobalContext.getContext());
        } else if (index == INDEX_TYPE_ETH) {
            return NetworkUtils.getEthIpAssignment(GlobalContext.getContext());
        }
        return IpConfiguration.IpAssignment.UNASSIGNED.name();
    }

    public String getIPv4Address(int index) {
        if (NetworkUtils.isWifiConnected(GlobalContext.getContext()) && index == INDEX_TYPE_WIFI) {
            return NetworkUtils.getIpv4Address(GlobalContext.getContext());
        } else if (NetworkUtils.isEthernetConnected(GlobalContext.getContext()) && index == INDEX_TYPE_ETH) {
            return NetworkUtils.getIpv4Address(GlobalContext.getContext());
        }
        return "0.0.0.0";
    }

    public String getIPv4SubnetMask(int index) {
        if (NetworkUtils.isWifiConnected(GlobalContext.getContext()) && index == INDEX_TYPE_WIFI) {
            return NetworkUtils.getLanMask(GlobalContext.getContext());
        } else if (NetworkUtils.isEthernetConnected(GlobalContext.getContext()) && index == INDEX_TYPE_ETH) {
            return NetworkUtils.getLanMask(GlobalContext.getContext());
        }
        return "0.0.0.0";
    }

    private LinkProperties getLinkProperties(Context context, String ifaceName) {
        if (context != null) {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                for (Network network : connectivityManager.getAllNetworks()) {
                    LinkProperties lp = connectivityManager.getLinkProperties(network);
                    if (lp.getInterfaceName().equals(ifaceName)) {
                        return lp;
                    }
                }
            }
        }
        return null;
    }

}
