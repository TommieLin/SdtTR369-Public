package com.sdt.diagnose.common.net;

import com.sdt.diagnose.command.Event;
import com.sdt.diagnose.common.FileUtils;
import com.sdt.diagnose.common.log.LogUtils;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;

import javax.net.ssl.HttpsURLConnection;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class HttpUtils {
    public static final String TAG = "HttpUtils";

    public static void uploadFile(String url, String fileFullPath, boolean isSyncRequest, Callback callback) {
        try {
            File file = new File(fileFullPath);

            HttpLoggingInterceptor logInterceptor = new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
                @Override
                public void log(String message) {
                    try {
                        String text = URLDecoder.decode(message, "utf-8");
                        LogUtils.d(TAG, "HttpLoggingInterceptor text: " + text);
                    } catch (UnsupportedEncodingException e) {
                        LogUtils.e(TAG, "HttpLoggingInterceptor error: " + e.getMessage());
                    }
                }
            });
            logInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
            OkHttpClient client = new OkHttpClient.Builder().addNetworkInterceptor(logInterceptor).build();

            RequestBody requestBody = RequestBody.create(FileUtils.fileToByte(file));

            Request request = new Request.Builder()
                    .addHeader("Accept", "*/*")
                    .url(url)
                    .post(requestBody)
                    .build();

            if (isSyncRequest) {
                Call call = client.newCall(request);
                try (Response response = call.execute()) {
                    if (callback != null) callback.onResponse(call, response);
                } catch (IOException e) {
                    if (callback != null) callback.onFailure(call, e);
                }
            } else {
                client.newCall(request).enqueue(callback);
            }
        } catch (Exception ex) {
            // Handle the error
            LogUtils.e(TAG, "uploadLog error: " + ex.getMessage());
        }
    }

    public static void uploadLogFile(String uploadUrl, String filePath, int fileCount) {
        try {
            URL url = new URL(uploadUrl);
            HttpsURLConnection con = (HttpsURLConnection) url.openConnection();
            // 允许Input、Output，不使用Cache
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setUseCaches(false);
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
                    // 将资料写入DataOutputStream中
                    // ds.write(new String(buffer, 0, length).getBytes("GBK"));
                    ds.write(buffer, 0, length);
                }
                ds.flush();
                fStream.close();
                ds.close();
            }

            if (con.getResponseCode() == 200) {
                String message = "Successfully uploaded the file via http.";
                LogUtils.e(TAG, "uploadLogFile: " + message + " file path: " + filePath);
                Event.setUploadResponseDBParams("Complete", message);
                // 上传成功，只删除分段保存的那些文件
                if (!filePath.contains(Event.RAW_LOG_FILE)) {
                    File file = new File(filePath);
                    if (file.exists()) {
                        LogUtils.d(TAG, "Http: Wait to delete file: " + filePath);
                        file.delete();
                    }
                }
            } else {
                String message = "The response code obtained via http is: " + con.getResponseCode();
                LogUtils.e(TAG, "uploadLogFile: " + message);
                Event.setUploadResponseDBParams("Error", message);
            }
            con.disconnect();

        } catch (Exception e) {
            String message = "Failed to upload file via http, " + e.getMessage();
            LogUtils.e(TAG, "uploadLogFile: " + message);
            Event.setUploadResponseDBParams("Error", message);
        }
    }
}
