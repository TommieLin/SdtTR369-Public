package com.sdt.android.tr369;

import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;

import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.log.LogUtils;

/*
 * Main Activity class that loads {@link MainFragment}.
 */
public class MainActivity extends FragmentActivity {
    private final static String TAG = "MainActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        LogUtils.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        GlobalContext.setContext(getApplicationContext());

        startForegroundService(new Intent(getApplicationContext(), SdtTr369Service.class));
    }
}