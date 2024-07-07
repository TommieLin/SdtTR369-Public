package com.skyworthdigital.speedtest.core.worker;

import android.content.Context;
import android.util.Log;

import com.skyworthdigital.speedtest.core.base.Connection;
import com.skyworthdigital.speedtest.core.base.Utils;
import com.skyworthdigital.speedtest.core.config.SpeedTestConfig;
import com.skyworthdigital.speedtest.core.config.TelemetryConfig;
import com.skyworthdigital.speedtest.core.download.DownloadStream;
import com.skyworthdigital.speedtest.core.getIP.GetIP;
import com.skyworthdigital.speedtest.core.log.Logger;
import com.skyworthdigital.speedtest.core.ping.PingStream;
import com.skyworthdigital.speedtest.core.serverSelector.TestPoint;
import com.skyworthdigital.speedtest.core.telemetry.Telemetry;
import com.skyworthdigital.speedtest.core.upload.UploadStream;

import org.json.JSONObject;

import java.util.Locale;

public abstract class SpeedTestWorker extends Thread {
    private static final String TAG = "TR369 SpeedTestWorker";
    private final TestPoint backend;
    private final SpeedTestConfig config;
    private final TelemetryConfig telemetryConfig;
    private boolean stopASAP = false;
    private double dl = -1, ul = -1, ping = -1, jitter = -1;
    private String ipIsp = "";
    private final Logger log = new Logger();
    private final Context mContext;

    public SpeedTestWorker(TestPoint backend, SpeedTestConfig config,
            TelemetryConfig telemetryConfig, Context context) {
        this.backend = backend;
        this.config = config == null ? new SpeedTestConfig() : config;
        this.telemetryConfig = telemetryConfig == null ? new TelemetryConfig() : telemetryConfig;
        this.mContext = context;
        start();
    }

    public void run() {
        log.l("Test started");
        try {
            for (char t : config.getTestOrder().toCharArray()) {
                if (stopASAP) break;
                //if (t == '_') Utils.sleep(1000);
                if (t == 'I') getIP();
                if (t == 'D') dlTest();
                if (t == 'U') ulTest();
                //if (t == 'P') pingTest();
            }
        } catch (Throwable t) {
            onCriticalFailure(t.toString());
        }
        try {
            sendTelemetry();
        } catch (Throwable t) {
        }
        onEnd();
    }

    private boolean getIPCalled = false;

    private void getIP() {
        if (getIPCalled) {
            return;
        } else {
            getIPCalled = true;
        }
        final long start = System.currentTimeMillis();
        Connection c = null;
        try {
            c = new Connection(backend.getServer(), config.getPingConnectTimeout(),
                    config.getPingSoTimeout(), -1, -1);
        } catch (Throwable t) {
            if (config.getErrorHandlingMode().equals(SpeedTestConfig.ONERROR_FAIL)) {
                abort();
                onCriticalFailure(t.toString());
            }
            return;
        }
        GetIP g = new GetIP(c, backend.getGetIpURL(), config.getGetIPIsp(),
                config.getGetIPDistance()) {
            @Override
            public void onDataReceived(String data) {
                ipIsp = data;
                try {
                    data = new JSONObject(data).getString("processedString");
                } catch (Throwable t) {
                }
                log.l("GetIP: " + data + " (took " + (System.currentTimeMillis() - start) + "ms)");
                onIPInfoUpdate(data);
            }

            @Override
            public void onError(String err) {
                log.l("GetIP: FAILED (took " + (System.currentTimeMillis() - start) + "ms)");
                abort();
                onCriticalFailure(err);
            }
        };
        while (g.isAlive()) Utils.sleep(0, 100);
    }

    private boolean dlCalled = false;

