package com.skyworthdigital.speedtest.core;

import android.content.Context;
import android.util.Log;

import com.skyworthdigital.speedtest.core.config.SpeedTestConfig;
import com.skyworthdigital.speedtest.core.config.TelemetryConfig;
import com.skyworthdigital.speedtest.core.serverSelector.ServerSelector;
import com.skyworthdigital.speedtest.core.serverSelector.TestPoint;
import com.skyworthdigital.speedtest.core.worker.SpeedTestWorker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;


public class SpeedTest {
    private static final String TAG = "TR369 SpeedTest";
    private final ArrayList<TestPoint> servers = new ArrayList<>();
    private TestPoint selectedServer = null;
    private SpeedTestConfig config = new SpeedTestConfig();
    private TelemetryConfig telemetryConfig = new TelemetryConfig();
    //0=configs, 1=test points, 2=server selection, 3=ready, 4=testing, 5=finished
    private int state = 0;
    private final Object mutex = new Object();
    private String originalExtra = "";
    private final Context mContext;

    public SpeedTest(Context context) {
        mContext = context;
    }

    public void setSpeedTestConfig(SpeedTestConfig c) {
        synchronized (mutex) {
            if (state != 0) throw new IllegalStateException("Cannot change config at this moment");
            config = c.clone();
            String extra = config.getTelemetryExtra();
            if (extra != null && !extra.isEmpty()) originalExtra = extra;
        }
    }

    public void setTelemetryConfig(TelemetryConfig c) {
        synchronized (mutex) {
            if (state != 0) throw new IllegalStateException("Cannot change config at this moment");
            telemetryConfig = c.clone();
        }
    }

    public void addTestPoint(TestPoint t) {
        synchronized (mutex) {
            if (state == 0) state = 1;
            if (state > 1) throw new IllegalStateException("Cannot add test points at this moment");
            servers.add(t);
        }
    }

    public void addTestPoints(TestPoint[] s) {
        synchronized (mutex) {
            for (TestPoint t : s) addTestPoint(t);
        }
    }

    public void addTestPoint(JSONObject json) {
        synchronized (mutex) {
            addTestPoint(new TestPoint(json));
        }
    }

    public void addTestPoints(JSONArray json) {
        synchronized (mutex) {
            for (int i = 0; i < json.length(); i++) {
                try {
                    addTestPoint(json.getJSONObject(i));
                } catch (JSONException e) {
                    Log.e(TAG, "addTestPoints error, " + e.getMessage());
                }
            }
        }
    }

    private static class ServerListLoader {
        private static String read(String url) {
            try {
                URL u = new URL(url);
                InputStream in = u.openStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(u.openStream()));
                StringBuilder s = new StringBuilder();
                try {
                    for (; ; ) {
                        String r = br.readLine();
                        if (r == null) {
                            break;
                        } else {
                            s.append(r);
                        }
                    }
                } catch (Throwable e) {
                    Log.e(TAG, "ServerListLoader readLine error, " + e.getMessage());
                }
                br.close();
                in.close();
                return s.toString();
            } catch (Throwable e) {
                Log.e(TAG, "ServerListLoader read error, " + e.getMessage());
                return null;
            }
        }

