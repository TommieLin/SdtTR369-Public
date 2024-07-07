package com.sdt.android.tr369.Receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.database.DbManager;

/**
 * @Author Outis
 * @Date 2023/09/25 11:37
 * @Version 1.0
 */
public class NetflixEsnReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String esn = intent.getStringExtra("ESNValue");
        DbManager.setDBParam("Device.X_Skyworth.Netflix.ESN", esn);
    }

    public static void notifyNetflix() {
        Intent esnQueryIntent = new Intent("com.netflix.ninja.intent.action.ESN");
        esnQueryIntent.setPackage("com.netflix.ninja");
        esnQueryIntent.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        GlobalContext.getContext().sendBroadcast(esnQueryIntent);
    }

}
