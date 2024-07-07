package com.sdt.diagnose.Device;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

import androidx.annotation.NonNull;

import com.sdt.annotations.Tr369Get;
import com.sdt.annotations.Tr369Set;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.NetworkUtils;
import com.sdt.diagnose.common.log.LogUtils;

/**
 * Device.LAN. tr181已废弃
 */
public class LanX {
    private static final String TAG = "LanX";
    private final Handler mHandler;
    private final HandlerThread mThread;
    private final int MSG_CHECK_INTERNET_CONNECTION = 3306;

    public static class Lan {
        public static String AddressType;
        public static String ip;
    }

    public LanX() {
        mThread = new HandlerThread("LanXThread", Process.THREAD_PRIORITY_BACKGROUND);
        mThread.start();
        mHandler =
                new Handler(mThread.getLooper()) {
                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        if (msg.what == MSG_CHECK_INTERNET_CONNECTION) {
                            mHandler.removeMessages(MSG_CHECK_INTERNET_CONNECTION);
                            if (!NetworkUtils.canAccessInternet(GlobalContext.getContext())) {
                                LogUtils.e(TAG, "Unable to access the internet, switching to dynamic IP soon.");
                                NetworkUtils.setStaticIP(GlobalContext.getContext(), "DHCP", null);
                            }
                        }
                    }
                };
    }

    @Tr369Get("Device.LAN.AddressingType")
    public String SK_TR369_GetLanAddressingType() {
        return NetworkUtils.getAddressingType(GlobalContext.getContext());
    }

    @Tr369Set("Device.LAN.AddressingType")
    public boolean SK_TR369_SetLanAddressingType(String path, String value) {
        Lan.AddressType = value;
        if (value.equals("DHCP")) {
            NetworkUtils.setStaticIP(GlobalContext.getContext(), Lan.AddressType, null);
        }
        return true;
    }

    @Tr369Get("Device.LAN.IPAddress")
    public String SK_TR369_GetLanIPAddress() {
        return NetworkUtils.getIpv4Address(GlobalContext.getContext());
    }

    @Tr369Get("Device.LAN.IPv6Address")
    public String SK_TR369_GetLanIPv6Address() {
        return NetworkUtils.getIpv6Address(GlobalContext.getContext());
    }

    @Tr369Set("Device.LAN.IPAddress")
    public boolean SK_TR369_SetLanIPAddress(String path, String value) {
        Lan.ip = value;
        NetworkUtils.setStaticIP(GlobalContext.getContext(), Lan.AddressType, Lan.ip);
        if (Lan.AddressType.equals("Static")) {
            if (!mHandler.hasMessages(MSG_CHECK_INTERNET_CONNECTION)) {
                LogUtils.i(TAG, "Switched to static IP, about to verify if this IP can access the internet.");
                mHandler.sendEmptyMessageDelayed(MSG_CHECK_INTERNET_CONNECTION, 10000);
            }
        }
        return true;
    }

    @Tr369Get("Device.LAN.SubnetMask")
    public String SK_TR369_GetLanSubnetMask() {
        return NetworkUtils.getLanMask(GlobalContext.getContext());
    }

    @Tr369Get("Device.LAN.DefaultGateway")
    public String SK_TR369_GetDefaultGateway() {
        return NetworkUtils.getDefaultGateway(GlobalContext.getContext());
    }

    @Tr369Get("Device.LAN.DNSServers")
    public String SK_TR369_GetDNSServers() {
        return NetworkUtils.getDns(GlobalContext.getContext());
    }

    @Tr369Get("Device.LAN.MACAddress")
    public String SK_TR369_GetMACAddress() {
        if (NetworkUtils.isEthernetConnected(GlobalContext.getContext())) {
            return NetworkUtils.getEthernetMacAddress();
        } else if (NetworkUtils.isWifiConnected(GlobalContext.getContext())) {
            return NetworkUtils.getWifiMacAddress();
        }
        return "00:00:00:00:00:00";
    }

}
