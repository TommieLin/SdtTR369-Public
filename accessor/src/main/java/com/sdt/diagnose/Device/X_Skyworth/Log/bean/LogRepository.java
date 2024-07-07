package com.sdt.diagnose.Device.X_Skyworth.Log.bean;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;
import android.text.TextUtils;
import android.text.format.Formatter;

import androidx.annotation.NonNull;

import com.sdt.diagnose.Device.X_Skyworth.Log.utils.StorageUtils;
import com.sdt.diagnose.Device.X_Skyworth.Log.utils.SystemUtils;
import com.sdt.diagnose.command.Event;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.database.DbManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * ClassName: LogRepository
 *
 * <p>ClassDescription: LogRepository
 *
 * <p>Author: ZHX Date: 2022/9/7
 *
 * <p>Editor: Outis Data: 2023/11/30
 */
public class LogRepository {
    private static final String TAG = "LogRepository";
    // Constants
    private static final String LOG_SOURCE_DIR_PATH = "/data/tcpdump/";
    private static final String LOG_SOURCE_FILE_PATH = "/data/tcpdump/logcat_tr369.log";
    private static final String FILE_LOG_COMMAND = "/mnt/skyinfo/tr369_command";
    private static final String KEY_COMMAND_NAME = "command_name";
    private static final String KEY_COMMAND_PARAM = "command_param";
    private static volatile LogRepository sLogRepository;
    private final Handler mHandler;
    private final HandlerThread mThread;
    private final int MSG_START_SPLIT_LOG_FILE = 3307;
    private int mPeriodicMillisTime = 0;
    private final int DEFAULT_PERIOD_MILLIS_TIME = 1800000;   // 默认三十分钟统计一次
    private boolean isAllowAutoUpload = false;
    private String mUploadUrl = null;
    private final int MAX_LOG_RETENTION_DAYS = 7;   // 日志文件最大保存天数

