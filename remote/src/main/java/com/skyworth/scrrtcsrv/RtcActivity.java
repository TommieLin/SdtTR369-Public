package com.skyworth.scrrtcsrv;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

public class RtcActivity extends Activity {
    private static final String TAG = "TR369 RtcActivity";
    private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;
    private static final String[] MANDATORY_PERMISSIONS = {
            "android.permission.INTERNET"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        setContentView(R.layout.activity_remote_control_helper);

        Bundle bundle = getIntent().getExtras();
        String socketio_url = bundle.getString(getString(R.string.socketio_url));
        if (socketio_url == null) {
            Log.e(TAG, "no socketio server url provided");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }

        for (String permission : MANDATORY_PERMISSIONS) {
            if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "check permission " + permission + " failed");
                setResult(RESULT_CANCELED);
                finish();
                return;
            }
        }

        startScreenCapture();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult(" + requestCode + ", " + resultCode + ", " + data + ")");
        if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE) {
            return;
        }
        if (resultCode != RESULT_OK) {
            Log.e(TAG, "User didn't give permission to capture the screen");
            return;
        }

        Intent intent = new Intent(this, RtcService.class);
        intent.putExtras(data);
        intent.putExtras(getIntent());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        finish();
    }

    private void startScreenCapture() {
        MediaProjectionManager manager = (MediaProjectionManager) getApplication()
                .getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(),
                CAPTURE_PERMISSION_REQUEST_CODE);
    }
}