        public static TestPoint[] loadServerList(String url) {
            try {
                String s = null;
                if (url.startsWith("//")) {
                    s = read("https:" + url);
                    if (s == null) s = read("http:" + url);
                } else {
                    s = read(url);
                }
                if (s == null) throw new Exception("Failed");
                JSONArray a = new JSONArray(s);
                ArrayList<TestPoint> ret = new ArrayList<>();
                for (int i = 0; i < a.length(); i++) {
                    ret.add(new TestPoint(a.getJSONObject(i)));
                }
                return ret.toArray(new TestPoint[0]);
            } catch (Throwable e) {
                Log.e(TAG, "loadServerList error, " + e.getMessage());
                return null;
            }
        }
    }

    public boolean loadServerList(String url) {
        synchronized (mutex) {
            if (state == 0) state = 1;
            if (state > 1) throw new IllegalStateException("Cannot add test points at this moment");
            TestPoint[] pts = ServerListLoader.loadServerList(url);
            if (pts != null) {
                addTestPoints(pts);
                return true;
            } else {
                return false;
            }
        }
    }

    public TestPoint[] getTestPoints() {
        synchronized (mutex) {
            return servers.toArray(new TestPoint[0]);
        }
    }

    private ServerSelector ss = null;

    public void selectServer(final ServerSelectedHandler callback) {
        synchronized (mutex) {
            if (state == 0) throw new IllegalStateException("No test points added");
            if (state == 2) throw new IllegalStateException("Server selection is in progress");
            if (state > 2) throw new IllegalStateException("Server already selected");
            state = 2;
            ss = new ServerSelector(getTestPoints(), config.getPingConnectTimeout()) {
                @Override
                public void onServerSelected(TestPoint server) {
                    selectedServer = server;
                    synchronized (mutex) {
                        if (server != null) {
                            state = 3;
                        } else {
                            state = 1;
                        }
                    }
                    callback.onServerSelected(server);
                }
            };
            ss.start();
        }
    }

    public void setSelectedServer(TestPoint t) {
        synchronized (mutex) {
            if (state == 2) throw new IllegalStateException("Server selection is in progress");
            if (t == null) throw new IllegalArgumentException("t is null");
            Log.d(TAG, "setSelectedServer in the final choice is " + t.toString());
            selectedServer = t;
            state = 3;
        }
    }

    private SpeedTestWorker st = null;

    public void start(final SpeedTestHandler callback) {
        synchronized (mutex) {
            if (state < 3) {
                Log.e(TAG, "Server hasn't been selected yet");
                return;
            }
            if (state == 4) {
                Log.e(TAG, "Test already running");
                return;
            }
            state = 4;
            try {
                JSONObject extra = new JSONObject();
                if (originalExtra != null && !originalExtra.isEmpty()) {
                    extra.put("extra", originalExtra);
                }
                extra.put("server", selectedServer.getName());
                config.setTelemetryExtra(extra.toString());
            } catch (Throwable e) {
                Log.e(TAG, "start error, " + e.getMessage());
            }
            st = new SpeedTestWorker(selectedServer, config, telemetryConfig, mContext) {
                @Override
                public void onDownloadUpdate(double dl, double progress) {
                    callback.onDownloadUpdate(dl, progress);
                }

                @Override
                public void onUploadUpdate(double ul, double progress) {
                    callback.onUploadUpdate(ul, progress);
                }

                @Override
                public void onPingJitterUpdate(double ping, double jitter, double progress) {
                    callback.onPingJitterUpdate(ping, jitter, progress);
                }

                @Override
                public void onIPInfoUpdate(String ipInfo) {
                    callback.onIPInfoUpdate(ipInfo);
                }

                @Override
                public void onTestIDReceived(String id) {
                    String shareURL = prepareShareURL(telemetryConfig);
                    if (shareURL != null) shareURL = String.format(shareURL, id);
                    callback.onTestIDReceived(id, shareURL);
                }

                @Override
                public void onEnd() {
                    synchronized (mutex) {
                        state = 5;
                    }
                    callback.onEnd();
                }

                @Override
                public void onCriticalFailure(String err) {
                    synchronized (mutex) {
                        state = 5;
                    }
                    callback.onCriticalFailure(err);
                }
            };
        }
    }

    private String prepareShareURL(TelemetryConfig c) {
        if (c == null) return null;
        String server = c.getServer(), shareURL = c.getShareURL();
        if (server == null || server.isEmpty() || shareURL == null || shareURL.isEmpty()) {
            return null;
        }
        if (!server.endsWith("/")) server = server + "/";
        while (shareURL.startsWith("/")) shareURL = shareURL.substring(1);
        if (server.startsWith("//")) server = "https:" + server;
        return server + shareURL;
    }

    public void abort() {
        synchronized (mutex) {
            if (state == 2) ss.stopASAP();
            if (state == 4) st.abort();
            state = 5;
        }
    }

    public static abstract class ServerSelectedHandler {
        public abstract void onServerSelected(TestPoint server);
    }

    public static abstract class SpeedTestHandler {
        public abstract void onDownloadUpdate(double dl, double progress);

        public abstract void onUploadUpdate(double ul, double progress);

        public abstract void onPingJitterUpdate(double ping, double jitter, double progress);

        public abstract void onIPInfoUpdate(String ipInfo);

        public abstract void onTestIDReceived(String id, String shareURL);

        public abstract void onEnd();

        public abstract void onCriticalFailure(String err);
    }
}
