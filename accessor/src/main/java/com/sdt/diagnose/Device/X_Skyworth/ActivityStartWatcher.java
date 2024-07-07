package com.sdt.diagnose.Device.X_Skyworth;

import android.app.IActivityController;
import android.app.IActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sdt.diagnose.common.log.LogUtils;

import java.util.ArrayList;
import java.util.List;

public class ActivityStartWatcher extends Service {
    private static final String TAG = "ActivityStartWatcher";
    private static final String CHANNEL_ID = "SdtTr369LockChannelId";
    private static final String CHANNEL_NAME = "SdtTr369LockChannelName";
    private static IActivityController mActivityController = null;
    private int mActivityLockType = 0;
    private ArrayList<String> mActivityLockList = new ArrayList<>();
    public static final int TYPE_UNDEFINED = 0;
    public static final int TYPE_WHITELIST = 1;
    public static final int TYPE_BLACKLIST = 2;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d(TAG, "onStartCommand...");

        createNotificationChannel();
        int type = intent.getIntExtra("lockType", TYPE_UNDEFINED);
        String list = intent.getStringExtra("lockList");

        if (!setActivityLockType(type) || !setActivityLockList(list)) {
            stopForegroundService();
            return Service.START_NOT_STICKY;
        }

        try {
            mActivityController = new ActivityListener();
            IActivityManager.Stub.asInterface(ServiceManager.checkService(Context.ACTIVITY_SERVICE))
                    .setActivityController(mActivityController, false);
        } catch (Exception e) {
            LogUtils.e(TAG, "setActivityController(activityListener) error, " + e.getMessage());
            stopForegroundService();
            return Service.START_NOT_STICKY;
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);

        Notification notification = new Notification.Builder(getApplicationContext(), CHANNEL_ID).build();
        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            IActivityManager.Stub.asInterface(ServiceManager.checkService(Context.ACTIVITY_SERVICE))
                    .setActivityController(null, false);
            mActivityController = null;
        } catch (Exception e) {
            LogUtils.e(TAG, "setActivityController(null) error, " + e.getMessage());
        }
    }

    private void stopForegroundService() {
        // 停止前台服务
        stopForeground(true);
        // 停止服务
        stopSelf();
    }

    private boolean setActivityLockType(int lockType) {
        if (lockType <= TYPE_UNDEFINED || lockType > TYPE_BLACKLIST) {
            return false;
        }
        mActivityLockType = lockType;
        return true;
    }

    private boolean setActivityLockList(String lockList) {
        try {
            Gson gson = new Gson();
            mActivityLockList = gson.fromJson(lockList, new TypeToken<List<String>>(){}.getType());
            if (mActivityLockList == null) {
                LogUtils.e(TAG, "The JSON data is empty");
                return false;
            }
            // 过滤空字符串或为null的元素
            mActivityLockList.removeIf(TextUtils::isEmpty);
            LogUtils.i(TAG, "lock list waiting for execution: " + mActivityLockList);
            if (mActivityLockList.isEmpty()) {
                LogUtils.e(TAG, "The content of packageNames is empty and no subsequent operations are required");
                return false;
            }
        }  catch (Exception e) {
            LogUtils.e(TAG, "JSON data parsing exception, " + e.getMessage());
            return false;
        }
        return true;
    }

    private void showWatcherAlertDialog() {
        Intent intent = new Intent(this, WatcherAlertActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private class ActivityListener extends IActivityController.Stub {
        public ActivityListener() {
            LogUtils.d(TAG, "ActivityListener start...");
        }

        @Override
        public boolean activityStarting(Intent intent, String pkg) throws RemoteException {
            String comp = intent.getComponent().toShortString();
            LogUtils.d(TAG, "activityStarting: " + comp + ", pkg: " + pkg);
            if (mActivityLockType == TYPE_WHITELIST && !mActivityLockList.contains(pkg)) {
                LogUtils.e(TAG, "STB Lock - Activity{" + pkg + "} is not on the whitelist");
                showWatcherAlertDialog();
                return false;
            } else if (mActivityLockType == TYPE_BLACKLIST && mActivityLockList.contains(pkg)) {
                LogUtils.e(TAG, "STB Lock - Activity{" + pkg + "} is on the blacklist");
                showWatcherAlertDialog();
                return false;
            }
            return true;
        }

        @Override
        public boolean activityResuming(String pkg) throws RemoteException {
            LogUtils.e(TAG, "activityResuming: " + pkg);
            return true;
        }

        @Override
        public boolean appCrashed(String processName, int pid, String shortMsg, String longMsg,
                                  long timeMillis, String stackTrace) throws RemoteException {
            LogUtils.e(TAG, "appCrashed: " + processName);
            return false;
        }

        @Override
        public int appEarlyNotResponding(String processName, int pid, String annotation)
                throws RemoteException {
            LogUtils.e(TAG, "appEarlyNotResponding: " + processName);
            return 0;
        }

        @Override
        public int appNotResponding(String processName, int pid, String processStats)
                throws RemoteException {
            LogUtils.e(TAG, "appNotResponding: " + processName);
            return 0;
        }

        @Override
        public int systemNotResponding(String msg) throws RemoteException {
            LogUtils.e(TAG, "systemNotResponding: " + msg);
            return 0;
        }
    }
}
