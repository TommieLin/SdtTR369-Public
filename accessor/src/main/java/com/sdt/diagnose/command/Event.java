package com.sdt.diagnose.command;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.TrafficStats;
import android.net.Uri;
import android.os.Environment;
import android.os.PowerManager;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.sdt.annotations.Tr369Set;
import com.sdt.diagnose.Device.X_Skyworth.App.AppX;
import com.sdt.diagnose.Tr369PathInvoke;
import com.sdt.diagnose.common.DeviceInfoUtils;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.NetworkUtils;
import com.sdt.diagnose.common.ScreenRecordActivity;
import com.sdt.diagnose.common.ScreenRecordService;
import com.sdt.diagnose.common.ScreenShot2;
import com.sdt.diagnose.common.ShellUtils;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.common.net.HttpUtils;
import com.sdt.diagnose.common.net.HttpsUtils;
import com.sdt.diagnose.database.DbManager;
import com.sdt.diagnose.traceroute.TraceRoute;
import com.sdt.diagnose.traceroute.TraceRouteManager;
import com.skyworth.scrrtcsrv.Device;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;

public class Event {
    private static final String TAG = "TR369 Event";
    private static final String REBOOT = "Reboot";
    private static final String FACTORY_RESET = "FactoryReset";
    private static final String UPLOAD_FILE = "UploadFile";
    private static final String DOWNLOAD_FILE = "DownloadFile";
    private static final String UPGRADE_FILE = "UpgradeFile";
    private static final String IP_PING = "IPPing";
    private static final String TRACE_ROUTE = "TraceRoute";
    private static final String DOWNLOAD_DIAGNOSTICS = "DownloadDiagnostics";
    private static final String SHORT_MESSAGE = "ShortMessage";
    private static final String SCREENSHOT_TYPE = "X Skyworth Screenshot File";
    private static final String VIDEO_TYPE = "X Skyworth Video File";
    private static final String APP_ICON_TYPE = "X Skyworth App Icon File";
    private static final String BUG_REPORT_TYPE = "X Skyworth Bug Report File";
    private static final String CONFIG_FILE_TYPE = "1 Vendor Configuration File";
    private static final String LOG_FILE_TYPE = "2 Vendor Log File";
    private static final String ACTION_BOOT_EXTERNAL_SYS = "com.skw.ota.update.ExternalSysUpdate";
    private static final String ACTION_BOOT_EXTERNAL_APP = "com.skw.ota.update.ExternalAppUpdate";
    private static final String OTA_NEW_PARAMS = "newParams";
    private static final String OTA_PKG_NAME = "packageName";

    private static final String SPLIT = "###";
    private static final int INDEX_COMMAND = 0;
    private static final int INDEX_PARAM_1 = 1;
    private static final int INDEX_PARAM_2 = 2;
    private static final int INDEX_PARAM_3 = 3;
    private static final int INDEX_PARAM_4 = 4;

    // Upload log
    private static final String format = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final SimpleDateFormat df = new SimpleDateFormat(format);
    public static final String RAW_LOG_FILE = "logcat_tr369.log";
    private static final String LOG_SOURCE_DIR_PATH = "/data/tcpdump/";

    // Bug report
    public static boolean isBugReportRunning = false;
    private static final String BUGREPORT_DIR = "bugreports";
    private static final String INTENT_BUGREPORT_REQUESTED =
            "com.android.internal.intent.action.BUGREPORT_REQUESTED";
    private static final String SHELL_APP_PACKAGE = "com.android.shell";
    private static final String EXTRA_BUGREPORT_TYPE = "android.intent.extra.BUGREPORT_TYPE";
    private static final String EXTRA_TMS_BUGREPORT_DIR = "android.intent.extra.tms.BUGREPORT_DIR";

    @Tr369Set("skyworth.tr369.event")
    public boolean SK_TR369_SetEventParams(String path, String value) {
        LogUtils.d(TAG, "skyworth.tr369.event path: " + path + ", value: " + value);
        String[] strings = split(value);
        if (strings == null || strings.length == 0) return false;

        switch (strings[INDEX_COMMAND]) {
            case REBOOT:
                ((PowerManager) GlobalContext.getContext().getSystemService(Context.POWER_SERVICE)).reboot("");
                break;
            case FACTORY_RESET:
                factoryReset();
                break;
            case UPLOAD_FILE:
                upload(strings);
                break;
            case DOWNLOAD_FILE:
                downloadFile(strings);
                break;
            case UPGRADE_FILE:
                // 发送升级广播
                upgradeSw(strings);
                break;
            case IP_PING:
                ping(strings);
                break;
            case TRACE_ROUTE:
                TraceRoute traceroute = TraceRouteManager.getInstance().getTraceRoute();
                DbManager.delMultiObject("Device.IP.Diagnostics.TraceRoute.RouteHops");
                traceroute.getTraces().clear();
                traceroute.executeTraceroute();
                break;
            case DOWNLOAD_DIAGNOSTICS:
                calcNetSpeed(strings);
                break;
            case SHORT_MESSAGE:
                LogUtils.d(TAG, "SHORT_MESSAGE String: " + value);
                String[] values = value.split(SPLIT, 2);
                if (values.length <= INDEX_PARAM_1) {
                    LogUtils.e(TAG, "Parameter error in SHORT_MESSAGE Event, values.len: " + values.length);
                    break;
                }
                ShortMessageUtils.handleShortMessage(values[INDEX_PARAM_1]);
                break;
            default:
                LogUtils.e(TAG, "Not Implemented, skyworth.tr369.event: " + value);
                break;
        }

        return true;
    }

