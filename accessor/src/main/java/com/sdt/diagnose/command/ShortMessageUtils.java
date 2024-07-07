package com.sdt.diagnose.command;

import static android.app.NotificationManager.IMPORTANCE_HIGH;
import static android.app.NotificationManager.IMPORTANCE_LOW;
import static android.app.PendingIntent.FLAG_IMMUTABLE;
import static android.app.PendingIntent.FLAG_UPDATE_CURRENT;
import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.app.AppOpsManager;
import android.app.Instrumentation;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.provider.Settings;
import android.view.KeyEvent;

import com.google.gson.Gson;
import com.sdt.accessor.R;
import com.sdt.diagnose.Tr369PathInvoke;
import com.sdt.diagnose.common.DeviceInfoUtils;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.bean.ShortMessageBean;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.common.net.HttpsUtils;
import com.sdt.diagnose.database.DbManager;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;

public class ShortMessageUtils {
    private static final String TAG = "ShortMessageUtils";
    public static final String KEY_TITLE = "title";
    public static final String KEY_CONTENT = "content";
    public static final String KEY_URL = "url";
    public static final String KEY_IMAGE_URL = "imageUrl";
    public static final String KEY_TYPE = "type";
    public static final String KEY_ID = "noticeId";
    public static final String SHOW_TIME = "showTime";
    public static final String KEY_POSITION = "position";
    private static final String CHANNEL_ID = "SdtTr369ShortMsgChannelId";
    private static final CharSequence CHANNEL_NAME = "SdtTr369ShortMsgChannelName";
    public static final String TYPE_TEXT = "01";
    public static final String TYPE_URL = "02";
    public static final String TYPE_IMAGE = "03";
    public static final String TYPE_NOTIFY = "04";
    public static final String TYPE_UPDATE = "05";
    private static final int NOTIFICATION_TR369_ID = 1133;
    private static final String ACTION_START_UPDATE_TASK = "com.skw.ota.update.internal.tr069";

    public static void handleShortMessage(String json) {
        LogUtils.d(TAG, "Get Json data: " + json);
        Gson gson = new Gson();
        ShortMessageBean shortMessage = gson.fromJson(json, ShortMessageBean.class);
        if (shortMessage != null) {
            LogUtils.d(TAG, "Get Launcher message: " + shortMessage);
            if (shortMessage.getType().equals(TYPE_NOTIFY)) {
                // type值为04时，此消息为Notifications通知
                notifyNotification(shortMessage.getTitle(), shortMessage.getContent());
                responseServer(shortMessage.getMessageId());
            } else if (shortMessage.getType().equals(TYPE_UPDATE)) {
                // type值为05时，此消息为通知OTA应用立即执行升级操作
                notifyOtaAppToUpdate();
            } else {
                Instrumentation inst = new Instrumentation();
                inst.sendKeyDownUpSync(KeyEvent.KEYCODE_WAKEUP); //唤醒屏幕
                Intent intent = new Intent(GlobalContext.getContext(), ShortMessageActivity.class);
                intent.setFlags(FLAG_ACTIVITY_NEW_TASK);
                Bundle bundle = new Bundle();
                bundle.putString(KEY_TITLE, shortMessage.getTitle());
                bundle.putString(KEY_CONTENT, shortMessage.getContent());
                bundle.putString(KEY_URL, shortMessage.getRichTextUrl());
                bundle.putString(KEY_IMAGE_URL, shortMessage.getImageUrl());
                bundle.putString(KEY_POSITION, shortMessage.getPosition());
                bundle.putString(KEY_TYPE, shortMessage.getType());
                bundle.putString(KEY_ID, shortMessage.getMessageId());
                bundle.putInt(SHOW_TIME, shortMessage.getShowTime());
                intent.putExtras(bundle);
                GlobalContext.getContext().startActivity(intent);
            }
        } else {
            LogUtils.e(TAG, "Failed to parse Json content.");
        }
    }

    private static void notifyOtaAppToUpdate() {
        Intent intent = new Intent();
        intent.setPackage("com.sdt.ota");
        intent.setAction(ACTION_START_UPDATE_TASK);
        GlobalContext.getContext().sendBroadcast(intent);
    }

    private static void notifyNotification(String title, String content) {
        NotificationManager manager =
                (NotificationManager) GlobalContext.getContext().getSystemService(NOTIFICATION_SERVICE);
//        // 禁止Notification在Launcher右上角弹出窗口
//        NotificationChannel channel =
//                new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, IMPORTANCE_LOW);
        NotificationChannel channel =
                new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, IMPORTANCE_HIGH);
        manager.createNotificationChannel(channel);

