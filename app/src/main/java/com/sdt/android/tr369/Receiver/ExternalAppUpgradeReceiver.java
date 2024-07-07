package com.sdt.android.tr369.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import androidx.annotation.NonNull;

import com.sdt.diagnose.Tr369PathInvoke;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.bean.AppUpgradeResponseBean;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.common.net.HttpsUtils;
import com.sdt.diagnose.database.DbManager;

import java.io.IOException;
import java.util.HashMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * @Author Outis
 * @Date 2022/12/16 14:32
 * @Version 1.0
 */
public class ExternalAppUpgradeReceiver extends BroadcastReceiver {
    private static final String TAG = "ExternalAppUpgradeReceiver";

    // 接收来自SkyUpdate.apk的广播，接收下载/安装APP的状态
    public static final String ACTION_BOOT_DIAGNOSE_APP_DOWNLOAD =
            "com.skyworth.diagnose.Broadcast.DownloadStatus";
    public static final String ACTION_BOOT_DIAGNOSE_APP_UPGRADE =
            "com.skyworth.diagnose.Broadcast.UpgradeStatus";

    // 上报APP下载和安装状态的接口，IP和Port由Device.X_Skyworth.ManagementServer.Url决定
    private static final String URL_DOWNLOAD_RESULT_REPORT = "/appList/downloadResult";
    private static final String URL_INSTALL_RESULT_REPORT = "/appList/installResult";

    // 重试发送状态的次数
    private static final int DEFAULT_REQUEST_RETRY_COUNT = 3;
    // 重试发送状态的延迟 (单位: 秒)
    private static final int DEFAULT_REQUEST_RETRY_DELAY = 10 * 1000;

