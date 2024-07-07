package com.sdt.diagnose.Device.X_Skyworth;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.text.TextUtils;

import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.common.net.HttpsUtils;

import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @Author Outis
 * @Date 2023/11/30 16:15
 * @Version 1.0
 */
public class LogManager {
    private static final String TAG = "LogManager";
    public int enable = 0;
    public String url = "";
    public static String logLevel = "Info";
    public static String keywords = "";
    public int interval = 1;
    public String transactionId;
    public String command;
    public static AtomicBoolean condition = new AtomicBoolean(true);
    public ThreadPoolExecutor threadPoolExecutor;
    public HttpsUtils.OnUploadCallback mOnUploadCallback;

    public static LogManager logManager;
    public Process p;
    public InputStream is;
    StringBuffer sb = new StringBuffer();
    Handler handler;
    HandlerThread handlerThread;
    String[] cmds = new String[]{"logcat"};
    public static LogThread mLogThread;

    private static final int MSG_START_REALTIME_LOG = 3308;

    public void init() {
        handlerThread = new HandlerThread("setFlag");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == MSG_START_REALTIME_LOG) {
                    handler.removeMessages(MSG_START_REALTIME_LOG);
                    condition.set(true);
                }
            }
        };
        mOnUploadCallback = length -> sb.delete(0, length);
        HttpsUtils.setOnUploadCallback(mOnUploadCallback);
    }

    public volatile boolean read = true;

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    private LogManager() {
    }

    public static LogManager getInstance() {
        synchronized (LogManager.class) {
            if (logManager == null) {
                logManager = new LogManager();
            }
        }
        return logManager;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public int isEnable() {
        return enable;
    }

    public void setEnable(int enable) {
        this.enable = enable;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public int getInterval() {
        return interval;
    }

    public void setInterval(int interval) {
        this.interval = interval;
    }

    public int Interval2Ms() {
        return getInstance().getInterval() * 1000;
    }

    public LogThread getThreadInstance() {
        synchronized (LogThread.class) {
            if (mLogThread == null) {
                mLogThread = new LogThread() {
                    @Override
                    public void getLog() {
                        getRealtimeLog();
                    }
                };
            }
        }
        return mLogThread;
    }

    public void startLog() {
        setRead(true);
        initThreadPool();
        getThreadInstance().start();
    }

    public void stopLog() {
        setRead(false);
        threadPoolExecutor.shutdownNow();
        getThreadInstance().interrupt();
        mLogThread = null;
    }

    public void start() {
        if (getInstance().isEnable() == 1
                && !TextUtils.isEmpty(getInstance().getUrl())
                && !TextUtils.isEmpty(getInstance().getTransactionId())
                && getInstance().getInterval() > 0) {
            getInstance().startLog();
        }
    }

    public void initThreadPool() {
        threadPoolExecutor = new ThreadPoolExecutor(4, 8, 50, TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(20), new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    public void getRealtimeLog() {
        try {
            init();
            getCmds();
            Runtime.getRuntime().exec("logcat -c");
            p = Runtime.getRuntime().exec(cmds);
            is = p.getInputStream();
            int len = 0;
            byte[] buf = new byte[1024];
            while (isRead() && (-1 != (len = is.read(buf)))) {
                sb.append(new String(buf, 0, len));
                if (condition.get()) {
                    handler.sendEmptyMessageDelayed(MSG_START_REALTIME_LOG, Interval2Ms());
                    condition.set(false);
                    //这里进行log上传
                    threadPoolExecutor.execute(
                            () -> HttpsUtils.uploadLog(getUrl(), sb.toString(), getTransactionId()));
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "getRealtimeLog call failed, " + e.getMessage());
        }
    }

    public String[] getCmds() {
        cmds = new String[]{"logcat"};
        if (!TextUtils.isEmpty(getInstance().getLogLevel())
                && !TextUtils.isEmpty(getInstance().getKeywords())) {
            cmds = new String[]{"logcat", getInstance().getKeywords() + ":" + getInstance().getLogLevel(), "*:s"};
        }
        if (!TextUtils.isEmpty(getInstance().getKeywords())
                && TextUtils.isEmpty(getInstance().getLogLevel())) {
            cmds = new String[]{"logcat", "-s", getInstance().getKeywords()};
        }
        if (TextUtils.isEmpty(getInstance().getKeywords())
                && !TextUtils.isEmpty(getInstance().getLogLevel())) {
            cmds = new String[]{"logcat", "*:" + getInstance().getLogLevel().charAt(0)};
        }
        return cmds;
    }
}