        Intent emptyIntent = new Intent();
        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        GlobalContext.getContext(),
                        0,
                        emptyIntent,
                        FLAG_UPDATE_CURRENT | FLAG_IMMUTABLE);

        Notification notification =
                new Notification.Builder(GlobalContext.getContext(), CHANNEL_ID)
                        .setContentText(content)
                        .setContentTitle(title)
                        .setSmallIcon(R.drawable.update_icon)
                        .setChannelId(CHANNEL_ID)
                        .extend((Notification.Extender) new Notification.TvExtender())
                        .setContentIntent(pendingIntent) // Set the empty pending intent
                        .setAutoCancel(true) // Automatically cancel the notification when it's clicked
//                        .setPriority(Notification.PRIORITY_LOW) // Set priority to low
                        .build();

        manager.notify(NOTIFICATION_TR369_ID, notification);
    }

    public static void responseServer(String id) {
        HashMap<String, String> params = new HashMap<>();
        params.put("deviceId", DeviceInfoUtils.getSerialNumber());
        params.put("messageId", id);
        String url = Tr369PathInvoke.getInstance().getString("Device.X_Skyworth.ManagementServer.Url");
        if (!url.isEmpty()) {
            HttpsUtils.noticeResponse(url + "/tr369/message/sendResult", params);
        } else {
            LogUtils.e(TAG, "ManagementServer URL is empty.");
        }
    }

    public static boolean checkFloatPermission(Context context) {
        AppOpsManager appOpsMgr = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if (appOpsMgr == null)
            return false;
        int mode = appOpsMgr.checkOpNoThrow("android:system_alert_window",
                Process.myUid(),
                context.getPackageName());
        return Settings.canDrawOverlays(context)
                || mode == AppOpsManager.MODE_ALLOWED
                || mode == AppOpsManager.MODE_IGNORED;
    }

    @SuppressWarnings("unchecked")
    public static void hookWebView() {
        int sdkInt = Build.VERSION.SDK_INT;
        try {
            Class<?> factoryClass = Class.forName("android.webkit.WebViewFactory");
            Field field = factoryClass.getDeclaredField("sProviderInstance");
            field.setAccessible(true);
            Object sProviderInstance = field.get(null);
            if (sProviderInstance != null) {
                LogUtils.i(TAG, "sProviderInstance isn't null");
                return;
            }

            Method getProviderClassMethod;
            if (sdkInt > 22) {
                getProviderClassMethod = factoryClass.getDeclaredMethod("getProviderClass");
            } else if (sdkInt == 22) {
                getProviderClassMethod = factoryClass.getDeclaredMethod("getFactoryClass");
            } else {
                LogUtils.i(TAG, "Don't need to Hook WebView");
                return;
            }
            getProviderClassMethod.setAccessible(true);
            Class<?> factoryProviderClass = (Class<?>) getProviderClassMethod.invoke(factoryClass);
            Class<?> delegateClass = Class.forName("android.webkit.WebViewDelegate");
            Constructor<?> delegateConstructor = delegateClass.getDeclaredConstructor();
            delegateConstructor.setAccessible(true);
            if (sdkInt < 26 && factoryProviderClass != null) {
                //低于Android O版本
                Constructor<?> providerConstructor = factoryProviderClass.getConstructor(delegateClass);
                providerConstructor.setAccessible(true);
                sProviderInstance = providerConstructor.newInstance(delegateConstructor.newInstance());
            } else {
                Field chromiumMethodName = factoryClass.getDeclaredField("CHROMIUM_WEBVIEW_FACTORY_METHOD");
                chromiumMethodName.setAccessible(true);
                String chromiumMethodNameStr = (String) chromiumMethodName.get(null);
                if (chromiumMethodNameStr == null) {
                    chromiumMethodNameStr = "create";
                }
                Method staticFactory = factoryProviderClass.getMethod(chromiumMethodNameStr, delegateClass);
                sProviderInstance = staticFactory.invoke(null, delegateConstructor.newInstance());
            }

            if (sProviderInstance != null) {
                field.set("sProviderInstance", sProviderInstance);
                LogUtils.i(TAG, "Hook success!");
            } else {
                LogUtils.i(TAG, "Hook failed!");
            }
        } catch (Throwable e) {
            LogUtils.e(TAG, "hookWebView error, " + e.getMessage());
        }
    }
}