    private void dlTest() {
        if (dlCalled) {
            return;
        } else {
            dlCalled = true;
        }
        final long start = System.currentTimeMillis();
        //开始测试
        onDownloadUpdate(0, 0);
        DownloadStream[] streams = new DownloadStream[config.getDlParallelStreams()];
        for (int i = 0; i < streams.length; i++) {
            streams[i] = new DownloadStream(backend.getServer(), backend.getDlURL(),
                    config.getDlCkSize(), config.getErrorHandlingMode(),
                    config.getDlConnectTimeout(), config.getDlSoTimeout(),
                    config.getDlRecvBuffer(), config.getDlSendBuffer(), log) {
                @Override
                public void onError(String err) {
                    log.l("Download: FAILED (took " + (System.currentTimeMillis() - start) + "ms)");
                    abort();
                    onCriticalFailure(err);
                }
            };
            Utils.sleep(config.getDlStreamDelay());
        }
        boolean graceTimeDone = false;
        long startT = System.currentTimeMillis(), bonusT = 0;
        for (; ; ) {
            double t = System.currentTimeMillis() - startT;
            if (!graceTimeDone && t >= config.getDlGraceTime() * 1000) {
                graceTimeDone = true;
                for (DownloadStream d : streams) d.resetDownloadCounter();
                startT = System.currentTimeMillis();
                continue;
            }
            if (stopASAP || t + bonusT >= config.getTimeDlMax() * 1000) {
                for (DownloadStream d : streams) d.stopASAP();
                for (DownloadStream d : streams) d.join();
                break;
            }
            if (graceTimeDone) {
                long totDownloaded = 0;
                for (DownloadStream d : streams) totDownloaded += d.getTotalDownloaded();
                double speed = totDownloaded / ((t < 100 ? 100 : t) / 1000.0);
                if (config.getTimeAuto()) {
                    double b = (2.5 * speed) / 100000.0;
                    bonusT += b > 200 ? 200 : b;
                }
                double progress = (t + bonusT) / (double) (config.getTimeDlMax() * 1000) * 3;
                speed = (speed * 8 * config.getOverheadCompensationFactor()) / (
                        config.getUseMebibits() ? 1048576.0 : 1000000.0);
                dl = speed;
                String actuallySpeed = format(dl);
                Log.d(TAG, "actually download Speed is " + actuallySpeed);
                //测试中
                onDownloadUpdate(dl, progress > 1 ? 1 : progress);
            }
            Utils.sleep(200);
        }
        if (stopASAP) return;
        log.l("Download: " + dl + " (took " + (System.currentTimeMillis() - start) + "ms)");
        //测试完成
        onDownloadUpdate(dl, 1);
    }

    private boolean ulCalled = false;

    private void ulTest() {
        if (ulCalled) {
            return;
        } else {
            ulCalled = true;
        }
        final long start = System.currentTimeMillis();
        onUploadUpdate(0, 0);
        UploadStream[] streams = new UploadStream[config.getUlParallelStreams()];
        for (int i = 0; i < streams.length; i++) {
            streams[i] = new UploadStream(backend.getServer(), backend.getUlURL(),
                    config.getUlCkSize(), config.getErrorHandlingMode(),
                    config.getUlConnectTimeout(), config.getUlSoTimeout(),
                    config.getUlRecvBuffer(), config.getUlSendBuffer(), log) {
                @Override
                public void onError(String err) {
                    log.l("Upload: FAILED (took " + (System.currentTimeMillis() - start) + "ms)");
                    abort();
                    onCriticalFailure(err);
                }
            };
            Utils.sleep(config.getUlStreamDelay());
        }
        boolean graceTimeDone = false;
        long startT = System.currentTimeMillis(), bonusT = 0;
        for (; ; ) {
            double t = System.currentTimeMillis() - startT;
            if (!graceTimeDone && t >= config.getUlGraceTime() * 1000) {
                graceTimeDone = true;
                for (UploadStream u : streams) u.resetUploadCounter();
                startT = System.currentTimeMillis();
                continue;
            }
            if (stopASAP || t + bonusT >= config.getTimeUlMax() * 1000) {
                for (UploadStream u : streams) u.stopASAP();
                for (UploadStream u : streams) u.join();
                break;
            }
            if (graceTimeDone) {
                long totUploaded = 0;
                for (UploadStream u : streams) totUploaded += u.getTotalUploaded();
                double speed = totUploaded / ((t < 100 ? 100 : t) / 1000.0);
                if (config.getTimeAuto()) {
                    double b = (2.5 * speed) / 100000.0;
                    bonusT += b > 200 ? 200 : b;
                }
                double progress = (t + bonusT) / (double) (config.getTimeUlMax() * 1000) * 3;
                speed = (speed * 8 * config.getOverheadCompensationFactor()) / (
                        config.getUseMebibits() ? 1048576.0 : 1000000.0);
                ul = speed;
                String ulSpeed = format(ul);
                Log.d(TAG, "actually UL speed is " + ulSpeed);
                onUploadUpdate(ul, progress > 1 ? 1 : progress);
            }
            Utils.sleep(200);
        }
        if (stopASAP) return;
        log.l("Upload: " + ul + " (took " + (System.currentTimeMillis() - start) + "ms)");
        onUploadUpdate(ul, 1);
    }

