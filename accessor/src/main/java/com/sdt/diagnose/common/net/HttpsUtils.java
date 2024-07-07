package com.sdt.diagnose.common.net;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.sdt.diagnose.Device.X_Skyworth.LogManager;
import com.sdt.diagnose.Tr369PathInvoke;
import com.sdt.diagnose.command.Event;
import com.sdt.diagnose.common.DeviceInfoUtils;
import com.sdt.diagnose.common.FileUtils;
import com.sdt.diagnose.common.bean.LogResponseBean;
import com.sdt.diagnose.common.bean.NotificationBean;
import com.sdt.diagnose.common.bean.StandbyBean;
import com.sdt.diagnose.common.log.LogUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HttpsUtils {
    private static final String TAG = "HttpsUtils";
    public static OnUploadCallback mOnUploadCallback;
    private static final CreateSSL mCreateSSL = new CreateSSL();

    public static void uploadFile(String url, String fileFullPath, boolean isSyncRequest, Callback callback) {
        File file = new File(fileFullPath);
        RequestBody requestBody = RequestBody.create(FileUtils.fileToByte(file));

        Request request = new Request.Builder()
                .header("Authorization", "Client-ID " + UUID.randomUUID())
                .url(url)
                .post(requestBody)
                .build();

        execute(call(request), isSyncRequest, callback);
    }

    public static void uploadSpeedData(String url, String content, String dataType, String transactionId, String isEnd) {
        OkHttpClient okHttpClient = mCreateSSL.getCheckedOkHttpClient();
        MediaType mediaType = MediaType.parse("text/x-markdown; charset=utf-8");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("dataType", dataType)    //取值有 upload download failure
                .addHeader("transactionId", transactionId)
                .addHeader("isEnd", isEnd)      //取值有 true false 用于指示测速是否结束
                .post(RequestBody.create(mediaType, content))
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                LogUtils.e(TAG, "uploadSpeedData onFailure: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                LogUtils.d(TAG, "uploadSpeedData protocol: " + response.protocol()
                        + ", code: " + response.code()
                        + ", message: " + response.message());
            }
        });
    }

    public static void uploadLog(String url, String content, String transactionId) {
        OkHttpClient okHttpClient = mCreateSSL.getCheckedOkHttpClient();
        MediaType mediaType = MediaType.parse("text/x-markdown; charset=utf-8");
        Request request = new Request.Builder()
                .header("transactionId", transactionId)
                .url(url)
                .post(RequestBody.create(mediaType, content))
                .build();
        mOnUploadCallback.deleteLog(content.length());

        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                LogUtils.e(TAG, "uploadLog onFailure: " + e.getMessage());
                LogManager.getInstance().stopLog();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                LogUtils.d(TAG, "uploadLog Protocol: " + response.protocol()
                        + ", Code: " + response.code());
                if (response.code() == 200 && response.body() != null) {
                    String responseBody = response.body().string();
                    LogUtils.d(TAG, "uploadLog response.body: " + responseBody);
                    Gson gson = new Gson();
                    LogResponseBean logResponseBean = gson.fromJson(responseBody, LogResponseBean.class);
                    if (logResponseBean != null) {
                        if (logResponseBean.getCode() == 0) {
                            LogUtils.d(TAG, "uploadLog: Continue to upload real-time logs");
                            return;
                        }
                        LogUtils.d(TAG, "uploadLog response code: " + logResponseBean.getCode() + ", stop upload log");
                    } else {
                        LogUtils.e(TAG, "uploadLog: Response body parsing failed");
                    }
                }
                LogUtils.i(TAG, "uploadLog: Stop uploading real-time logs");
                LogManager.getInstance().stopLog();
            }
        });
    }

    public static void requestAppUpgradeStatus(String url, HashMap<String, String> param, Callback callback) {
        OkHttpClient okHttpClient = mCreateSSL.getCheckedOkHttpClient();
        String wholeUrl = buildUrl(url, param);
        LogUtils.d(TAG, "requestAppUpgradeStatus wholeUrl: " + wholeUrl);
        Request request = new Request.Builder()
                .url(wholeUrl)
                .get()
                .build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    public static void requestMqttServerConfigs(String url, String token, HashMap<String, String> param, Callback callback) {
        OkHttpClient okHttpClient = mCreateSSL.getCheckedOkHttpClient();
        String wholeUrl = buildUrl(url, param);
        LogUtils.d(TAG, "requestMqttServerConfigs wholeUrl: " + wholeUrl);
        Request request = new Request.Builder()
                .header("X-Auth-Token", token)
                .url(wholeUrl)
                .get()
                .build();
        okHttpClient.newCall(request).enqueue(callback);
    }

    public static void uploadScreenshotAllowStatus(String url, int status, String transactionId) {
        HashMap<String, String> param = new HashMap<>();
        param.put("deviceId", DeviceInfoUtils.getSerialNumber());
        param.put("confirmCode", String.valueOf(status));
        param.put("transactionId", transactionId);
        String wholeUrl = buildUrl(url, param);
        LogUtils.d(TAG, "uploadScreenshotAllowStatus wholeUrl: " + wholeUrl);
        requestAndCallback(wholeUrl);
    }

    public static void uploadAllowStatus(String url, int status, String confirmMessage, String transactionId) {
        if (TextUtils.isEmpty(url)) {
            LogUtils.e(TAG, "uploadAllowStatus: The reported URL is empty!");
            return;
        }
        HashMap<String, String> param = new HashMap<>();
        param.put("deviceId", DeviceInfoUtils.getSerialNumber());
        param.put("confirmCode", String.valueOf(status));
        param.put("confirmMessage", confirmMessage);
        param.put("transactionId", transactionId);
        String wholeUrl = buildUrl(url, param);
        LogUtils.d(TAG, "uploadAllowStatus wholeUrl: " + wholeUrl);
        requestAndCallback(wholeUrl);
    }

    public static void uploadStandbyStatus(int status) {
        String url = StandbyBean.getInstance().getUpdateUrl();
        if (TextUtils.isEmpty(url)) {
            LogUtils.e(TAG, "uploadStandbyStatus: The upload URL is empty!");
            return;
        }
        HashMap<String, String> param = new HashMap<>();
        param.put("deviceId", DeviceInfoUtils.getSerialNumber());
        param.put("confirmCode", String.valueOf(status));
        String wholeUrl = buildUrl(url, param);
        LogUtils.d(TAG, "uploadStandbyStatus wholeUrl: " + wholeUrl);
        requestAndCallback(wholeUrl);
    }

    public static void uploadNotificationStatus(boolean isCompleted) {
        String url = NotificationBean.getInstance().getUrl();
        if (TextUtils.isEmpty(url)) {
            LogUtils.e(TAG, "uploadNotificationStatus: The upload URL is empty!");
            return;
        }
        HashMap<String, String> param = new HashMap<>();
        param.put("status", String.valueOf(isCompleted));
        String wholeUrl = buildUrl(url, param);
        LogUtils.d(TAG, "uploadNotificationStatus wholeUrl: " + wholeUrl);
        requestAndCallback(wholeUrl);
    }

    private static void requestAndCallback(String wholeUrl) {
        OkHttpClient okHttpClient = mCreateSSL.getCheckedOkHttpClient();
        Request request = new Request.Builder()
                .url(wholeUrl)
                .get()
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                LogUtils.e(TAG, "requestAndCallback onFailure: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                LogUtils.d(TAG, "requestAndCallback onResponse: " + response.protocol()
                        + ", code: " + response.code()
                        + ", message: " + response.message());
            }
        });
    }

    public static void uploadLogFile(String uploadUrl, String filePath, int fileCount) {
        try {
            URL url = new URL(uploadUrl);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            // 允许Input、Output，不使用Cache
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
            con.setSSLSocketFactory(mCreateSSL.getSSLSocketFactory());
            con.setHostnameVerifier((hostname, session) -> true);   // 设置默认主机名验证器接受所有主机名
            con.setConnectTimeout(50000);
            con.setReadTimeout(50000);
            // 设置传送的method=POST
            con.setRequestMethod("POST");
            // 在一次TCP连接中可以持续发送多份数据而不会断开连接
            con.setRequestProperty("Connection", "Keep-Alive");
            // 设置编码
            con.setRequestProperty("Charset", "UTF-8");
            // text/plain能上传纯文本文件的编码格式
            con.setRequestProperty("Content-Type", "text/plain");

            // 指定剩余待上传文件
            String remainingFileCount = String.valueOf(fileCount);
            con.setRequestProperty("RemainingFileCount", remainingFileCount);
            // 指定当前上传的文件名
            String fileName = filePath.substring(filePath.lastIndexOf("/") + 1);
            con.setRequestProperty("Filename", fileName);

            if (fileCount > 0) {
                // 设置DataOutputStream
                DataOutputStream ds = new DataOutputStream(con.getOutputStream());
                // 取得文件的FileInputStream
                FileInputStream fStream = new FileInputStream(filePath);
                // 设置每次写入1024bytes
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];

                int length = -1;
                // 从文件读取数据至缓冲区
                while ((length = fStream.read(buffer)) != -1) {
                    // 将资料写入DataOutputStream中 对于有中文的文件需要使用GBK编码格式
                    // ds.write(new String(buffer, 0, length).getBytes("GBK"));
                    ds.write(buffer, 0, length);
                }
                ds.flush();
                fStream.close();
                ds.close();
            }

            if (con.getResponseCode() == 200) {
                String message = "Successfully uploaded the file via https.";
                LogUtils.e(TAG, "uploadLogFile: " + message + " file path: " + filePath);
                Event.setUploadResponseDBParams("Complete", message);
                // 上传成功，只删除分段保存的那些文件
                if (!filePath.contains(Event.RAW_LOG_FILE)) {
                    File file = new File(filePath);
                    if (file.exists()) {
                        LogUtils.d(TAG, "Wait to delete file: " + filePath);
                        file.delete();
                    }
                }
            } else {
                String message = "The response code obtained via https is: " + con.getResponseCode();
                LogUtils.e(TAG, "uploadLogFile: " + message);
                Event.setUploadResponseDBParams("Error", message);
            }
            con.disconnect();

        } catch (Exception e) {
            String message = "Failed to upload file via https, " + e.getMessage();
            LogUtils.e(TAG, "uploadLogFile: " + message);
            Event.setUploadResponseDBParams("Error", message);
        }
    }

    public static void noticeResponse(String url, HashMap<String, String> param) {
        OkHttpClient okHttpClient = mCreateSSL.getCheckedOkHttpClient();
        String wholeUrl = buildUrl(url, param);
        LogUtils.d(TAG, "noticeResponse url: " + url + ", wholeUrl: " + wholeUrl);
        Request request = new Request.Builder()
                .url(wholeUrl)
                .get()
                .build();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                LogUtils.e(TAG, "noticeResponse onFailure: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                LogUtils.d(TAG, "noticeResponse onResponse: " + response.protocol()
                        + ", code: " + response.code()
                        + ", message: " + response.message());
            }
        });
    }

    /**
     * 构建https请求
     */
    private static Call call(Request httpRequest) {
        return mCreateSSL.getCheckedOkHttpClient().newCall(httpRequest);
    }

    /**
     * 执行 OkHttps call
     */
    private static void execute(Call call, boolean isSyncRequest, Callback callback) {
        if (isSyncRequest) {
            Response response = null;
            try {
                if (call != null) {
                    response = call.execute();
                    if (callback != null) callback.onResponse(call, response);
                }
            } catch (IOException e) {
                if (callback != null) callback.onFailure(call, e);
            } finally {
                if (response != null) response.close();
            }
        } else {
            if (call != null) call.enqueue(callback);
        }
    }

    private static String buildUrl(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }
        // 去掉baseUrl后面的?符号
        if (url.endsWith("?")) {
            url = url.substring(0, url.length() - 1);
        }
        String param = buildUrlParams(params);
        if (!TextUtils.isEmpty(param)) {
            url = url + param;
        }
        return url;
    }

    private static String buildUrlParams(Map<String, String> params) {
        // TODO: 2019/8/6 参数由外部保证 query不能为null
        int count = 0;
        StringBuilder result = new StringBuilder();
        if (null == params || params.isEmpty()) {
            return null;
        }

        final String gap = "&";
        for (String key : params.keySet()) {
            String value = null;

            if (key != null) {
                value = params.get(key);
                if (!TextUtils.isEmpty(value)) {
                    if (count == 0) {
                        result = new StringBuilder("?" + key + "=" + value);
                    } else {
                        result.append(gap).append(key).append("=").append(value);
                    }
                    count++;
                }
            }
        }

        return result.toString();
    }

    public interface OnUploadCallback {
        void deleteLog(int length);
    }

    public static void setOnUploadCallback(OnUploadCallback onUploadCallback) {
        mOnUploadCallback = onUploadCallback;
    }
}