    private static int mRetryCount = 0;
    private static final int MSG_REQUEST_RETRY = 3309;
    private static boolean isRequestSuccess = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        LogUtils.d(TAG, "onReceive action: " + action);
        if (ACTION_BOOT_DIAGNOSE_APP_DOWNLOAD.equals(action)) {
            LogUtils.d(TAG, "ACTION_BOOT_DIAGNOSE_APP_DOWNLOAD");
            if (checkPackageNameValidity(intent)) handleExternalAppDownloadStatus(intent);
        } else if (ACTION_BOOT_DIAGNOSE_APP_UPGRADE.equals(action)) {
            LogUtils.d(TAG, "ACTION_BOOT_DIAGNOSE_APP_UPGRADE");
            if (checkPackageNameValidity(intent)) handleExternalAppUpgradeStatus(intent);
        } else if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)) {
            if (isConnected(context.getApplicationContext())) {
                if (DbManager.getDBParam("Device.X_Skyworth.UpgradeResponse.Enable").equals("1")) {
                    retryReportResponse();
                }
            }
        }
    }

    private boolean checkPackageNameValidity(Intent input) {
        String pkgName = input.getStringExtra("packageName");
        if (pkgName != null) {
            LogUtils.d(TAG, "checkPackageNameValidity pkgName: " + pkgName);
            return pkgName.equals(GlobalContext.getContext().getPackageName());
        }
        return false;
    }

    private boolean isConnected(Context context) {
        ConnectivityManager cm =
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    private void handleExternalAppDownloadStatus(Intent input) {
        // 将下载状态通知http
        AppUpgradeResponseBean requestBean = new AppUpgradeResponseBean(input);
        HashMap<String, String> hashMap = requestBean.toHashMap();
        LogUtils.d(TAG, "execute POST download request, params: " + hashMap);

        String url = Tr369PathInvoke.getInstance().getString("Device.X_Skyworth.ManagementServer.Url");
        if (!url.isEmpty()) {
            HttpsUtils.noticeResponse(url + URL_DOWNLOAD_RESULT_REPORT, hashMap);
        } else {
            LogUtils.e(TAG, "The URL of the download result report is illegal");
        }
    }

    private void handleExternalAppUpgradeStatus(Intent intent) {
        // 将安装状态通知http
        AppUpgradeResponseBean requestBean = new AppUpgradeResponseBean(intent);
        HashMap<String, String> hashMap = requestBean.toHashMap();
        LogUtils.d(TAG, "execute POST install request, params: " + hashMap);

        String url = Tr369PathInvoke.getInstance().getString("Device.X_Skyworth.ManagementServer.Url");
        if (url.isEmpty()) {
            LogUtils.e(TAG, "The URL of the install result report is illegal");
            return;
        }

        String reportUrl = url + URL_INSTALL_RESULT_REPORT;
        HttpsUtils.requestAppUpgradeStatus(reportUrl, hashMap, new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                LogUtils.e(TAG, "Failed to report installation status");
                DbManager.setDBParam("Device.X_Skyworth.UpgradeResponse.Enable", "1");
                requestBean.setResponseDBParams();
                retryReportResponse();
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                LogUtils.d(TAG, "requestAppUpgradeStatus Protocol: " + response.protocol()
                        + ", Code: " + response.code());
                if (response.code() == 200) {
                    DbManager.setDBParam("Device.X_Skyworth.UpgradeResponse.Enable", "0");
                } else {
                    DbManager.setDBParam("Device.X_Skyworth.UpgradeResponse.Enable", "1");
                    requestBean.setResponseDBParams();
                    retryReportResponse();
                }
            }
        });
    }

    private static void handleResponse() {
        final HashMap<String, String> hashMap = AppUpgradeResponseBean.getResponseDBParams();
        LogUtils.d(TAG, "execute POST install request, params: " + hashMap);

        String url = Tr369PathInvoke.getInstance().getString("Device.X_Skyworth.ManagementServer.Url");
        if (url.isEmpty()) {
            LogUtils.e(TAG, "The URL of the install result report is illegal");
            return;
        }

        String reportUrl = url + URL_INSTALL_RESULT_REPORT;
        HttpsUtils.requestAppUpgradeStatus(reportUrl, hashMap,
                new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        LogUtils.e(TAG, "Reporting the installation status failed again");
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        LogUtils.d(TAG, "handleResponse Protocol: " + response.protocol()
                                + ", Code: " + response.code());
                        if (response.code() == 200) {
                            DbManager.setDBParam("Device.X_Skyworth.UpgradeResponse.Enable", "0");
                            isRequestSuccess = true;
                        }
                    }
                });
    }

    public static void retryReportResponse() {
        mRetryCount = 0;
        isRequestSuccess = false;

        Handler handler = ReportResponseThread.getInstance().getHandler();
        handler.sendEmptyMessage(MSG_REQUEST_RETRY);
    }

    private static class ReportResponseThread {
        private final HandlerThread mHandlerThread = new HandlerThread("ReportResponseThread");
        private final Handler mHandler;

        private ReportResponseThread() {
            LogUtils.d(TAG, "ReportResponseThread Create");
            mHandlerThread.start();
            mHandler = new Handler(mHandlerThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    if (msg.what == MSG_REQUEST_RETRY) {
                        if (mRetryCount > DEFAULT_REQUEST_RETRY_COUNT || isRequestSuccess) {
                            mHandler.removeMessages(MSG_REQUEST_RETRY);
                        } else {
                            handleResponse();
                        }
                        mRetryCount++;
                        mHandler.sendEmptyMessageDelayed(MSG_REQUEST_RETRY, DEFAULT_REQUEST_RETRY_DELAY);
                    }
                }
            };
        }

        private static volatile ReportResponseThread instance = null;

        public static ReportResponseThread getInstance() {
            if (instance == null) {
                synchronized (ReportResponseThread.class) {
                    if (instance == null) {
                        instance = new ReportResponseThread();
                    }
                }
            }
            return instance;
        }

        public Handler getHandler() {
            return mHandler;
        }
    }
}

