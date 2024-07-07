package com.sdt.diagnose.common;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import com.sdt.accessor.R;
import com.sdt.diagnose.common.log.LogUtils;

import java.util.Timer;
import java.util.TimerTask;

public class ScreenRecordActivity extends Activity {
    private static final String TAG = "ScreenRecordActivity";
    private static final int REQUEST_CODE = 0x01;
    public static final int WAITING_USER_AGREE_TIME_MS = 15 * 1000;
    public static final String UPLOAD_URL = "UPLOAD_URL";
    public static final String DELAY_SECONDS = "DELAY_SECONDS";
    String uploadUrl;
    int delaySeconds;
    Timer mTimer = new Timer();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        uploadUrl = getIntent().getStringExtra(UPLOAD_URL);
        delaySeconds = getIntent().getIntExtra(DELAY_SECONDS, 0);
        if (uploadUrl == null || delaySeconds == 0) {
            finish();
        } else {
            setContentView(R.layout.activity_record);
            android.view.WindowManager.LayoutParams p = getWindow().getAttributes();
            p.height = 1;
            p.width = 1;
            p.alpha = 0.0f;
            getWindow().setAttributes(p);

            Intent intent = ((MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE))
                    .createScreenCaptureIntent();
            startActivityForResult(intent, REQUEST_CODE);

            // 用户经过 WAITING_USER_AGREE_TIME_MS 时间后不操作，则关闭请求Activity
            TimerTask timeoutTask = new TimerTask() {
                @Override
                public void run() {
                    finishActivity(REQUEST_CODE);
                    LogUtils.e(TAG, "TimerTask: User reject this request!");
                    synchronized (ScreenRecordService.SYNC_OBJ) {
                        ScreenRecordService.SYNC_OBJ.notify();
                    }
                    mTimer.cancel();
                }
            };
            mTimer.schedule(timeoutTask, WAITING_USER_AGREE_TIME_MS);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        mTimer.cancel();
        if (requestCode == REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Intent service = new Intent(this, ScreenRecordService.class);
            service.putExtra(ScreenRecordService.RESULT_CODE, resultCode);
            service.putExtra(ScreenRecordService.DATA, data);
            service.putExtra(ScreenRecordService.UPLOAD_URL, uploadUrl);
            service.putExtra(ScreenRecordService.DELAY_SECONDS, delaySeconds);
            startForegroundService(service);
        } else {
            LogUtils.e(TAG, "onActivityResult: User reject this request!");
            synchronized (ScreenRecordService.SYNC_OBJ) {
                ScreenRecordService.SYNC_OBJ.notify();
            }
        }
        finish();
    }
}