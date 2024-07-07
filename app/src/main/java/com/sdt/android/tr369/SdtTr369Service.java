package com.sdt.android.tr369;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemProperties;

import androidx.annotation.NonNull;

import com.sdt.android.tr369.Receiver.BluetoothMonitorReceiver;
import com.sdt.android.tr369.Receiver.BugreportReceiver;
import com.sdt.android.tr369.Receiver.ExternalAppUpgradeReceiver;
import com.sdt.android.tr369.Receiver.NetflixEsnReceiver;
import com.sdt.android.tr369.Receiver.PackageReceiver;
import com.sdt.android.tr369.Receiver.ShutdownReceiver;
import com.sdt.android.tr369.Receiver.StandbyModeReceiver;
import com.sdt.diagnose.Device.LanX;
import com.sdt.diagnose.Device.X_Skyworth.App.AppX;
import com.sdt.diagnose.Device.X_Skyworth.Bluetooth.BluetoothDeviceX;
import com.sdt.diagnose.Device.X_Skyworth.FTIMonitor;
import com.sdt.diagnose.Device.X_Skyworth.Log.bean.LogCmd;
import com.sdt.diagnose.Device.X_Skyworth.Log.bean.LogRepository;
import com.sdt.diagnose.Device.X_Skyworth.SystemDataStat;
import com.sdt.diagnose.Tr369PathInvoke;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.database.DbManager;
import com.sdt.opentr369.OpenTR369Native;

import java.io.File;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Arrays;

public class SdtTr369Service extends Service {
    private static final String TAG = "SdtTr369Service";
    private static final String CHANNEL_ID = "SdtTr369ServiceChannelId";
    private static final String CHANNEL_NAME = "SdtTr369ServiceChannelName";
    private SdtTr369Receiver mSdtTr369Receiver = null;
    private PackageReceiver mPackageReceiver = null;
    private BluetoothMonitorReceiver mBluetoothMonitorReceiver = null;
    private StandbyModeReceiver mStandbyModeReceiver = null;
    private ShutdownReceiver mShutdownReceiver = null;
    private ExternalAppUpgradeReceiver mExternalAppUpgradeReceiver = null;
    private NetflixEsnReceiver mNetflixEsnReceiver = null;
    private BugreportReceiver mBugreportReceiver = null;
    private HandlerThread mHandlerThread = null;
    private Handler mHandler = null;
    public static final int MSG_START_TR369_SERVICE = 3300;
    public static final int MSG_STOP_TR369_SERVICE = 3301;

    public void handleServiceMessage(@NonNull Message msg) {
        switch (msg.what) {
            case MSG_START_TR369_SERVICE:
                mHandler.removeMessages(MSG_START_TR369_SERVICE);
                LogUtils.d(TAG, "MSG_START_TR369_SERVICE");
                startTr369Service();
                break;
            case MSG_STOP_TR369_SERVICE:
                mHandler.removeMessages(MSG_STOP_TR369_SERVICE);
                LogUtils.d(TAG, "MSG_STOP_TR369_SERVICE");
                break;
            default:
                break;
        }
    }

    @Override
    public IBinder onBind(Intent arg0) {
        LogUtils.d(TAG, "onBind");
        return null;
    }

    @Override
    public void onCreate() {
        LogUtils.d(TAG, "onCreate");
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
        Notification notification = new Notification.Builder(getApplicationContext(), CHANNEL_ID).build();
        startForeground(1, notification);

        GlobalContext.setContext(getApplicationContext());
    }

