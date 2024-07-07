package com.sdt.diagnose.Device.X_Skyworth;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.annotation.Nullable;

import com.sdt.accessor.R;
import com.sdt.diagnose.common.GlobalContext;

public class WatcherAlertActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        android.view.WindowManager.LayoutParams p = getWindow().getAttributes();
        p.height = 1;
        p.width = 1;
        p.alpha = 0.0f;
        getWindow().setAttributes(p);

        AlertDialog.Builder builder = new AlertDialog.Builder(
                this, com.android.internal.R.style.Theme_Material_Dialog_Alert);
        builder.setTitle(
                        GlobalContext.getContext().getString(R.string.app_suspend_dialog_title))
                .setMessage(
                        GlobalContext.getContext().getString(R.string.app_suspend_dialog_message))
                .setPositiveButton(
                        GlobalContext.getContext().getString(R.string.ok),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // 关闭当前Activity
                                finish();
                            }
                        })
                .show();
    }
}
