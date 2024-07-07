package com.sdt.diagnose.common;

import static com.sdt.diagnose.common.NetworkUtils.WIFI_SECURITY_OWE;
import static com.sdt.diagnose.common.NetworkUtils.WIFI_SECURITY_PSK;
import static com.sdt.diagnose.common.NetworkUtils.WIFI_SECURITY_SAE;
import static com.sdt.diagnose.common.NetworkUtils.WIFI_SECURITY_SUITE_B_192;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.text.TextUtils;

import com.sdt.diagnose.common.bean.ScanedWifiInfo;
import com.sdt.diagnose.common.log.LogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 缓存盒子扫描到的wifi 信息列表
 */
public class ScanWifiInfosManager extends AbstractCachedArray<ScanedWifiInfo> {
    private static final String TAG = "ScanWifiInfosManager";

    public ScanWifiInfosManager(Context context) {
        super(context);
    }

    @Override
    public void buildList(Context context) {
        WifiManager mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (!NetworkUtils.isWifiEnabled(mWifiManager)) {
            LogUtils.e(TAG, "Wifi disable.");
            return;
        }
        if (!mWifiManager.startScan()) {
            LogUtils.e(TAG, "Wifi scan failed.");
            return;
        }
        // 堵塞线程6秒,等待wifi 扫描结束.正常是监听WifiManager.SCAN_RESULTS_AVAILABLE_ACTION广播
        // 但因为监听广播不能堵塞住线程,而且到接收到广播差不多有5秒左右的间隔,所以此处就采用6秒,哪怕广播延迟也要去获取列表
        try {
            Thread.sleep(6000);
        } catch (InterruptedException e) {
            LogUtils.e(TAG, "buildList error, " + e.getMessage());
        }

        fetchScansAndConfigs(context, mWifiManager);
    }

    private void fetchScansAndConfigs(Context context, WifiManager wm) {
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        List<ScanResult> newScanResults = wm.getScanResults();
        if (null == newScanResults) {
            return;
        }
        LogUtils.d(TAG, "newScanResults size: " + newScanResults.size());
        // Filter all unsupported networks from the scan result list
        final List<ScanResult> filteredScanResults =
                filterScanResultsByCapabilities(wm, newScanResults);
        LogUtils.d(TAG, "filteredScanResults size: " + filteredScanResults.size());

        // 把同名同加密方式的ssid放在同一个list中,后面只取其中一个作为一个wifi看待
        HashMap<String, List<ScanResult>> cached = new HashMap<>();
        for (ScanResult scan : filteredScanResults) {
            String apKey = NetworkUtils.getKey(wm, scan);
            List<ScanResult> resultList;
            if (cached.containsKey(apKey)) {
                resultList = cached.get(apKey);
            } else {
                resultList = new ArrayList<>();
                cached.put(apKey, resultList);
            }
            if (resultList != null) resultList.add(scan);
        }

        List<WifiConfiguration> wifiConfigs = wm.getConfiguredNetworks();

        // 当前连接的wifi
        WifiInfo wifiInfo = wm.getConnectionInfo();

        // 当前网络连接状态
        NetworkInfo networkInfo = cm.getNetworkInfo(wm.getCurrentNetwork());

        for (Map.Entry<String, List<ScanResult>> entry : cached.entrySet()) {
            List<ScanResult> results = entry.getValue();
            if (results != null && results.size() > 0) {
                ScanedWifiInfo info = null;

                // 当前有网络连接,构建 当前连接的 WIFI ScanedWifiInfo结构体
                if (wifiInfo != null && wifiInfo.getNetworkId() != WifiConfiguration.INVALID_NETWORK_ID
                        && !TextUtils.isEmpty(wifiInfo.getBSSID())) {
                    for (ScanResult scan : results) {
                        if (TextUtils.equals(wifiInfo.getBSSID(), scan.BSSID)) {
                            info = new ScanedWifiInfo(wm, scan);
                            info.update(wifiInfo, networkInfo);
                        }
                    }
                }
                // 未匹配到 WifiInfo
                if (info == null) {
                    info = new ScanedWifiInfo(wm, results.get(0));
                }
                info.setKey(entry.getKey());
                // 用 WifiConfiguration 填充 ScanedWifiInfo 对象
                if (wifiConfigs != null) {
                    for (WifiConfiguration config : wifiConfigs) {
                        if (info.matches(config, wm)) {
                            info.update(config);
                        }
                    }
                }
                add(info);
            }
        }
        if (!isEmpty()) {
            mList.sort(ScanedWifiInfo::compareTo);
        }
    }

    /**
     * Filters unsupported networks from scan results. New WPA3 networks and OWE networks
     * may not be compatible with the device HW/SW.
     *
     * @param scanResults List of scan results
     * @return List of filtered scan results based on local device capabilities
     */
    private List<ScanResult> filterScanResultsByCapabilities(WifiManager mWifiManager, List<ScanResult> scanResults) {
        if (scanResults == null) {
            return null;
        }

        // Get and cache advanced capabilities
        final boolean isOweSupported = mWifiManager.isEnhancedOpenSupported();
        final boolean isSaeSupported = mWifiManager.isWpa3SaeSupported();
        final boolean isSuiteBSupported = mWifiManager.isWpa3SuiteBSupported();

        List<ScanResult> filteredScanResultList = new ArrayList<>();

        // Iterate through the list of scan results and filter out APs which are not
        // compatible with our device.
        for (ScanResult scanResult : scanResults) {
            if (scanResult.SSID == null || scanResult.SSID.isEmpty() ||
                    scanResult.capabilities.contains("[IBSS]")) {
                continue;
            }
            if (scanResult.capabilities.contains(WIFI_SECURITY_PSK)) {
                // All devices (today) support RSN-PSK or WPA-PSK
                // Add this here because some APs may support both PSK and SAE and the check
                // below will filter it out.
                filteredScanResultList.add(scanResult);
                continue;
            }
            if ((scanResult.capabilities.contains(WIFI_SECURITY_SUITE_B_192) && !isSuiteBSupported)
                    || (scanResult.capabilities.contains(WIFI_SECURITY_SAE) && !isSaeSupported)
                    || (scanResult.capabilities.contains(WIFI_SECURITY_OWE) && !isOweSupported)) {
                continue;
            }
            filteredScanResultList.add(scanResult);
        }

        return filteredScanResultList;
    }
}
