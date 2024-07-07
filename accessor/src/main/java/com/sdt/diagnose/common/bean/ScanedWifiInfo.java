package com.sdt.diagnose.common.bean;

import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import static android.net.ConnectivityManager.TYPE_WIFI;
import static com.sdt.diagnose.common.NetworkUtils.SECURITY_NONE;
import static com.sdt.diagnose.common.NetworkUtils.SECURITY_OWE;
import static com.sdt.diagnose.common.NetworkUtils.SECURITY_PSK;
import static com.sdt.diagnose.common.NetworkUtils.SECURITY_SAE;

import com.sdt.diagnose.common.NetworkUtils;
import com.sdt.diagnose.common.log.LogUtils;

public class ScanedWifiInfo implements Comparable<ScanedWifiInfo> {
    private static final String TAG = "ScanedWifiInfo";
    public String radio;
    public String ssid;
    public String bssid;
    public String mode = "AdHoc";
    public int channel;
    public int signalStrength;
    public String securityModeEnabled;
    public String encryptionMode;
    public String operatingFrequencyBand;
    public String supportedStandards;
    public String operatingStandards;
    public String operatingChannelBandwidth;
    public int noise;
    public String basicDataTransferRates;
    public String supportedDataTransferRates;
    public int DTIMPeriod;

    private String mKey;
    private int mSecurity;
    private int mNetworkId = WifiConfiguration.INVALID_NETWORK_ID;
    private int mRssi = Integer.MIN_VALUE;
    private boolean mIsPskSaeTransitionMode = false;
    private boolean mIsOweTransitionMode = false;
    public WifiConfiguration mConfig;
    public WifiInfo mInfo;
    public NetworkInfo mNetworkInfo;

    public ScanedWifiInfo(WifiManager wm, ScanResult result) {
        radio = "802.11 ";
        supportedStandards = "NA";
        operatingStandards = "NA";
        operatingFrequencyBand = "2.4GHz";
        ssid = null;
        bssid = null;
        securityModeEnabled = "NA";
        encryptionMode = "NA";
        operatingChannelBandwidth = "NA";
        noise = 0;
        basicDataTransferRates = "NA";
        supportedDataTransferRates = "NA";
        transform(wm, result);
    }

    public String getKey() {
        return mKey;
    }

    public void setKey(String key) {
        mKey = key;
    }

    public int getSecurity() {
        return mSecurity;
    }

    public int getNetworkId() {
        return mNetworkId;
    }

    public int getRssi() {
        return mRssi;
    }

