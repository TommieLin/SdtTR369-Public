package com.sdt.diagnose.common;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Debug;

import com.sdt.diagnose.common.bean.ProcessInfo;
import com.sdt.diagnose.common.log.LogUtils;

import java.util.List;

public class ProcessManager extends AbstractCachedArray<ProcessInfo> {
    private static final String TAG = "ProcessManager";

    public ProcessManager(Context context) {
        super(context);
    }

    @Override
    void buildList(Context context) {
        List<ActivityManager.RunningAppProcessInfo> procInfoList;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            procInfoList = am.getRunningAppProcesses();
            if (!procInfoList.isEmpty()) {
                LogUtils.d(TAG, "procInfoList size: " + procInfoList.size());
                for (int i = 0; i < procInfoList.size(); i++) {
                    ProcessInfo process = new ProcessInfo(procInfoList.get(i));
                    LogUtils.d(TAG, "procInfoList name: " + process.getCommand());
                    int[] pids = new int[1];
                    pids[0] = process.getPid();
                    Debug.MemoryInfo[] curMemInfo = am.getProcessMemoryInfo(pids);
                    if (curMemInfo != null) {
                        process.setSize(curMemInfo[0].dalvikPrivateDirty);
                    }
                    add(process);
                }
            }
        }
    }
}
