package com.sdt.android.tr369.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.sdt.diagnose.command.Event;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.common.net.HttpsUtils;
import com.sdt.diagnose.database.DbManager;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class BugreportReceiver extends BroadcastReceiver {
    private static final String TAG = "BugreportReceiver";
    public static final String ACTION_TMS_BUGREPORT_FINISH =
            "com.sdt.tms.intent.action.BUGREPORT_FINISH";
    public static final String ACTION_TMS_BUGREPORT_ERROR =
            "com.sdt.tms.intent.action.BUGREPORT_ERROR";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (ACTION_TMS_BUGREPORT_FINISH.equals(action)) {
            String filePath = intent.getStringExtra("path");
            String uploadUrl = DbManager.getDBParam("Device.X_Skyworth.BugReport.Url");
            LogUtils.d(TAG, "filePath: " + filePath + ", uploadUrl: " + uploadUrl);
            if (!TextUtils.isEmpty(uploadUrl) && !TextUtils.isEmpty(filePath)) {
                uploadBugReportFile(uploadUrl, filePath);
            } else {
                Event.isBugReportRunning = false;
            }
        } else if (ACTION_TMS_BUGREPORT_ERROR.equals(action)) {
            String reason = intent.getStringExtra("reason");
            LogUtils.e(TAG, "Bug report execution failed, " + reason);
            Event.isBugReportRunning = false;
        }
    }

    private void uploadBugReportFile(String uploadUrl, String filePath) {
        HttpsUtils.uploadFile(uploadUrl, filePath, false, new Callback() {
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {
                LogUtils.d(TAG, "uploadBugReportFile onResponse: " + response.protocol()
                        + ", code: " + response.code());
                Event.isBugReportRunning = false;
                if (response.code() == 200) {
                    File file = new File(filePath);
                    if (file.exists()) {
                        LogUtils.d(TAG, "uploadBugReportFile: Wait to delete file: " + filePath);
                        file.delete();
                    }
                    String message = "Bug report file uploaded successfully";
                    LogUtils.e(TAG, "uploadBugReportFile: " + message + ", file path: " + filePath);
                    return;
                }
                String message = "Failed to upload bug report file, " + response.protocol()
                        + " " + response.code() + " " + response.message();
                LogUtils.e(TAG, "uploadBugReportFile: " + message);
            }
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                File file = new File(filePath);
                file.delete();
                Event.isBugReportRunning = false;
                String message = "Failed to upload bug report file, " + e.getMessage();
                LogUtils.e(TAG, "uploadBugReportFile: " + message);
            }
        });
    }
}