    private void calcNetSpeed(String[] params) {
        if (params.length <= INDEX_PARAM_2) {
            LogUtils.e(TAG, "Parameter error in calcNetSpeed() function, params.len: " + params.length);
            DbManager.setDBParam("Device.IP.Diagnostics.DownloadDiagnostics.Status", "Error_Internal");
            return;
        }
        String url = params[INDEX_PARAM_1];
        String duration = params[INDEX_PARAM_2];
        LogUtils.d(TAG, "calcNetSpeed url: " + url + ", duration: " + duration);

        long startTime = System.currentTimeMillis();
        long exc_duration;
        if (TextUtils.isEmpty(duration)) {
            exc_duration = 10000;
        } else {
            exc_duration = Long.parseLong(duration) * 1000 - 500;
        }

        CountDownLatch lock = new CountDownLatch(1);
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(url).build();
        Call call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                LogUtils.e(TAG, "Failed to calculate network speed. Failure Message: " + e.getMessage());
                long endTime = System.currentTimeMillis();
                DbManager.setDBParam("Device.IP.Diagnostics.DownloadDiagnostics.EOMTime", df.format(endTime));
                DbManager.setDBParam("Device.IP.Diagnostics.DownloadDiagnostics.Status", "Error_TransferFailed");
                call.cancel();
                lock.countDown();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response)
                    throws IOException {
                if (response.body() == null) {
                    LogUtils.e(TAG, "calcNetSpeed error. response body returns a null pointer");
                    return;
                }
                // 下载 outputStream inputStream
                InputStream inputStream = Objects.requireNonNull(response.body()).byteStream();
                //文件的总长度
                long maxLen = Objects.requireNonNull(response.body()).contentLength();
                LogUtils.d(TAG, "calcNetSpeed onResponse maxLen: " + maxLen);
                byte[] bytes = new byte[1024];

                int readLength = 0;
                long cureeLength = 0;

                long startRx = TrafficStats.getTotalRxBytes();
                long endTime = System.currentTimeMillis();

                while ((readLength = inputStream.read(bytes)) != -1) {
                    cureeLength += readLength;
                    endTime = System.currentTimeMillis();
                    if (cureeLength < maxLen) {
                        if (endTime - startTime >= exc_duration) {
                            break;
                        }
                    } else {
                        break;
                    }
                }

                DbManager.setDBParam("Device.IP.Diagnostics.DownloadDiagnostics.EOMTime", df.format(endTime));
                LogUtils.d(TAG, "calcNetSpeed onResponse cureeLength: " + cureeLength);
                long endRx = TrafficStats.getTotalRxBytes();
                LogUtils.d(TAG, "calcNetSpeed onResponse Rx length: " + (endRx - startRx));
                DbManager.setDBParam("Device.IP.Diagnostics.DownloadDiagnostics.TestBytesReceived",
                        String.valueOf(cureeLength));
                call.cancel();
                LogUtils.d(TAG, "calcNetSpeed onResponse TestBytesReceived: " + cureeLength
                        + ", isCanceled: " + call.isCanceled());

                DbManager.setDBParam("Device.IP.Diagnostics.DownloadDiagnostics.Status", "Complete");
                inputStream.close();
                lock.countDown();
            }
        });

        DbManager.setDBParam("Device.IP.Diagnostics.DownloadDiagnostics.BOMTime", df.format(startTime));
        try {
            lock.await(exc_duration + 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            LogUtils.e(TAG, "calcNetSpeed lock.await call failed, " + e.getMessage());
            DbManager.setDBParam("Device.IP.Diagnostics.DownloadDiagnostics.Status", "Error_Internal");
        }
    }

    private String[] split(String value) {
        if (value == null || TextUtils.isEmpty(value)) return null;
        return value.split(SPLIT);
    }

    private void upgradeSw(String[] params) {
        int paramLen = params.length;
        String upgradeJson;
        String upgradeFileType;
        LogUtils.d(TAG, "upgradeSw params.length: " + params.length);
        if (paramLen > INDEX_PARAM_2) {
            upgradeJson = params[INDEX_PARAM_1];
            upgradeFileType = params[INDEX_PARAM_2];
            LogUtils.d(TAG, "upgradeSw: upgradeJson: " + upgradeJson
                    + ", fileType: " + upgradeFileType);
            // 如果是.zip结尾表示是系统升级,.apk结尾表示是app升级
            if (upgradeJson.contains(".zip")) {
                handleSysUpgradeRequest(upgradeJson);
            } else if (upgradeJson.contains(".apk")) {
                handleAppUpgradeRequest(upgradeJson);
            }
        }
    }

    private void handleSysUpgradeRequest(String upgradeJson) {
        Intent intent = new Intent();
        intent.setPackage("com.sdt.ota");
        intent.setAction(ACTION_BOOT_EXTERNAL_SYS);
        intent.putExtra(OTA_NEW_PARAMS, upgradeJson);
        intent.putExtra(OTA_PKG_NAME, GlobalContext.getContext().getPackageName());
        GlobalContext.getContext().sendBroadcast(intent);
    }

    private void handleAppUpgradeRequest(String upgradeJson) {
        try {
            JSONObject jsonObject = new JSONObject(upgradeJson);
            String planId = jsonObject.getString("reference");
            String packageName = jsonObject.getString("packageName");
            if (!AppX.isPkgAllowedToInstall(packageName)) {
                // 检查黑白名单，判断是否允许下载，不允许则通知服务器更新失败
                LogUtils.e(TAG, "TMS Warning: Installation of Package {"
                        + packageName + "} is prohibited");
                HashMap<String, String> params = new HashMap<>();
                params.put("planId", planId);
                params.put("deviceId", DeviceInfoUtils.getSerialNumber());
                params.put("operatorCode", DeviceInfoUtils.getOperatorName());
                params.put("status", "false");
                params.put("msg", "Application installation is prohibited");
                String url = Tr369PathInvoke.getInstance().getString(
                        "Device.X_Skyworth.ManagementServer.Url");
                if (!url.isEmpty()) {
                    HttpsUtils.noticeResponse(url + "/appList/downloadResult", params);
                } else {
                    LogUtils.e(TAG, "ManagementServer URL is empty.");
                }
                return;
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "handleAppUpgradeRequest error, " + e.getMessage());
            return;
        }
        // 通知com.sdt.ota进行应用安装或更新
        Intent intent = new Intent();
        intent.setPackage("com.sdt.ota");
        intent.setAction(ACTION_BOOT_EXTERNAL_APP);
        intent.putExtra(OTA_NEW_PARAMS, upgradeJson);
        intent.putExtra(OTA_PKG_NAME, GlobalContext.getContext().getPackageName());
        GlobalContext.getContext().sendBroadcast(intent);
    }

    private void downloadFile(String[] params) {
        if (params.length <= INDEX_PARAM_2) {
            LogUtils.e(TAG, "Parameter error in downloadFile() function, params.len: " + params.length);
            return;
        }

        OkHttpClient.Builder okHttpClientBuilder = new OkHttpClient.Builder();
        okHttpClientBuilder.addNetworkInterceptor(new HttpLoggingInterceptor(new HttpLoggingInterceptor.Logger() {
            @Override
            public void log(String message) {
                LogUtils.d(TAG, "HttpLoggingInterceptor message: " + message);
            }
        }));
        Request request = new Request.Builder()
                .url(params[INDEX_PARAM_1])
                .build();
        Call call = okHttpClientBuilder.build().newCall(request);
        try {
            call.enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    LogUtils.e(TAG, "Failed to download file. Failure Message: " + e.getMessage());
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (200 == response.code()) {
                        if (response.body() == null) {
                            LogUtils.e(TAG, "downloadFile error. response.body() is a null pointer");
                            return;
                        }
                        File file = new File("/cache/", "update.zip");
                        FileOutputStream fos = new FileOutputStream(file);
                        InputStream inputStream = Objects.requireNonNull(response.body()).byteStream();
                        byte[] buffer = new byte[1024 * 4];
                        int len = 0;
                        float count = 0;
                        long contentLength = Objects.requireNonNull(response.body()).contentLength();
                        while ((len = inputStream.read(buffer)) != -1) {
                            count += len;
                            fos.write(buffer, 0, len);
                            float l = (float) (count * 100 / contentLength * 1.0);
                            if (l % (1024 * 1024 * 10) == 0) {
                                LogUtils.d(TAG, "downloadFile result: " + (l * 1.0 / contentLength) * 100 + ("%"));
                            }
                        }
                        fos.flush();
                        fos.close();
                        inputStream.close();
                    }
                }
            });
        } catch (Exception e) {
            LogUtils.e(TAG, "Call to download function failed. Failure Message: " + e.getMessage());
        }

    }

    public static void setUploadResponseDBParams(String status, String message) {
        DbManager.setDBParam("Device.X_Skyworth.UploadResponse.Status", status);
        DbManager.setDBParam("Device.X_Skyworth.UploadResponse.Message", message);
    }

    public static void uploadLogFile(String uploadUrl, String filePath, int fileCount) {
        try {
            File file = new File(filePath);
            if (!file.exists() || !file.isFile()) {
                String message = "The file under the specified path was not found.";
                LogUtils.e(TAG, "uploadLogFile: " + message + " file path: " + filePath);
                setUploadResponseDBParams("Error", message);
                return;
            }
            long fileSize = file.length();
            LogUtils.d(TAG, "Start uploading files: " + filePath
                    + ", size: " + fileSize
                    + ", count: " + fileCount
                    + " to " + uploadUrl);
            if (uploadUrl.contains("https")) {
                HttpsUtils.uploadLogFile(uploadUrl, filePath, fileCount);
            } else {
                HttpUtils.uploadLogFile(uploadUrl, filePath, fileCount);
            }
        } catch (Exception e) {
            String message = "Failed to upload log files, " + e.getMessage();
            LogUtils.e(TAG, "uploadLogFile: " + message);
            setUploadResponseDBParams("Error", message);
        }
    }

    private void upload(String[] params) {
        if (params.length <= INDEX_PARAM_3) {
            LogUtils.e(TAG, "Parameter error in upload() function, params.len: " + params.length);
            return;
        }
        // 初始化Upload事件的结果
        setUploadResponseDBParams("", "");

        // 处理Upload事件
        String fileType = params[INDEX_PARAM_1];
        String delaySeconds = params[INDEX_PARAM_2];
        String uploadUrl = params[INDEX_PARAM_3];
        switch (fileType) {
            case SCREENSHOT_TYPE:
                handleScreenCapture(uploadUrl);
                break;
            case VIDEO_TYPE:
                handleVideoFile(uploadUrl, delaySeconds);
                break;
            case CONFIG_FILE_TYPE:
                String configFilePath = GlobalContext.getContext().getFilesDir().getPath() + "refresh.txt";
                getRefreshData(configFilePath);
                uploadLogFile(uploadUrl, configFilePath, 1);
                break;
            case LOG_FILE_TYPE:
                handleLogFile(uploadUrl);
                break;
            case APP_ICON_TYPE:
                uploadIconFile(uploadUrl);
                break;
            case BUG_REPORT_TYPE:
                handleBugReport(uploadUrl);
                break;
        }
    }

    private void handleScreenCapture(String uploadUrl) {
        if (!Device.isScreenOn()) {
            String message = "The user screen is not enabled and cannot take screen captures.";
            LogUtils.e(TAG, "handleScreenCapture: " + message);
            setUploadResponseDBParams("Error", message);
            return;
        }
        if (isVideoPlaying()) {
            String message = "The user is playing a video and screen capture is prohibited.";
            LogUtils.e(TAG, "handleScreenCapture: " + message);
            setUploadResponseDBParams("Error", message);
            return;
        }
        ScreenShot2.getInstance().takeAndUpload(uploadUrl);
    }

    private void handleVideoFile(String uploadUrl, String delaySeconds) {
        if (!Device.isScreenOn()) {
            String message = "The user screen is not enabled and cannot record video.";
            LogUtils.e(TAG, "handleVideoFile: " + message);
            setUploadResponseDBParams("Error", message);
            return;
        }
        if (isVideoPlaying()) {
            String message = "The user is playing a video and video recording is prohibited.";
            LogUtils.e(TAG, "handleVideoFile: " + message);
            setUploadResponseDBParams("Error", message);
            return;
        }

        Intent intent = new Intent(GlobalContext.getContext(), ScreenRecordActivity.class);
        intent.putExtra(ScreenRecordActivity.UPLOAD_URL, uploadUrl);
        intent.putExtra(ScreenRecordActivity.DELAY_SECONDS, Integer.parseInt(delaySeconds));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        GlobalContext.getContext().startActivity(intent);
        //同步
        synchronized (ScreenRecordService.SYNC_OBJ) {
            try {
                //超时退出，至少要等待：要录制的时间 + 用户同意录屏的时间 + 缓冲时间。
                ScreenRecordService.SYNC_OBJ.wait(
                        Integer.parseInt(delaySeconds) * 1000L
                                + ScreenRecordActivity.WAITING_USER_AGREE_TIME_MS
                                + 5000);
            } catch (InterruptedException e) {
                LogUtils.e(TAG, "Call to ScreenRecordService function failed. " + e.getMessage());
            }
        }
    }

    // 检查是否有视频在播放
    public static boolean isVideoPlaying() {
        // 方法一: 在一般情况下是可以准确判断的，但是在一些特殊情况比如网络环境不好时，视频正在缓冲，导致isMusicActive()返回false
        AudioManager am = (AudioManager) GlobalContext.getContext().getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            return am.isMusicActive();
        }
        return false;
        // 方式二: 通过"dumpsys power"查询 Wake Lock 状态