    @SuppressLint("WrongConstant")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d(TAG, "onStartCommand");
        initTr369Service();
        return super.onStartCommand(intent, START_STICKY, startId);
    }

    private void initTr369Service() {
        LogUtils.d(TAG, "initTr369Service start");
        mHandlerThread = new HandlerThread("tr369_server");
        // 先启动，再初始化handler
        mHandlerThread.start();
        if (mHandler == null) {
            mHandler = new Handler(mHandlerThread.getLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);
                    handleServiceMessage(msg);
                }
            };
        }
        // 设置回调函数
        OpenTR369Native.SetListener(mListener);
        // 注册监听器以启动TR369协议
        registerSdtTr369Receiver();

        // 初始化FTI停留时间监控程序
        new FTIMonitor(getContentResolver());
    }

    private void registerSdtTr369Receiver() {
        LogUtils.d(TAG, "registerSdtTr369Receiver");
        mSdtTr369Receiver = new SdtTr369Receiver(getApplicationContext());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mSdtTr369Receiver, intentFilter);
    }

    private void registerPackageReceiver() {
        LogUtils.d(TAG, "registerPackageReceiver");
        mPackageReceiver = new PackageReceiver();
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_MEDIA_MOUNTED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        intentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        intentFilter.addDataScheme("package");
        registerReceiver(mPackageReceiver, intentFilter);
    }

    private void registerBluetoothMonitorReceiver() {
        LogUtils.d(TAG, "registerBluetoothMonitorReceiver");
        if (mBluetoothMonitorReceiver == null) {
            mBluetoothMonitorReceiver = new BluetoothMonitorReceiver();
        }
        //注册监听
        IntentFilter stateChangeFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        IntentFilter connectedFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter disConnectedFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        IntentFilter nameChangeFilter = new IntentFilter(BluetoothDevice.ACTION_ALIAS_CHANGED);

        registerReceiver(mBluetoothMonitorReceiver, stateChangeFilter);
        registerReceiver(mBluetoothMonitorReceiver, connectedFilter);
        registerReceiver(mBluetoothMonitorReceiver, disConnectedFilter);
        registerReceiver(mBluetoothMonitorReceiver, nameChangeFilter);
    }

    private void registerStandbyReceiver() {
        mStandbyModeReceiver = new StandbyModeReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.SCREEN_ON");
        intentFilter.addAction("android.intent.action.SCREEN_OFF");
        registerReceiver(mStandbyModeReceiver, intentFilter);
    }

    private void registerShutdownReceiver() {
        if (mShutdownReceiver == null) {
            mShutdownReceiver = new ShutdownReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("android.intent.action.ACTION_SHUTDOWN");
        registerReceiver(mShutdownReceiver, intentFilter);
    }

    private void registerExternalAppUpdateReceiver() {
        if (mExternalAppUpgradeReceiver == null) {
            mExternalAppUpgradeReceiver = new ExternalAppUpgradeReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ExternalAppUpgradeReceiver.ACTION_BOOT_DIAGNOSE_APP_DOWNLOAD);
        intentFilter.addAction(ExternalAppUpgradeReceiver.ACTION_BOOT_DIAGNOSE_APP_UPGRADE);
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mExternalAppUpgradeReceiver, intentFilter);
    }

    private void registerNetflixEsnReceiver() {
        if (mNetflixEsnReceiver == null) {
            mNetflixEsnReceiver = new NetflixEsnReceiver();
        }
        IntentFilter intentFilter = new IntentFilter("com.netflix.ninja.intent.action.ESN_RESPONSE");
        registerReceiver(mNetflixEsnReceiver, intentFilter, "com.netflix.ninja.permission.ESN", null);
    }

    private void registerBugreportReceiver() {
        if (mBugreportReceiver == null) {
            mBugreportReceiver = new BugreportReceiver();
        }
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BugreportReceiver.ACTION_TMS_BUGREPORT_FINISH);
        intentFilter.addAction(BugreportReceiver.ACTION_TMS_BUGREPORT_ERROR);
        registerReceiver(mBugreportReceiver, intentFilter);
    }

    @Override
    public void onDestroy() {
        LogUtils.d(TAG, "onDestroy");
        if (mSdtTr369Receiver != null) unregisterReceiver(mSdtTr369Receiver);
        if (mBluetoothMonitorReceiver != null) unregisterReceiver(mBluetoothMonitorReceiver);
        if (mPackageReceiver != null) unregisterReceiver(mPackageReceiver);
        if (mStandbyModeReceiver != null) unregisterReceiver(mStandbyModeReceiver);
        if (mShutdownReceiver != null) unregisterReceiver(mShutdownReceiver);
        if (mExternalAppUpgradeReceiver != null) unregisterReceiver(mExternalAppUpgradeReceiver);
        if (mNetflixEsnReceiver != null) unregisterReceiver(mNetflixEsnReceiver);
        if (mBugreportReceiver != null) unregisterReceiver(mBugreportReceiver);

        if (mHandler != null) {
            mHandler.sendEmptyMessage(MSG_STOP_TR369_SERVICE);
            mHandler = null;
        }
        if (mHandlerThread != null) {
            mHandlerThread.quit();
            mHandlerThread = null;
        }
        super.onDestroy();
    }

    private void startTr369Service() {
        // 注册监听器
        registerPackageReceiver();
        registerBluetoothMonitorReceiver();
        registerStandbyReceiver();
        registerShutdownReceiver();
        registerExternalAppUpdateReceiver();
        registerNetflixEsnReceiver();
        registerBugreportReceiver();

        // 开机同步后台logcat状态
        LogRepository.getLogRepository().startCommand(LogCmd.CatchLog, "sky_log_tr369_logcat.sh");
        // 初始化tcpdump参数
        SystemProperties.set("persist.sys.skyworth.tcpdump", "0");
        SystemProperties.set("persist.sys.skyworth.tcpdump.args", " ");
        // 在启动时，检测抓包文件是否存在，如果存在就删除文件
        File file = new File("/data/tcpdump/test1.pcap");
        if (file.exists()) {
            file.delete();
        }
        // 开机同步STB Lock状态
        if (DbManager.getDBParam("Device.X_Skyworth.Lock.Enable").equals("1")) {
            Tr369PathInvoke.getInstance().setString("Device.X_Skyworth.Lock.Enable", "1");
        }
        // 重新上传APP安装结果
        if (DbManager.getDBParam("Device.X_Skyworth.UpgradeResponse.Enable").equals("1")) {
            ExternalAppUpgradeReceiver.retryReportResponse();
        }
        // 初始化系统数据采集程序
        new SystemDataStat(this);
        // 初始化LanX用于检测静态IP是否能访问互联网
        new LanX();

        // 更新协议中储存的APP列表
        AppX.updateAppList();
        // 更新协议中储存的蓝牙列表
        BluetoothDeviceX.updateBluetoothList();

        // 初始化时获取Netflix ESN
        NetflixEsnReceiver.notifyNetflix();
    }

    private final OpenTR369Native.IOpenTr369Listener mListener = new OpenTR369Native.IOpenTr369Listener() {
        @Override
        public String openTR369GetAttr(int what, String path) {
            String ret = Tr369PathInvoke.getInstance().getAttribute(what, path);
            if (ret == null) {
                ret = DbManager.getDBParam(path);
            }
            return ret;
        }

        @Override
        public boolean openTR369SetAttr(int what, String path, String value) {
            return Tr369PathInvoke.getInstance().setAttribute(what, path, value);
        }

        @Override
        public void openTR369Start() {
            // 延迟一段时间以确保协议初始化完毕
            mHandler.sendEmptyMessageDelayed(MSG_START_TR369_SERVICE, 5000);
        }
    };

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (checkCallingOrSelfPermission(android.Manifest.permission.DUMP)
                != PackageManager.PERMISSION_GRANTED) {
            pw.println("Permission Denial: Can't dump ActivityManager from from pid = "
                    + Binder.getCallingPid() + ", uid = " + Binder.getCallingUid()
                    + " without permission " + android.Manifest.permission.DUMP);
            return;
        }
        printTr369Message(fd, pw, args);
    }

    private void printTr369Message(FileDescriptor fd, PrintWriter pw, String[] args) {
        if (args.length > 1) {
            String cmd = args[0];
            String path = args[1];
            LogUtils.d(TAG, "mSkParamDB dumpsys args: " + Arrays.toString(args));
            if ("dbget".equalsIgnoreCase(cmd)) {
                pw.println(formatString(path));
            } else if ("dbset".equalsIgnoreCase(cmd) && args.length > 2) {
                String value = args[2];
                boolean ret = mListener.openTR369SetAttr(0, path, value);
                if (!ret) {
                    ret = (DbManager.setDBParam(path, value) == 0);
                }
                if (ret) {
                    pw.println(formatString(path));
                } else {
                    pw.println(cmd + " execution failed!");
                }
            } else if (("dbdel").equalsIgnoreCase(cmd)) {
                if (DbManager.delMultiObject(path) == 0) {
                    pw.println(formatString(path + "NumberOfEntries"));
                } else {
                    pw.println(cmd + " execution failed!");
                }
            } else if ("show".equals(cmd)) {
                // [show] [database|datamodel]
//                pw.println(DbManager.showData(args[1]));
                DbManager.showData(path);
            }
        }
    }

    private String formatString(String paramKey) {
        return paramKey + " : [" + mListener.openTR369GetAttr(0, paramKey) + "]";
    }

}
