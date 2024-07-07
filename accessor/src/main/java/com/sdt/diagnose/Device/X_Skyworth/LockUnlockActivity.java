package com.sdt.diagnose.Device.X_Skyworth;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import android.view.KeyEvent;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader;
import com.bumptech.glide.load.model.GlideUrl;
import com.sdt.accessor.R;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.net.SSLHelper;
import com.sdt.diagnose.database.DbManager;

import java.io.InputStream;

/**
 * @Author Outis
 * @Date 2023/11/30 9:48
 * @Version 1.0
 */
public class LockUnlockActivity extends Activity {
    public static Activity instance = null;
    public ImageView iv_lockBk;
    public AlertDialog mUpdatingDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_lockunlock);
        Glide.get(this).getRegistry().replace(
                GlideUrl.class,
                InputStream.class,
                new OkHttpUrlLoader.Factory(SSLHelper.getNoCheckOkHttpClient()));
        iv_lockBk = findViewById(R.id.lockBg);

        String imageUrl = getIntent().getStringExtra("imageUrl");
        Glide.with(this)
                .load(imageUrl).error(R.drawable.lock_background_error) // 异常时候显示的图片
                .placeholder(R.drawable.lock_background_default) // 加载成功前显示的图片
                .fallback(R.drawable.lock_background_blank) // url为空的时候,显示的图片
                .into(iv_lockBk);
        showDialog();
    }

    @Override
    public void onStart() {
        super.onStart();
        DbManager.setDBParam("Device.X_Skyworth.Lock.Enable", "1");
    }

    @Override
    public void onStop() {
        super.onStop();
        DbManager.setDBParam("Device.X_Skyworth.Lock.Enable", "0");
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // 按center键唤出提示打开网络设置的弹窗
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
            showDialog();
        }
        return true;
    }

    private void showDialog() {
        String title = DbManager.getDBParam("Device.X_Skyworth.Lock.Dialog.Title");
        if (title.isEmpty())
            title = GlobalContext.getContext().getString(R.string.lock_alert_dialog_title);

        String message = DbManager.getDBParam("Device.X_Skyworth.Lock.Dialog.Message");
        if (message.isEmpty())
            message = GlobalContext.getContext().getString(R.string.lock_alert_dialog_message);

        String negBtnText = DbManager.getDBParam("Device.X_Skyworth.Lock.Dialog.NegativeButtonText");
        if (negBtnText.isEmpty())
            negBtnText = GlobalContext.getContext().getString(R.string.cancel);

        String posBtnText = DbManager.getDBParam("Device.X_Skyworth.Lock.Dialog.PositiveButtonText");
        if (posBtnText.isEmpty())
            posBtnText = GlobalContext.getContext().getString(R.string.ok);

        mUpdatingDialog = new AlertDialog.Builder(this, com.android.internal.R.style.Theme_Material_Dialog_Alert)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(posBtnText,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Intent intent = new Intent().setComponent(
                                        new ComponentName("com.android.tv.settings",
                                                "com.android.tv.settings.connectivity.NetworkActivity"));
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                GlobalContext.getContext().startActivity(intent);
                            }
                        })
                .setNegativeButton(negBtnText,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                                mUpdatingDialog = null;
                            }
                        })
                .create();
        mUpdatingDialog.show();
    }
}
