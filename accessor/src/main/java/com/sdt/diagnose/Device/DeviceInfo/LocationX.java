package com.sdt.diagnose.Device.DeviceInfo;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.sdt.diagnose.common.DeviceInfoUtils;
import com.sdt.diagnose.common.bean.LocationInfo;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.database.DbManager;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.ipinfo.api.IPinfo;
import io.ipinfo.api.model.IPResponse;

public class LocationX {
    private static final String TAG = "LocationX";
    private static LocationX mLocationX;
    private String mApiIp = null;
    private String mApiToken = null;

    LocationX() {
    }

    public static LocationX getInstance() {
        if (null == mLocationX) {
            mLocationX = new LocationX();
        }
        return mLocationX;
    }

    private static String extractIpAddress(String url) {
        // 定义IP地址的正则表达式
        String regex = "ipinfo\\.io/(.*?)/";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);

        // 查找匹配
        if (matcher.find()) {
            // 提取匹配的部分
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private static String extractToken(String url) {
        // 定义Token的正则表达式
        String regex = "token=(\\w+)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(url);

        // 查找匹配
        if (matcher.find()) {
            // 提取匹配的部分
            return matcher.group(1);
        } else {
            return null;
        }
    }

    private boolean parseIpInfoParamsFromSrc() {
        String url = DbManager.getDBParam("Device.DeviceInfo.Location.ExternalSource");
        if (TextUtils.isEmpty(url)) {
            LogUtils.e(TAG, "The value of the ExternalSource parameter is empty");
            return false;
        }
        mApiIp = extractIpAddress(url);
        LogUtils.d(TAG, "Parsed ip: " + mApiIp);

        mApiToken = extractToken(url);
        LogUtils.d(TAG, "Parsed token: " + mApiToken);

        return !TextUtils.isEmpty(mApiIp) && !TextUtils.isEmpty(mApiToken);
    }

    /**
     * 使用ipinfo.io接口的方式获取Location，此方法需要使用官方提供的token来授权才能使用，token每个月有使用上限，
     * 因此暂时不考虑此方法。
     * <p>
     * API相关信息请查阅：<a href="https://ipinfo.io/">ipinfo.io</a>
     */
    public boolean handleIpInfoIoApi() {
        if (!parseIpInfoParamsFromSrc()) {
            LogUtils.e(TAG, "Failed to parse ExternalSource which does not contain expected IP and Token");
            return false;
        }

        String origin = DbManager.getDBParam("Device.DeviceInfo.Location.DataObject");
        if (!TextUtils.isEmpty(origin)) {
            LogUtils.d(TAG, "Stored data: " + origin);
            Gson gson = new Gson();
            LocationInfo bean = gson.fromJson(origin, LocationInfo.class);
            if (bean != null) {
                String ip = bean.getIp();
                if (ip.equals(mApiIp)) {
                    LogUtils.i(TAG, "The data already exists, no need to call the API repeatedly");
                    return true;
                }
            }
        }

        // 调用ipinfo.io接口
        IPinfo ipInfo = new IPinfo.Builder()
                .setToken(mApiToken)
                .build();

        try {
            IPResponse response = ipInfo.lookupIP(mApiIp);
            // Print out the response
            LogUtils.d(TAG, "Get IPResponse: " + response);

            // Handle the response
            JSONObject params = new JSONObject();
            params.put("ip", response.getIp());
            params.put("city", response.getCity());
            params.put("region", response.getRegion());
            params.put("country", response.getCountryCode());
            params.put("loc", response.getLocation());
            params.put("org", response.getOrg());
            params.put("timezone", response.getTimezone());
            params.put("readme", "https://ipinfo.io/missingauth");

            // Print out the json
            LogUtils.d(TAG, "Returned Json: " + params);

            // Store data in the database
            String time = DeviceInfoUtils.getTime();
            DbManager.setDBParam("Device.DeviceInfo.Location.AcquiredTime", time);
            String data = params.toString();
            DbManager.setDBParam("Device.DeviceInfo.Location.DataObject", data);

        } catch (Exception e) {
            // Handle rate limits here.
            LogUtils.e(TAG, "getLocationInfo error, " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * 通过Get的方式直接获取ipinfo.io的JSON数据，此方式可以避开使用token，免去token使用次数的限制
     * URL: <a href="https://ipinfo.io/json">https://ipinfo.io/json</a>
     */
    public String getIpInfoIoJson() {
        // 访问返回IP地址的网站
        URL url;
        HttpURLConnection connection;
        try {
            url = new URL("https://ipinfo.io/json");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            // 读取网站返回的IP地址字符串
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            StringBuilder json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
            LogUtils.d(TAG, "The JSON data acquired from \"ipinfo.io\" is: " + json);

            // 关闭连接和输入流
            reader.close();
            connection.disconnect();

            // 更新时间
            String time = DeviceInfoUtils.getTime();
            DbManager.setDBParam("Device.DeviceInfo.Location.1.AcquiredTime", time);

            return json.toString();
        } catch (Exception e) {
            LogUtils.e(TAG, "getIpInfoIoJson error, " + e.getMessage());
        }
        return "";
    }

    /**
     * 获取公网IP地址
     */
    public static String getIpv4NetIP() {
        // 访问返回IP地址的网站
        URL url;
        HttpURLConnection connection;
        try {
            url = new URL("https://checkip.amazonaws.com/");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            // 读取网站返回的IP地址字符串
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String ipAddress = reader.readLine();
            // 关闭连接和输入流
            reader.close();
            connection.disconnect();
            // 提取IP地址字符串
            String regex = "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b";
            Pattern pattern = Pattern.compile(regex);
            Matcher matcher = pattern.matcher(ipAddress);
            if (matcher.find()) {
                return matcher.group();
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getIpv4NetIP error, " + e.getMessage());
        }
        return "";
    }

}
