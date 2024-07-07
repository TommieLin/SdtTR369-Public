package com.skyworth.scrrtcsrv;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;

import java.util.ArrayList;
import java.util.HashMap;

public class RemoteControlAPI {
    public static void start(Context context, HashMap<String, String> params) {
        Intent intent = new Intent(context, RtcActivity.class);
        intent.putExtra(context.getString(R.string.socketio_url)
                , params.get(context.getString(R.string.socketio_url)));
        intent.putExtra(context.getString(R.string.video_fps)
                , Integer.parseInt(params.get(context.getString(R.string.video_fps))));
        intent.putExtra(context.getString(R.string.video_size)
                , params.get(context.getString(R.string.video_size)));
        intent.putExtra(context.getString(R.string.dev_name)
                , params.get(context.getString(R.string.dev_name)));
        ArrayList<String> iceServices = new ArrayList<>();
        iceServices.add(params.get(context.getString(R.string.ice_servers)));
        intent.putExtra(context.getString(R.string.ice_servers), iceServices);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void stop(Context context) {
        Intent intent = new Intent(context, RtcService.class);
        context.stopService(intent);
    }

    public static boolean isRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        //although ActivityManager#getRunningServices has been deprecated from Android 8.0;
        //it will still return the caller's own services;
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (RtcService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
