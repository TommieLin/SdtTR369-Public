package com.sdt.diagnose.Device.X_Skyworth;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;

import com.sdt.accessor.R;
import com.sdt.diagnose.Tr369PathInvoke;

import static com.sdt.diagnose.common.net.HttpsUtils.uploadAllowStatus;

public class BoxAllowActivity extends Activity {
    private static final int WAITING_FOR_USER_TIME_MS = 30 * 1000;
    private static final Handler mHandler = new Handler();
    private Button mBtnConfirm;
    private Button mBtnRefuse;

    private final Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            BoxControlBean.getInstance().setAllow(false);
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.box_control_dialog);
        initEvent();
    }

    private void initEvent() {
        mBtnConfirm = findViewById(R.id.btnConfirm);
        mBtnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //allow
                BoxControlBean.getInstance().setAllow(true);
                uploadAllowStatus(BoxControlBean.getInstance().getConfirmResultUrl(),
                        1,
                        "success",
                        BoxControlBean.getInstance().getTransactionId());
                Tr369PathInvoke.getInstance().setString("Device.X_Skyworth.RemoteControl.Enable", "1");
                finish();
            }
        });
        mBtnRefuse = findViewById(R.id.btnRefuse);
        mBtnRefuse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //refuse
                BoxControlBean.getInstance().setAllow(false);
                uploadAllowStatus(BoxControlBean.getInstance().getConfirmResultUrl(),
                        0,
                        "Failed because click reject",
                        BoxControlBean.getInstance().getTransactionId());
                finish();
            }
        });

//        new Thread(new MyCountTimer()).start();
        mHandler.postDelayed(mRunnable, WAITING_FOR_USER_TIME_MS);
    }
}
