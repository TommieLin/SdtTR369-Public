package com.sdt.diagnose.Device.X_Skyworth.Tcpdump;

import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.StatFs;
import android.os.SystemProperties;
import android.text.TextUtils;

import com.sdt.annotations.Tr369Set;
import com.sdt.diagnose.command.Event;
import com.sdt.diagnose.common.log.LogUtils;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

/**
 * @Author Outis
 * @Date 2023/11/30 10:52
 * @Version 1.0
 */
public class TcpdumpX {
    private static final String TAG = "TcpdumpX";
    private final int MAX_TCPDUMP_DURATION = 30;    // 单位秒
    private final long MAX_TCPDUMP_SIZE = 300;  // 单位MB
    private final long MAX_TCPDUMP_PERCENT = 5;
    TcpdumpBean tcpdumpBean = new TcpdumpBean();
    private Timer timer;
    Handler handler;

    @Tr369Set("Device.X_Skyworth.Tcpdump.")
    public boolean SK_TR369_SetTcpdumpParams(String path, String value) {
        LogUtils.i(TAG, "SetTcpdumpParams path: " + path + ", value: " + value);
        String[] split = path.split("\\.");
        switch (split[split.length - 1]) {
            case "Enable":
                tcpdumpBean.setEnable(value);
                break;
            case "Url":
                tcpdumpBean.setUrl(value);
                // 开始Tcpdump功能
                checkStart();
                break;
            case "Ip":
                tcpdumpBean.setIp(value);
                break;
            case "Port":
                tcpdumpBean.setPort(value);
                break;
            case "Duration":
                if (!TextUtils.isEmpty(value)) {
                    tcpdumpBean.setDuration(value);
                } else {
                    //如果前端没有设置抓包时长，默认抓包30S
                    tcpdumpBean.setDuration(String.valueOf(MAX_TCPDUMP_DURATION));
                }
                break;
            case "NetType":
                tcpdumpBean.setNetType(value);
                break;
            case "FileSize":
                tcpdumpBean.setFileSize(Long.parseLong(value));
                break;
        }

        return true;
    }

    HandlerThread handlerThread;

    public void startTcpdump() {
        LogUtils.i(TAG, "startTcpdump called.");
        SystemProperties.set("persist.sys.skyworth.tcpdump", "1");
        File file = new File("/data/tcpdump/test1.pcap");
        handlerThread = new HandlerThread("tcpdump");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!file.exists()) {
                    LogUtils.e(TAG, "The pcap file already exist.");
                    stopTcpdump();
                    return;
                }
                long fileSize = getFileSize();
                while (true) {
                    if (file.length() >= fileSize * 1024 * 1024L) {
                        LogUtils.e(TAG, "startTcpdump getFileSize: " + tcpdumpBean.getFileSize());
                        stopTcpdump();
                        Event.uploadLogFile(tcpdumpBean.getUrl(), file.getAbsolutePath(), 1);
                        timer.cancel();
                        return;
                    }
                }
            }
        }, 500);

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                LogUtils.i(TAG, "startTcpdump Tcpdump Timer.");
                stopTcpdump();
                handler.removeCallbacksAndMessages(null);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    LogUtils.e(TAG, "Timer operation error, " + e.getMessage());
                }
                Event.uploadLogFile(tcpdumpBean.getUrl(), file.getAbsolutePath(), 1);
            }
        }, Integer.parseInt(tcpdumpBean.getDuration()) * 1000L);
    }

    public void stopTcpdump() {
        LogUtils.i(TAG, "stopTcpdump called.");
        SystemProperties.set("persist.sys.skyworth.tcpdump", "0");
        SystemProperties.set("persist.sys.skyworth.tcpdump.args", " ");
        handlerThread.quit();
    }

    public boolean generateArgs() {
        StringBuilder stringBuilder = new StringBuilder();
        if (!TextUtils.isEmpty(tcpdumpBean.getNetType())) {
            stringBuilder.append(tcpdumpBean.getNetType());
        }
        if (!TextUtils.isEmpty(tcpdumpBean.getPort())) {
            stringBuilder.append(" port ").append(tcpdumpBean.getPort());
        }
        if (!TextUtils.isEmpty(tcpdumpBean.getIp())) {
            if (!TextUtils.isEmpty(stringBuilder.toString())) {
                stringBuilder.append(" and");
            }
            stringBuilder.append(" dst ").append(tcpdumpBean.getIp());
        }
        if (!TextUtils.isEmpty(stringBuilder.toString())) {
            SystemProperties.set("persist.sys.skyworth.tcpdump.args", stringBuilder.toString());
            LogUtils.d(TAG, "Set tcpdump args: " + stringBuilder);
            return true;
        } else {
            LogUtils.e(TAG, "Error: The parameter cannot be empty.");
            SystemProperties.set("persist.sys.skyworth.tcpdump", "0");
            SystemProperties.set("persist.sys.skyworth.tcpdump.args", " ");
        }
        return false;
    }

    public void checkStart() {
        if (tcpdumpBean.getEnable().equals("1") && !TextUtils.isEmpty(tcpdumpBean.getUrl())) {
            if (generateArgs()) startTcpdump();
        }
    }


    /**
     * @return 抓包文件大小限制
     */
    public long getFileSize() {
        if (tcpdumpBean.getFileSize() > 0) {
            //前端指定文件大小
            return getFreeRom() >=
                    (tcpdumpBean.getFileSize() * MAX_TCPDUMP_PERCENT)
                    ? tcpdumpBean.getFileSize()
                    : getFreeRom() / MAX_TCPDUMP_PERCENT;
        } else {
            //配置文件指定文件大小
            return getFreeRom() >=
                    (MAX_TCPDUMP_SIZE * MAX_TCPDUMP_PERCENT)
                    ? MAX_TCPDUMP_SIZE
                    : getFreeRom() / MAX_TCPDUMP_PERCENT;
        }
    }

    public long getFreeRom() {
        File file = Environment.getDataDirectory();
        StatFs stat = new StatFs(file.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        LogUtils.d(TAG, "getFreeRom result: " + (blockSize * availableBlocks / 1000000));
        return blockSize * availableBlocks / 1000000;//单位MB
    }
}