    private LogRepository() {
        setAutoUploadStatus();
        setPeriodicMillisTime();
        mThread = new HandlerThread("LogRepositoryThread", Process.THREAD_PRIORITY_BACKGROUND);
        mThread.start();
        mHandler =
                new Handler(mThread.getLooper()) {
                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        if (msg.what == MSG_START_SPLIT_LOG_FILE) {
                            // 按时间间隔保存日志文件
                            splitLogFile();
                            // 自动上传日志功能
                            if (isAllowAutoUpload) {
                                autoUploadLogFile();
                            }
                            // 清理存储超过规定天数的日志文件
                            cleanExpiredFiles();
                        }
                    }
                };
    }

    public static LogRepository getLogRepository() {
        if (sLogRepository == null) {
            synchronized (LogRepository.class) {
                if (sLogRepository == null) {
                    sLogRepository = new LogRepository();
                }
            }
        }
        return sLogRepository;
    }

    private long getMillisToNextHour() {
        // 获取当前时间
        Calendar calendar = Calendar.getInstance();
        long currentTimeInMillis = calendar.getTimeInMillis();

        // 设置分钟和秒钟为0
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // 获取下一个整点时刻
        calendar.add(Calendar.HOUR_OF_DAY, 1);
        long nextHourInMillis = calendar.getTimeInMillis();

        // 计算剩余毫秒数
        long remainingMillis = nextHourInMillis - currentTimeInMillis;

        // 输出剩余毫秒数
        LogUtils.d(TAG, "Time remaining until the next whole hour: " + remainingMillis + "ms");
        return remainingMillis;
    }

    public boolean setPeriodicMillisTime(String time) {
        if (time != null &&
                time.length() != 0 &&
                Integer.parseInt(time) > 0) {
            setPeriodicMillisTime(Integer.parseInt(time) * 1000);
            if (mHandler.hasMessages(MSG_START_SPLIT_LOG_FILE)) {
                mHandler.removeMessages(MSG_START_SPLIT_LOG_FILE);
            }
            mHandler.sendEmptyMessageDelayed(MSG_START_SPLIT_LOG_FILE, mPeriodicMillisTime);
            return (DbManager.setDBParam("Device.X_Skyworth.Logcat.AutoUpload.Interval", time) == 0);
        } else {
            LogUtils.e(TAG, "Failed to set the automatic upload periodic time");
            return false;
        }
    }

    private void setPeriodicMillisTime(int time) {
        mPeriodicMillisTime = time;
    }

    private void setPeriodicMillisTime() {
        mPeriodicMillisTime = DEFAULT_PERIOD_MILLIS_TIME;
        String time = DbManager.getDBParam("Device.X_Skyworth.Logcat.AutoUpload.Interval");
        // AutoUploadInterval单位：秒
        if (time.length() != 0 && Integer.parseInt(time) >= 0) {
            mPeriodicMillisTime = Integer.parseInt(time) * 1000;
        }
    }

    public int getPeriodicMillisTime() {
        return mPeriodicMillisTime / 1000;
    }

    public boolean getAutoUploadStatus() {
        return isAllowAutoUpload;
    }

    public void setAutoUploadStatus(boolean status) {
        isAllowAutoUpload = status;
    }

    public void setAutoUploadStatus() {
        String status = DbManager.getDBParam("Device.X_Skyworth.Logcat.AutoUpload.Enable");
        isAllowAutoUpload = "1".equals(status);
    }

    public void setAutoUploadUrl(String url) {
        mUploadUrl = url;
    }

    public String getAutoUploadUrl() {
        return mUploadUrl;
    }

    private void autoUploadLogFile() {
        if (TextUtils.isEmpty(mUploadUrl)) {
            LogUtils.e(TAG, "The upload URL in the class is empty");
            String dbUrl = DbManager.getDBParam("Device.X_Skyworth.Logcat.AutoUpload.Url");
            if (TextUtils.isEmpty(dbUrl)) {
                LogUtils.e(TAG, "The upload URL in the database is empty");
                return;
            }
            setAutoUploadUrl(dbUrl);
        }

        File folder = new File(LOG_SOURCE_DIR_PATH);
        if (!folder.exists() || !folder.isDirectory()) {
            return;
        }
        ArrayList<String> logFiles = new ArrayList<>();
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    if (fileName.contains("logcat_tr369_")
                            && fileName.contains(".txt")) {
                        logFiles.add(file.getAbsolutePath());
                    }
                }
            }

            int fileCounts = logFiles.size();
            if (fileCounts > 0) {
                for (String logFile : logFiles) {
                    LogUtils.d(TAG, "Waiting for automatic upload of file: " + logFile);
                    Event.uploadLogFile(mUploadUrl, logFile, fileCounts);
                    fileCounts--;
                }
            }
        }
    }

    /**
     * Split log file.
     *
     * @return Path of the newly generated file.
     */
    private void splitLogFile() {
        // 先暂停抓取日志指令
        stopCommand();
        // 复制一份日志文件，并根据系统时间戳命令文件（时间戳为秒级）
        String time = String.valueOf(System.currentTimeMillis());
        File folder = new File(LOG_SOURCE_DIR_PATH);
        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    String fileName = file.getName();
                    // 先把logcat_tr369.log.*的所有文件依次拷贝至新文件
                    if (file.isFile() && fileName.startsWith(Event.RAW_LOG_FILE + ".")) {
                        copyLogFile(LOG_SOURCE_DIR_PATH + fileName,
                                LOG_SOURCE_DIR_PATH + "logcat_tr369_" + time + ".txt");
                        // 删除原始日志文件
                        file.delete();
                    }
                }
                // 最后将logcat_tr369.log拷贝至新文件
                File sourceFile = new File(LOG_SOURCE_FILE_PATH);
                if (sourceFile.exists()) {
                    copyLogFile(LOG_SOURCE_FILE_PATH,
                            LOG_SOURCE_DIR_PATH + "logcat_tr369_" + time + ".txt");
                    sourceFile.delete();
                }
            }
        }
        // 重启抓日志指令
        try {
            Runtime.getRuntime().exec("logcat -c");
        } catch (IOException e) {
            LogUtils.e(TAG, "Command exec failed, " + e.getMessage());
        }
        startCommand(LogCmd.CatchLog, "sky_log_tr369_logcat.sh");
    }

    public void copyLogFile(String sourceFilePath, String destinationFilePath) {
        LogUtils.d(TAG, "Preparing to copy file " + sourceFilePath + " to " + destinationFilePath);
        File sourceFile = new File(sourceFilePath);
        File destinationFile = new File(destinationFilePath);

        FileInputStream fileInputStream = null;
        FileOutputStream fileOutputStream = null;

        try {
            fileInputStream = new FileInputStream(sourceFile);
            fileOutputStream = new FileOutputStream(destinationFile, true);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = fileInputStream.read(buffer)) > 0) {
                fileOutputStream.write(buffer, 0, length);
            }

            // 拷贝完成
            LogUtils.d(TAG, "File copy completely!");
        } catch (IOException e) {
            LogUtils.e(TAG, "File copy failed, " + e.getMessage());
        } finally {
            // 关闭输入输出流
            try {
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
                if (fileOutputStream != null) {
                    fileOutputStream.close();
                }
            } catch (IOException e) {
                LogUtils.e(TAG, "Stream close failed, " + e.getMessage());
            }
        }
    }

    private HashMap<String, String> setLogFilesInfoMap(String fileName, long fileSize) {
        final HashMap<String, String> map = new LinkedHashMap<>();
        map.put("Name", fileName);
        map.put("Size", Formatter.formatShortFileSize(GlobalContext.getContext(), fileSize));
        return map;
    }

    public String getLogList() {
        final List<HashMap<String, String>> filesInfo = new ArrayList<>();

        File folder = new File(LOG_SOURCE_DIR_PATH);
        if (!folder.exists() || !folder.isDirectory()) {
            return filesInfo.toString();
        }
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    if (fileName.contains("logcat_tr369")) {
                        long fileSize = file.length();
                        filesInfo.add(setLogFilesInfoMap(fileName, fileSize));
                    }
                }
            }
        }
        return filesInfo.toString();
    }

    private String[] split(String value) {
        if (TextUtils.isEmpty(value)) return null;
        return value.split("%%%");
    }

    private boolean isFileNeedToBeDeleted(String fileName, String startTime, String endTime) {
        if (fileName == null || startTime == null || endTime == null) {
            return false;
        }

        Pattern pattern = Pattern.compile("logcat_tr369_(.*?)\\.txt");
        Matcher matcher = pattern.matcher(fileName);
        String fileTime;

        if (matcher.find()) {
            fileTime = matcher.group(1);
            if (fileTime == null) return false;
            LogUtils.d(TAG, "isFileNeedToBeDeleted: fileTime: " + fileTime);
        } else {
            return false;
        }

        try {
            return Long.parseLong(fileTime) > Long.parseLong(startTime)
                    && Long.parseLong(fileTime) <= Long.parseLong(endTime);
        } catch (Exception e) {
            LogUtils.e(TAG, "isFileNeedToBeDeleted error, " + e.getMessage());
            return false;
        }
    }

    public boolean deleteLogFiles(String filterTime) {
        File folder = new File(LOG_SOURCE_DIR_PATH);
        if (!folder.exists() || !folder.isDirectory()) {
            return false;
        }

        String[] times = split(filterTime);
        String startTime = "";
        String endTime = "";
        if (times != null && times.length > 1) {
            startTime = times[0];
            endTime = times[1];
        }
        if (startTime.isEmpty() || endTime.isEmpty()) {
            return false;
        }

        File[] files = folder.listFiles();
        if (files != null) {
            LogUtils.d(TAG, "Preparing to delete all files between " + startTime + " and " + endTime);
            for (File file : files) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    if (fileName.contains("logcat_tr369_")
                            && fileName.contains(".txt")
                            && isFileNeedToBeDeleted(fileName, startTime, endTime)) {
                        LogUtils.d(TAG, "deleteLogFiles: Wait to delete file: " + fileName);
                        file.delete();
                    }
                }
            }
        }
        return true;
    }

    private void cleanExpiredFiles() {
        long curTime = System.currentTimeMillis();
        String endTime = String.valueOf(curTime - (MAX_LOG_RETENTION_DAYS * 24 * 60 * 60 * 1000));
        deleteLogFiles("0%%%" + endTime);
    }

    /**
     * start command
     *
     * @param cmd:LogCmd
     * @param param:param
     */
    public void startCommand(LogCmd cmd, String param) {
        String enable = DbManager.getDBParam("Device.X_Skyworth.Logcat.Background.Enable");
        if (!("1".equals(enable) || "true".equals(enable))) {
            LogUtils.i(TAG, "logcat background task is prohibited from being executed.");
            return;
        }
        StorageUtils.writeProperty(FILE_LOG_COMMAND, KEY_COMMAND_NAME, cmd.getCmdName());
        StorageUtils.writeProperty(FILE_LOG_COMMAND, KEY_COMMAND_PARAM, param);
        SystemUtils.startScriptService();
        if (cmd.getCmdName().equals(LogCmd.CatchLog.getCmdName())) {
            mHandler.sendEmptyMessageDelayed(MSG_START_SPLIT_LOG_FILE, mPeriodicMillisTime);
        }
    }

    /**
     * stop command
     */
    public void stopCommand() {
        StorageUtils.writeProperty(FILE_LOG_COMMAND, KEY_COMMAND_NAME, "do_nothing");
        StorageUtils.writeProperty(FILE_LOG_COMMAND, KEY_COMMAND_PARAM, "");
        SystemUtils.stopScriptService();
        mHandler.removeMessages(MSG_START_SPLIT_LOG_FILE);
    }

    /**
     * get current command name
     *
     * @return command name
     */
    public String getCurrentCommandName() {
        return StorageUtils.readProperty(FILE_LOG_COMMAND, KEY_COMMAND_NAME, "");
    }

    /**
     * get current command param
     *
     * @return command param
     */
    public String getCurrentCommandParam() {
        return StorageUtils.readProperty(FILE_LOG_COMMAND, KEY_COMMAND_PARAM, "");
    }

    public List<String> getSelectedScripts() {
        List<String> scripts = new ArrayList<>();
        String scriptContent = getCurrentCommandParam();
        if (TextUtils.isEmpty(scriptContent)) {
            return scripts;
        } else {
            String[] scriptArray = scriptContent.split(",");
            scripts.addAll(Arrays.asList(scriptArray));
        }
        return scripts;
    }
}