    private String format(double d) {
        try {
            Locale l = mContext.getResources().getConfiguration().getLocales().get(0);
            if (d < 10) return String.format(l, "%.2f", d);
            if (d < 100) return String.format(l, "%.1f", d);
            return "" + Math.round(d);
        } catch (Exception e) {
            Log.e(TAG, "format error, " + e.getMessage());
        }
        return "";
    }

    private boolean pingCalled = false;

    private void pingTest() {
        if (pingCalled) {
            return;
        } else {
            pingCalled = true;
        }
        final long start = System.currentTimeMillis();
        onPingJitterUpdate(0, 0, 0);
        PingStream ps = new PingStream(backend.getServer(), backend.getPingURL(),
                config.getCountPing(), config.getErrorHandlingMode(),
                config.getPingConnectTimeout(), config.getPingSoTimeout(),
                config.getPingRecvBuffer(), config.getPingSendBuffer(), log) {
            private double minPing = Double.MAX_VALUE, prevPing = -1;
            private int counter = 0;

            @Override
            public void onError(String err) {
                log.l("Ping: FAILED (took " + (System.currentTimeMillis() - start) + "ms)");
                abort();
                onCriticalFailure(err);
            }

            @Override
            public boolean onPong(long ns) {
                counter++;
                double ms = ns / 1000000.0;
                if (ms < minPing) minPing = ms;
                ping = minPing;
                if (prevPing == -1) {
                    jitter = 0;
                } else {
                    double j = Math.abs(ms - prevPing);
                    jitter = j > jitter ? (jitter * 0.3 + j * 0.7) : (jitter * 0.8 + j * 0.2);
                }
                prevPing = ms;
                double progress = counter / (double) config.getCountPing();
                onPingJitterUpdate(ping, jitter, progress > 1 ? 1 : progress);
                return !stopASAP;
            }

            @Override
            public void onDone() {
            }
        };
        ps.join();
        if (stopASAP) return;
        log.l("Ping: " + ping + " " + jitter + " (took " + (System.currentTimeMillis() - start)
                + "ms)");
        onPingJitterUpdate(ping, jitter, 1);
    }

    private void sendTelemetry() {
        if (telemetryConfig.getTelemetryLevel().equals(TelemetryConfig.LEVEL_DISABLED)) return;
        if (stopASAP && telemetryConfig.getTelemetryLevel().equals(TelemetryConfig.LEVEL_BASIC)) {
            return;
        }
        try {
            Connection c = new Connection(telemetryConfig.getServer(), -1, -1, -1, -1);
            Telemetry t = new Telemetry(c, telemetryConfig.getPath(),
                    telemetryConfig.getTelemetryLevel(), ipIsp, config.getTelemetryExtra(),
                    dl == -1 ? "" : String.format(Locale.ENGLISH, "%.2f", dl),
                    ul == -1 ? "" : String.format(Locale.ENGLISH, "%.2f", ul),
                    ping == -1 ? "" : String.format(Locale.ENGLISH, "%.2f", ping),
                    jitter == -1 ? "" : String.format(Locale.ENGLISH, "%.2f", jitter),
                    log.getLog()) {
                @Override
                public void onDataReceived(String data) {
                    if (data.startsWith("id")) {
                        onTestIDReceived(data.split(" ")[1]);
                    }
                }

                @Override
                public void onError(String err) {
                    System.err.println("Telemetry error: " + err);
                }
            };
            t.join();
        } catch (Throwable t) {
            System.err.println("Failed to send telemetry: " + t.toString());
            t.printStackTrace(System.err);
        }
    }

    public void abort() {
        if (stopASAP) return;
        log.l("Manually aborted");
        stopASAP = true;
    }

    public abstract void onDownloadUpdate(double dl, double progress);

    public abstract void onUploadUpdate(double ul, double progress);

    public abstract void onPingJitterUpdate(double ping, double jitter, double progress);

    public abstract void onIPInfoUpdate(String ipInfo);

    public abstract void onTestIDReceived(String id);

    public abstract void onEnd();

    public abstract void onCriticalFailure(String err);

}