    private void transform(WifiManager wm, ScanResult result) {
        bssid = result.BSSID;
        ssid = result.SSID;
        signalStrength = result.level;
        mSecurity = NetworkUtils.getSecurity(wm, result);
        operatingChannelBandwidth = Integer.toString(result.channelWidth);
        channel = Integer.parseInt(getChannelByFrequency(result.frequency));
        mIsPskSaeTransitionMode = result.capabilities.contains("PSK") && result.capabilities.contains("SAE");
        mIsOweTransitionMode = result.capabilities.contains("OWE_TRANSITION");
        boolean A = false;
        boolean G = false;
        boolean N = false;
        boolean AC = false;
        boolean AES = false;
        boolean TKIP = false;
        if (/*is5GHzWifi(result.frequency)*/result.is5GHz()) {
            A = true;
            operatingFrequencyBand = "5GHz";
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

        String tmpRadio = "";

        if (A) {
            tmpRadio += "a";
            if (N) {
                tmpRadio += "/n";
            }
            if (AC) {
                tmpRadio += "/ac";
            }
        } else {
            tmpRadio += "b";
            if (G) {
                tmpRadio += "/g";
            }
            if (N) {
                tmpRadio += "/n";
            }
        }
        radio += tmpRadio;
        supportedStandards = tmpRadio;

        if (result.capabilities.contains("WPA2-PSK-WPA3-SAE")) {
            securityModeEnabled = "WPA2-PSK-WPA3-SAE";
        } else if (result.capabilities.contains("WPA-WPA2-Enterprise")) {
            securityModeEnabled = "WPA-WPA2-Enterprise";
        } else if (result.capabilities.contains("WPA3-Enterprise")) {
            securityModeEnabled = "WPA3-Enterprise";
        } else if (result.capabilities.contains("WPA2-Enterprise")) {
            securityModeEnabled = "WPA2-Enterprise";
        } else if (result.capabilities.contains("WPA-Enterprise")) {
            securityModeEnabled = "WPA-Enterprise";
        } else if (result.capabilities.contains("WPA-WPA2")) {
            securityModeEnabled = "WPA-WPA2";
        } else if (result.capabilities.contains("WPA3-SAE")) {
            securityModeEnabled = "WPA3-SAE";
        } else if (result.capabilities.contains("WPA2")) {
            securityModeEnabled = "WPA2";
        } else if (result.capabilities.contains("WPA")) {
            securityModeEnabled = "WPA";
        } else if (result.capabilities.contains("WEP")) {
            securityModeEnabled = "WEP";
        } else {
            securityModeEnabled = "None";
        }

        if (result.capabilities.contains("TKIP")) {
            TKIP = true;
        }
        if (result.capabilities.contains("CCMP")) {
            AES = true;
        }

        if (TKIP && AES) {
            encryptionMode = "TKIP/AES";
        } else if (TKIP) {
            encryptionMode = "TKIP";
        } else if (AES) {
            encryptionMode = "AES";
        }
    }

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

    public void update(WifiConfiguration config) {
        mConfig = config;
        if (mConfig != null && !isPasspoint()) {
            ssid = removeDoubleQuotes(mConfig.SSID);
        }
        mNetworkId = config != null ? config.networkId : WifiConfiguration.INVALID_NETWORK_ID;
    }

    public void update(WifiInfo info, NetworkInfo net) {
        mInfo = info;
        if (net != null && net.getType() == TYPE_WIFI) {
            mNetworkInfo = net;
        }
        if (info != null) {
            noise = info.getRssi();
            basicDataTransferRates = String.valueOf(info.getLinkSpeed());
        }
    }

    /**
     * Return true if this AccessPoint represents a Passpoint AP.
     */
    private boolean isPasspoint() {
        return mConfig != null && mConfig.isPasspoint();
    }

    /**
     * Return whether this is the active connection.
     * For ephemeral connections (networkId is invalid), this returns false if the network is
     * disconnected.
     */
    public boolean isActive() {
        return mNetworkInfo != null &&
                (mNetworkId != WifiConfiguration.INVALID_NETWORK_ID ||
                        mNetworkInfo.getState() != NetworkInfo.State.DISCONNECTED);
    }

    /**
     * Return true if the current RSSI is reachable, and false otherwise.
     */
    public boolean isReachable() {
        return mRssi != Integer.MIN_VALUE;
    }

    public boolean isSaved() {
        return mConfig != null;
    }


    /**
     * Returns a negative integer, zero, or a positive integer if this AccessPoint is less than,
     * equal to, or greater than the other AccessPoint.
     * <p>
     * Sort order rules for AccessPoints:
     * 1. Active before inactive
     * 2. Reachable before unreachable
     * 3. Saved before unsaved
     * 4. Network speed value
     * 5. Stronger signal before weaker signal
     * 6. SSID alphabetically
     * <p>
     * Note that AccessPoints with a signal are usually also Reachable,
     * and will thus appear before unreachable saved AccessPoints.
     */
    @Override
    public int compareTo(@NonNull ScanedWifiInfo other) {
        // Active one goes first.
        if (isActive() && !other.isActive()) return -1;
        if (!isActive() && other.isActive()) return 1;

        // Reachable one goes before unreachable one.
        if (isReachable() && !other.isReachable()) return -1;
        if (!isReachable() && other.isReachable()) return 1;

        // Configured (saved) one goes before unconfigured one.
        if (isSaved() && !other.isSaved()) return -1;
        if (!isSaved() && other.isSaved()) return 1;

        // Sort by signal strength, bucketed by level
        int difference = WifiManager.calculateSignalLevel(other.mRssi, 5)
                - WifiManager.calculateSignalLevel(mRssi, 5);
        if (difference != 0) {
            return difference;
        }

        // Sort by title.
        difference = ssid.compareToIgnoreCase(other.ssid);
        if (difference != 0) {
            return difference;
        }

        // Do a case sensitive comparison to distinguish SSIDs that differ in case only
        return ssid.compareTo(other.ssid);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof ScanedWifiInfo)) return false;
        return (this.compareTo((ScanedWifiInfo) other) == 0);
    }

    public boolean matches(WifiConfiguration config, WifiManager wm) {
        if (!ssid.equals(removeDoubleQuotes(config.SSID))
                || (mConfig != null && mConfig.shared != config.shared)) {
            return false;
        }
        LogUtils.d(TAG, "match WifiConfiguration for: " + ssid);
        final int configSecurity = NetworkUtils.getSecurity(config);
        if (mIsPskSaeTransitionMode) {
            if (configSecurity == SECURITY_SAE && wm.isWpa3SaeSupported()) {
                return true;
            } else if (configSecurity == SECURITY_PSK) {
                return true;
            }
        }

        if (mIsOweTransitionMode) {
            if (configSecurity == SECURITY_OWE && wm.isEnhancedOpenSupported()) {
                return true;
            } else if (configSecurity == SECURITY_NONE) {
                return true;
            }
        }

        return mSecurity == configSecurity;
    }

    static String removeDoubleQuotes(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        int length = string.length();
        if ((length > 1) && (string.charAt(0) == '"')
                && (string.charAt(length - 1) == '"')) {
            return string.substring(1, length - 1);
        }
        return string;
    }
}
