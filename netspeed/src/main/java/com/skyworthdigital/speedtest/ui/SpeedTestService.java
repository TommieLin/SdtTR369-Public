package com.skyworthdigital.speedtest.ui;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

import com.skyworthdigital.speedtest.core.SpeedTest;
import com.skyworthdigital.speedtest.core.config.SpeedTestConfig;
import com.skyworthdigital.speedtest.core.config.TelemetryConfig;
import com.skyworthdigital.speedtest.core.serverSelector.TestPoint;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

/**
 * @Author Outis
 * @Date 2023/11/2 11:09
 * @Version 1.0
 */
public class SpeedTestService extends Service {
    private static final String TAG = "TR369 SpeedTestService";
    ArrayList<TestPoint> availableServers;
    public Callback mCallback;
    public ReadyCallback mReadyCallback;

    public class MyBinder extends Binder {
        public SpeedTestService getService() {
            return SpeedTestService.this;
        }
    }

    private static SpeedTest st = null;

    public double downloadSpeed = 0;
    public double uploadSpeed = 0;
    Handler handler;
    HandlerThread handlerThread;
    public MyBinder mMyBinder = new MyBinder();

    @Override
    public IBinder onBind(Intent intent) {
        return mMyBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        handlerThread = new HandlerThread("SpeedTest");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        handler.post(new Runnable() {
            @Override
            public void run() {
                pageInit();
            }
        });
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    private void pageInit() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                SpeedTestConfig config = null;
                TelemetryConfig telemetryConfig = null;
                TestPoint[] servers = null;
                try {
                    String c = readFileFromAssets("SpeedTestConfig.json");
                    JSONObject o = new JSONObject(c);
                    config = new SpeedTestConfig(o);
                    c = readFileFromAssets("TelemetryConfig.json");
                    o = new JSONObject(c);
                    telemetryConfig = new TelemetryConfig(o);
                    if (st != null) {
                        try {
                            st.abort();
                        } catch (Throwable e) {
                            Log.e(TAG, "pageInit error, 1: " + e.getMessage());
                        }
                    }
                    st = new SpeedTest(SpeedTestService.this);
                    st.setSpeedTestConfig(config);
                    st.setTelemetryConfig(telemetryConfig);
                    c = readFileFromAssets("ServerList.json");
                    if (c.startsWith("\"") || c.startsWith("'")) { //fetch server list from URL
                        if (!st.loadServerList(c.subSequence(1, c.length() - 1).toString())) {
                            throw new Exception("Failed to load server list");
                        }
                    } else {
                        //use provided server list
                        JSONArray a = new JSONArray(c);
                        if (a.length() == 0) throw new Exception("No test points");
                        ArrayList<TestPoint> s = new ArrayList<>();
                        s.add(new TestPoint(a.getJSONObject(0)));
                        servers = s.toArray(new TestPoint[0]);
                        st.addTestPoints(servers);
                    }
                    final String testOrder = config.getTestOrder();
                } catch (final Throwable e) {
                    Log.e(TAG, "pageInit error, 2: " + e.getMessage());
                    st = null;
                    return;
                }
                TestPoint[] testPoints = st.getTestPoints();
                Log.d(TAG, "testPoints length is " + testPoints.length + " and the firstOne is " + testPoints[0].toString());
                st.selectServer(new SpeedTest.ServerSelectedHandler() {
                    @Override
                    public void onServerSelected(final TestPoint server) {
                        pageServerSelect(server, st.getTestPoints());
                    }
                });
            }
        });
    }

    public void startNetSpeedTest() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                pageTest();
            }
        });
    }

    private void pageServerSelect(TestPoint selected, TestPoint[] servers) {
        availableServers = new ArrayList<>();
        for (TestPoint t : servers) {
            if (t.getPing() != -1) {
                availableServers.add(t);
            }
        }
        int index = availableServers.indexOf(selected);
        if (index >= 0) {
            st.setSelectedServer(selected);
        } else {
            //如果通过ping选择的服务器节点不在可用节点内则默认选用第一个可用节点
            if (availableServers.size() > 0) {
                st.setSelectedServer(availableServers.get(0));
            }
        }
        Log.d(TAG, "servers available servers size is " + availableServers.size());
        mReadyCallback.isReady();
    }

    private void pageTest() {
        st.start(new SpeedTest.SpeedTestHandler() {
            @Override
            public void onDownloadUpdate(final double dl, final double progress) {
                mCallback.setDownloadSpeed(String.format(Locale.ENGLISH, "%.2f", dl));
                if (dl > downloadSpeed) {
                    downloadSpeed = dl;
                }
            }

            @Override
            public void onUploadUpdate(final double ul, final double progress) {
                mCallback.setUploadSpeed(String.format(Locale.ENGLISH, "%.2f", ul));
                if (ul > uploadSpeed) {
                    uploadSpeed = ul;
                }
            }

            @Override
            public void onPingJitterUpdate(double ping, double jitter, double progress) {

            }

            @Override
            public void onIPInfoUpdate(String ipInfo) {

            }

            @Override
            public void onTestIDReceived(String id, String shareURL) {

            }

            @Override
            public void onEnd() {
                //保留两位小数
                mCallback.setResult(String.format(Locale.ENGLISH, "%.2f", downloadSpeed),
                        String.format(Locale.ENGLISH, "%.2f", uploadSpeed));
                handler.removeCallbacksAndMessages(null);
            }

            @Override
            public void onCriticalFailure(String err) {

            }
        });
    }

    private String readFileFromAssets(String name) throws Exception {
        BufferedReader b = new BufferedReader(new InputStreamReader(getAssets().open(name)));
        StringBuilder ret = new StringBuilder();
        try {
            for (; ; ) {
                String s = b.readLine();
                if (s == null) break;
                ret.append(s);
            }
        } catch (EOFException e) {
            Log.e(TAG, "readFileFromAssets error, " + e.getMessage());
        }
        return ret.toString();
    }

    public interface Callback {
        void setResult(String downloadSpeed, String uploadSpeed);

        void setDownloadSpeed(String downloadSpeed);

        void setUploadSpeed(String uploadSpeed);
    }

    public interface ReadyCallback {
        void isReady();
    }

    public void setCallback(Callback callback) {
        mCallback = callback;
    }

    public void setReadyCallback(ReadyCallback callback) {
        mReadyCallback = callback;
    }
}
