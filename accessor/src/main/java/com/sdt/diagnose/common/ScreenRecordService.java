package com.sdt.diagnose.common;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaRecorder;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.sdt.accessor.R;
import com.sdt.diagnose.command.Event;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.common.net.HttpUtils;
import com.sdt.diagnose.common.net.HttpsUtils;
import com.sdt.diagnose.database.DbManager;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ScreenRecordService extends Service {
    private static final String TAG = "ScreenRecordService";
    private static final String CHANNEL_ID = "ScreenRecordServiceID";
    private static final String CHANNEL_NAME = "ScreenRecordServiceName";
    public static final String RESULT_CODE = "RESULT_CODE";
    public static final String DATA = "DATA";
    public static final String UPLOAD_URL = "UPLOAD_URL";
    public static final String DELAY_SECONDS = "DELAY_SECONDS";
    public static final Object SYNC_OBJ = "SYNC_OBJ";

    /**
     * 是否为标清视频
     */
    private boolean isVideoSd = true;
    private int mScreenWidth;
    private int mScreenHeight;
    private int mScreenDensity;

    private int mResultCode;
    private Intent mResultData;
    private String uploadUrl;
    private int delaySeconds;
    private String recordFilePath;

    private MediaProjection mMediaProjection;
    private MediaRecorder mMediaRecorder;
    private VirtualDisplay mVirtualDisplay;

    Timer mTimer = new Timer();
    SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    private static final String fileParentDirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).toString();

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setContentTitle("Front Desk")
                .setContentText("Recording screen content...")
                .setSmallIcon(R.mipmap.ic_launcher);  //设置小图标

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        manager.createNotificationChannel(channel);

        notificationBuilder.setChannelId(CHANNEL_ID);
        startForeground(1, notificationBuilder.build());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mResultCode = intent.getIntExtra(RESULT_CODE, 1);
        mResultData = intent.getParcelableExtra(DATA);
        uploadUrl = intent.getStringExtra(UPLOAD_URL);
        delaySeconds = intent.getIntExtra(DELAY_SECONDS, 0);
        getScreenBaseInfo();

        mMediaProjection = createMediaProjection();
        if (mMediaRecorder == null) {
            mMediaRecorder = createMediaRecorder();
        } else {
            mMediaRecorder.reset();
        }
        mVirtualDisplay = createVirtualDisplay(); // 必须在mediaRecorder.prepare() 之后调用，否则报错"fail to get surface"

        try {
            mMediaRecorder.start();
        } catch (Exception e) {
            LogUtils.e(TAG, "MediaRecorder start error, " + e.getMessage());
        }

        TimerTask timeoutTask = new TimerTask() {
            @Override
            public void run() {
                stopForeground(true);
                stopSelf();
                mTimer.cancel();
            }
        };
        mTimer.schedule(timeoutTask, delaySeconds * 1000L);

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        LogUtils.d(TAG, "onDestroy");
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }

        if (mMediaRecorder != null) {
            try {
                mMediaRecorder.setOnErrorListener(null);
                mMediaRecorder.setOnInfoListener(null);
                mMediaRecorder.setPreviewDisplay(null);
                mMediaRecorder.stop();
            } catch (Exception e) {
                LogUtils.e(TAG, "MediaRecorder stop error, " + e.getMessage());
            }
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }

        // 判断文件大小是否超出上传上限
        File file = new File(recordFilePath);
        if (!file.exists() || !file.isFile()) {
            return;
        }
        long fileSize = file.length();
        LogUtils.w(TAG, "Record video file size: " + fileSize);
        String maxUplaodFileSize = DbManager.getDBParam("Device.X_Skyworth.MaxScreenRecordFileSize");
        long maxFileSize = 10 * 1024 * 1024;
        if (maxUplaodFileSize.length() != 0 && Long.parseLong(maxUplaodFileSize) > 0) {
            maxFileSize = Long.parseLong(maxUplaodFileSize);
        }
        if (fileSize >= maxFileSize) {
            file.delete();
            String message = "The target file is too large and cannot be uploaded.";
            LogUtils.e(TAG, message + " File size: " + fileSize + " > Max Size: " + maxFileSize);
            Event.setUploadResponseDBParams("Error", message);
            return;
        }
        // 开启新线程上传文件
        UploadRunnable runnable = new UploadRunnable();
        runnable.uploadUrl = uploadUrl;
        runnable.recordFilePath = recordFilePath;
        Thread thread = new Thread(runnable);
        thread.start();
    }

    /**
     * 获取屏幕相关数据
     */
    private void getScreenBaseInfo() {
        WindowManager wm = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        mScreenWidth = wm.getDefaultDisplay().getWidth();
        mScreenHeight = wm.getDefaultDisplay().getHeight();
        DisplayMetrics dm = getResources().getDisplayMetrics();
        mScreenDensity = dm.densityDpi;
    }

    private MediaProjection createMediaProjection() {
        return ((MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE)).getMediaProjection(mResultCode, mResultData);
    }

    private MediaRecorder createMediaRecorder() {

        Date curDate = new Date(System.currentTimeMillis());
        String curTime = formatter.format(curDate);
        String videoQuality = "HD";
        if (isVideoSd) videoQuality = "SD";

        MediaRecorder mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);  //before setOutputFormat()
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);  //before setOutputFormat()
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        recordFilePath = String.format("%s/%s_%s.mp4", fileParentDirPath, videoQuality, curTime);
        mediaRecorder.setOutputFile(recordFilePath);
        mediaRecorder.setVideoSize(mScreenWidth, mScreenHeight);  //after setVideoSource(), setOutFormat()
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);  //after setOutputFormat()
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);  //after setOutputFormat()

        if (isVideoSd) {
            mediaRecorder.setVideoEncodingBitRate(mScreenWidth * mScreenHeight);
            mediaRecorder.setVideoFrameRate(30);
        } else {
            mediaRecorder.setVideoEncodingBitRate(5 * mScreenWidth * mScreenHeight);
            mediaRecorder.setVideoFrameRate(60); //after setVideoSource(), setOutFormat()
        }
        try {
            mediaRecorder.prepare();
        } catch (IllegalStateException | IOException e) {
            LogUtils.e(TAG, "MediaRecorder prepare error, " + e.getMessage());
        }

        return mediaRecorder;
    }

    private VirtualDisplay createVirtualDisplay() {
        return mMediaProjection.createVirtualDisplay(TAG, mScreenWidth, mScreenHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, mMediaRecorder.getSurface(), null, null);
    }

    static class UploadRunnable implements Runnable {
        public String uploadUrl;
        public String recordFilePath;

        @Override
        public void run() {
            if (uploadUrl.startsWith("https")) {
                HttpsUtils.uploadFile(uploadUrl, recordFilePath, false, new UploadFileCallback());
            } else if (uploadUrl.startsWith("http")) {
                HttpUtils.uploadFile(uploadUrl, recordFilePath, false, new UploadFileCallback());
            }
        }

        class UploadFileCallback implements Callback {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                File file = new File(recordFilePath);
                file.delete();
                String message = "Failed to upload video, " + e.getMessage();
                LogUtils.e(TAG, "UploadFileCallback: " + message);
                Event.setUploadResponseDBParams("Error", message);
                synchronized (SYNC_OBJ) {
                    SYNC_OBJ.notify();
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response)
                    throws IOException {
                if (response.code() == 200) {
                    File file = new File(recordFilePath);
                    file.delete();
                    String message = "Successfully uploaded video file.";
                    LogUtils.e(TAG, "UploadFileCallback: " + message
                            + " Protocol: " + response.protocol()
                            + ", Code: " + response.code());
                    Event.setUploadResponseDBParams("Complete", message);
                } else {
                    String message = "Failed to upload video file. " + response.protocol()
                            + " " + response.code() + " " + response.message();
                    LogUtils.e(TAG, "UploadFileCallback: " + message);
                    Event.setUploadResponseDBParams("Error", message);
                }
                synchronized (SYNC_OBJ) {
                    SYNC_OBJ.notify();
                }
            }
        }
    }
}