//        Process process = null;
//        BufferedReader bufferedReader = null;
//
//        try {
//            process = Runtime.getRuntime().exec("dumpsys power");
//            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            String line;
//            int wakeLockSize = 0;
//            while ((line = bufferedReader.readLine()) != null) {
//                if (line.contains("Wake Locks:")) {
//                    String[] wakeLock = line.split("=");
//                    if (wakeLock.length < 2) {
//                        LogUtils.e(TAG, "Context parsing exception. context: " + line);
//                        return false;
//                    }
//                    wakeLockSize = Integer.parseInt(wakeLock[1]);
//                    LogUtils.d(TAG, "Wake Locks: size = " + wakeLockSize);
//                    if (wakeLockSize > 1) {
//                        // 当前有系统锁
//                        return true;
//                    }
//                } else if (line.contains("PARTIAL_WAKE_LOCK") && wakeLockSize > 0) {
//                    // MewTv(com.sdt.tv)应用会固定占用导致Size恒为1 (莫名其妙..)
//                    if (line.contains("com.sdt.tv")) {
//                        LogUtils.d(TAG, "Ignore the impact of 'com.sdt.tv'");
//                        return false;
//                    }
//                } else if (line.contains("Suspend Blockers:")) {
//                    // 结束对于"Wake Locks:"的解析
//                    if (wakeLockSize > 0) {
//                        // 当前有系统锁
//                        return true;
//                    }
//                }
//            }
//        } catch (Exception e) {
//            LogUtils.e(TAG, "isVideoPlaying exec 'dumpsys power' failed, " + e.getMessage());
//        } finally {
//            if (process != null) {
//                process.destroy();
//            }
//            if (bufferedReader != null) {
//                try {
//                    bufferedReader.close();
//                } catch (IOException e) {
//                    LogUtils.e(TAG, "bufferedReader close call failed, " + e.getMessage());
//                }
//            }
//        }
//        return false;
    }

    private boolean isFileNeedToBeUploaded(String fileName, String startTime, String endTime) {
        if (fileName == null || startTime == null || endTime == null) {
            return false;
        }

        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(fileName);
        String fileTime = "0";

        if (matcher.find()) {
            fileTime = matcher.group();
        } else {
            return false;
        }

        return Long.parseLong(fileTime) > Long.parseLong(startTime)
                && Long.parseLong(fileTime) <= Long.parseLong(endTime);
    }

    private void handleLogFile(String uploadUrl) {
        if (TextUtils.isEmpty(uploadUrl)) {
            String message = "The URL parameter is empty.";
            LogUtils.e(TAG, "handleLogFile: " + message);
            setUploadResponseDBParams("Error", message);
            return;
        }
        File folder = new File(LOG_SOURCE_DIR_PATH);
        if (!folder.exists() || !folder.isDirectory()) {
            String message = "Unable to find the folder for storing logs.";
            LogUtils.e(TAG, "handleLogFile: " + message);
            setUploadResponseDBParams("Error", message);
            return;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            String message = "This abstract pathname does not denote the directory.";
            LogUtils.e(TAG, "handleLogFile: " + message);
            setUploadResponseDBParams("Error", message);
            return;
        }

        // 分割号和同事协商后定义为%%%，服务端下发数据内容<URL>https://xxx/xxx%%%开始时间戳%%%结束时间戳</URL>
        String[] keywords = uploadUrl.split("%%%");
        String filterUrl = "";
        String filterStartTime = "";
        String filterEndTime = "";
        if (keywords.length > 0) {
            filterUrl = keywords[0];
            if (keywords.length > 2) {
                filterStartTime = keywords[1];
                filterEndTime = keywords[2];
            }
            LogUtils.d(TAG, "The filtering condition for uploading logs is: " + filterStartTime
                    + " ~ " + filterEndTime + " > filterUrl: " + filterUrl);
        }

        ArrayList<String> logFiles = new ArrayList<>();
        if (TextUtils.isEmpty(filterUrl)) {
            String message = "The URL parameter is empty.";
            LogUtils.e(TAG, "handleLogFile: " + message);
            setUploadResponseDBParams("Error", message);
            return;
        } else if (filterStartTime.isEmpty() || filterEndTime.isEmpty()) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    if (fileName.startsWith(RAW_LOG_FILE)) {
                        logFiles.add(file.getAbsolutePath());
                    }
                }
            }
        } else {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    if (fileName.contains("logcat_tr369_")
                            && fileName.contains(".txt")
                            && isFileNeedToBeUploaded(fileName, filterStartTime, filterEndTime)) {
                        logFiles.add(file.getAbsolutePath());
                    }
                }
            }
        }

        int fileCounts = logFiles.size();
        if (fileCounts > 0) {
            for (String logFile : logFiles) {
                LogUtils.d(TAG, "About to upload file: " + logFile);
                uploadLogFile(filterUrl, logFile, fileCounts);
                fileCounts--;
            }
        } else {
            String message = "No such file or directory.";
            LogUtils.e(TAG, "handleLogFile: " + message);
            setUploadResponseDBParams("Error", message);
        }

    }

    public void uploadIconFile(String uploadUrl) {
        if (TextUtils.isEmpty(uploadUrl)) {
            String message = "The upload URL is empty";
            LogUtils.e(TAG, "uploadIconFile: " + message);
            setUploadResponseDBParams("Error", message);
            return;
        }
        String regex = "appIcon/(.*?)/";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(uploadUrl);
        if (!matcher.find() || matcher.group(1) == null) {
            String message = "The upload URL format is incorrect and the specified string cannot be found";
            LogUtils.e(TAG, "uploadIconFile: " + message);
            setUploadResponseDBParams("Error", message);
            return;
        }
        String packageName = matcher.group(1).replace("-", ".");
        LogUtils.d(TAG, "uploadIconFile packageName: " + packageName);

        PackageManager packageManager = GlobalContext.getContext().getPackageManager();
        List<PackageInfo> packlist = packageManager.getInstalledPackages(PackageManager.GET_ACTIVITIES);
        for (PackageInfo packageInfo : packlist) {
            if (packageInfo.packageName.equals(packageName)) {
                Drawable drawable = packageInfo.applicationInfo.loadIcon(packageManager);
                saveIcon(drawable, packageInfo.applicationInfo.name);
                String iconPath = GlobalContext.getContext().getFilesDir() + "/" + packageInfo.applicationInfo.name + ".png";
                uploadAppIcon(uploadUrl, iconPath, new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        String message = "Failed to upload icon file, " + e.getMessage();
                        LogUtils.e(TAG, "uploadIconFile: " + message);
                        setUploadResponseDBParams("Error", message);
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        LogUtils.d(TAG, "uploadIconFile onResponse: " + response.protocol()
                                + ", code: " + response.code());
                        if (response.code() == 200) {
                            File file = new File(iconPath);
                            if (file.exists()) {
                                LogUtils.d(TAG, "uploadIconFile: Wait to delete file: " + iconPath);
                                file.delete();
                            }
                            String message = "Icon file uploaded successfully";
                            LogUtils.e(TAG, "uploadIconFile: " + message + " file path: " + iconPath);
                            setUploadResponseDBParams("Complete", message);
                            return;
                        }
                        String message = "Failed to upload icon file, " + response.protocol()
                                + " " + response.code() + " " + response.message();
                        LogUtils.e(TAG, "uploadIconFile: " + message);
                        setUploadResponseDBParams("Error", message);
                    }
                });
            }
        }
    }

    public void saveIcon(Drawable icon, String appName) {
        String dataPath = GlobalContext.getContext().getFilesDir() + "/";
        File file = new File(dataPath + appName + ".png");
        if (!file.exists()) {
            try {
                file.createNewFile();
                Bitmap bitmap = drawableToBitmap(icon);
                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                //上传app图标
            } catch (IOException e) {
                LogUtils.e(TAG, "Call to saveIcon function failed. " + e.getMessage());
            }
        }
    }

    public Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap = Bitmap.createBitmap(
                drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565);
        Canvas canvas = new Canvas(bitmap);
        // canvas.setBitmap(bitmap);
        drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    private void uploadAppIcon(String uploadUrl, String filePath, Callback callback) {
        if (uploadUrl.startsWith("https")) {
            HttpsUtils.uploadFile(uploadUrl, filePath, true, callback);
        } else if (uploadUrl.startsWith("http")) {
            HttpUtils.uploadFile(uploadUrl, filePath, true, callback);
        }
    }

    private void handleBugReport(String uploadUrl) {
        File bugreportsDir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
                BUGREPORT_DIR);
        if (!bugreportsDir.exists()) {
            LogUtils.i(TAG, "Creating directory " + bugreportsDir
                    + " to store bugreports and screenshots");
            if (!bugreportsDir.mkdir()) {
                LogUtils.e(TAG, "Could not create directory " + bugreportsDir);
                return;
            }
        }
        if (isBugReportRunning) {
            String message = "BugReport function is running";
            LogUtils.e(TAG, "handleBugReport: " + message);
            setUploadResponseDBParams("Error", message);
            return;
        }
        DbManager.setDBParam("Device.X_Skyworth.BugReport.Url", uploadUrl);

        Intent triggerShellBugreport = new Intent();
        triggerShellBugreport.setAction(INTENT_BUGREPORT_REQUESTED);
        triggerShellBugreport.setPackage(SHELL_APP_PACKAGE);
        triggerShellBugreport.putExtra(EXTRA_BUGREPORT_TYPE, "bugreportfull");
        triggerShellBugreport.putExtra(EXTRA_TMS_BUGREPORT_DIR, bugreportsDir.getPath());
        triggerShellBugreport.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        triggerShellBugreport.addFlags(Intent.FLAG_RECEIVER_INCLUDE_BACKGROUND);
        GlobalContext.getContext().sendBroadcast(triggerShellBugreport);
        isBugReportRunning = true;

        String message = "Bug Report task execution begins";
        LogUtils.d(TAG, "handleBugReport: " + message + ", file path: " + bugreportsDir.getPath());
        setUploadResponseDBParams("Complete", message);
    }

    private void factoryReset() {
        LogUtils.d(TAG, "FactoryReset called.");
        Intent resetIntent = new Intent("android.intent.action.MASTER_CLEAR"/*Intent.ACTION_FACTORY_RESET*/);
        resetIntent.setPackage("android");
        resetIntent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        resetIntent.putExtra("resason"/*Intent.EXTRA_REASON*/, "RemoteFactoryReset");
        GlobalContext.getContext().sendBroadcast(resetIntent);
    }

    /**
     * ping -c %d -s %d -t %d %s
     * 示例：ping -c 5 -s 1024 -t 64 www.baidu.com
     */
    private void ping(String[] params) {
        // 先初始化Output节点
        DbManager.setDBParam("Device.IP.Diagnostics.IPPing.IPAddressUsed",
                NetworkUtils.getIpv4Address(GlobalContext.getContext()));
        DbManager.setDBParam("Device.IP.Diagnostics.IPPing.SuccessCount", "0");
        DbManager.setDBParam("Device.IP.Diagnostics.IPPing.FailureCount", "0");
        DbManager.setDBParam("Device.IP.Diagnostics.IPPing.AverageResponseTime", "0");
        DbManager.setDBParam("Device.IP.Diagnostics.IPPing.MinimumResponseTime", "0");
        DbManager.setDBParam("Device.IP.Diagnostics.IPPing.MaximumResponseTime", "0");
        DbManager.setDBParam("Device.IP.Diagnostics.IPPing.AverageResponseTimeDetailed", "0");
        DbManager.setDBParam("Device.IP.Diagnostics.IPPing.MinimumResponseTimeDetailed", "0");
        DbManager.setDBParam("Device.IP.Diagnostics.IPPing.MaximumResponseTimeDetailed", "0");

        if (params.length <= INDEX_PARAM_4) {
            LogUtils.e(TAG, "Parameter error in ping() function, params.len: " + params.length);
            DbManager.setDBParam("Device.IP.Diagnostics.IPPing.Status", "Error_Internal");
            return;
        }
        String addr = params[INDEX_PARAM_1];
        String size = params[INDEX_PARAM_2];
        String count = params[INDEX_PARAM_3];
        String timeout_ms = params[INDEX_PARAM_4];

        StringBuilder cmd = new StringBuilder("/system/bin/");
        cmd.append("ping -c ").append(count)
                .append(" -s ").append(size)
                .append(" -t ").append(Integer.parseInt(timeout_ms) / 1000)    // 毫秒转换为秒
                .append(" ").append(addr);

        LogUtils.d(TAG, "tr369_ping cmd: " + cmd);

        ShellUtils.CommandResult commandResult = ShellUtils.execCommand(cmd.toString(), false);
        LogUtils.d(TAG, "tr369_ping commandResult: " + commandResult);

        String result = (commandResult.result == 0 ? commandResult.successMsg : commandResult.errorMsg);
        LogUtils.d(TAG, "tr369_ping result: " + result);
        parsePingResult(result);
    }

    private void parsePingResult(String msg) {
        // 开始解析
        if (msg == null || msg.isEmpty()) {
            DbManager.setDBParam("Device.IP.Diagnostics.IPPing.Status", "Error");
        } else if (msg.contains("unknown host")) {
            DbManager.setDBParam("Device.IP.Diagnostics.IPPing.Status", "Error_CannotResolveHostName");
        } else if (msg.contains("Destination Host Unreachable") || msg.contains("Network is unreachable")) {
            DbManager.setDBParam("Device.IP.Diagnostics.IPPing.Status", "Error_NoRouteToHost");
        } else if (msg.contains("min/avg/max/mdev")) {
            // 期望的情况
            DbManager.setDBParam("Device.IP.Diagnostics.IPPing.Status", "Complete");
            String[] times = msg.split("min/avg/max/mdev = ")[1].split(" ms")[0].split("/");
            if (times.length < 4) {
                DbManager.setDBParam("Device.IP.Diagnostics.IPPing.Status", "Error");
                return;
            }

            float min_ms = Float.parseFloat(times[0]);
            float avg_ms = Float.parseFloat(times[1]);
            float max_ms = Float.parseFloat(times[2]);
            int min_us = (int) (min_ms * 1000);
            int avg_us = (int) (avg_ms * 1000);
            int max_us = (int) (max_ms * 1000);
            LogUtils.d(TAG, "tr369_ping parsePingResult min_ms: " + min_ms
                    + ", avg_ms: " + avg_ms
                    + ", max_ms: " + max_ms
                    + ", min_us: " + min_us
                    + ", avg_us: " + avg_us
                    + ", max_us: " + max_us);

            DbManager.setDBParam("Device.IP.Diagnostics.IPPing.AverageResponseTime", String.valueOf((int) avg_ms));
            DbManager.setDBParam("Device.IP.Diagnostics.IPPing.MinimumResponseTime", String.valueOf((int) min_ms));
            DbManager.setDBParam("Device.IP.Diagnostics.IPPing.MaximumResponseTime", String.valueOf((int) max_ms));
            DbManager.setDBParam("Device.IP.Diagnostics.IPPing.AverageResponseTimeDetailed", String.valueOf(avg_us));
            DbManager.setDBParam("Device.IP.Diagnostics.IPPing.MinimumResponseTimeDetailed", String.valueOf(min_us));
            DbManager.setDBParam("Device.IP.Diagnostics.IPPing.MaximumResponseTimeDetailed", String.valueOf(max_us));

            String[] counts_part1 = msg.split("ping statistics ---\n")[1].split(" packets transmitted, ");
            if (counts_part1.length > 1) {
                int totalCount = Integer.parseInt(counts_part1[0]);
                LogUtils.d(TAG, "tr369_ping parsePingResult totalCount: " + totalCount);
                String[] counts_part2 = counts_part1[1].split(" received,");
                if (counts_part2.length > 1) {
                    int successCount = Integer.parseInt(counts_part2[0]);
                    LogUtils.d(TAG, "tr369_ping parsePingResult successCount: " + successCount);
                    DbManager.setDBParam("Device.IP.Diagnostics.IPPing.SuccessCount", String.valueOf(successCount));
                    DbManager.setDBParam("Device.IP.Diagnostics.IPPing.FailureCount", String.valueOf(totalCount - successCount));
                }
            }
        } else {
            DbManager.setDBParam("Device.IP.Diagnostics.IPPing.Status", "Canceled");
        }
    }

    public void getRefreshData(String path) {
        String AUTHORITY = "com.skw.data.center";
        Uri sUri = Uri.parse("content://" + AUTHORITY + "/all");
        new Thread() {
            @Override
            public void run() {
                super.run();
                Cursor cursor = GlobalContext.getContext().getContentResolver().query(sUri, null, null,
                        null, null, null);
                File file = new File(path);
                try {
                    if (file.exists()) {
                        file.delete();
                    }
                    file.createNewFile();
                    FileOutputStream fileOutputStream = new FileOutputStream(file, true);
                    if (cursor != null) {
                        while (cursor.moveToNext()) {
                            try {
                                String line1 = cursor.getString(0) + "=" + cursor.getString(1) + "\n";
                                String line2 = cursor.getString(2) + "=" + cursor.getInt(3) + "\n";
                                fileOutputStream.write(line1.getBytes(StandardCharsets.UTF_8));
                                fileOutputStream.write(line2.getBytes(StandardCharsets.UTF_8));
                            } catch (IOException e) {
                                LogUtils.e(TAG, "getRefreshData Stream function call failed. " + e.getMessage());
                            }
                        }
                        fileOutputStream.close();
                        cursor.close();
                    }
                } catch (IOException e) {
                    LogUtils.e(TAG, "getRefreshData File function call failed. " + e.getMessage());
                }
            }
        }.start();
    }
}
