package com.sdt.diagnose.Device.DeviceInfo;

import android.text.TextUtils;

import com.sdt.annotations.Tr369Get;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.IProtocolArray;
import com.sdt.diagnose.common.ProcessManager;
import com.sdt.diagnose.common.ProtocolPathUtils;
import com.sdt.diagnose.common.bean.ProcessInfo;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.database.DbManager;

import org.jetbrains.annotations.NotNull;

import java.util.List;


public class ProcessInfoX implements IProtocolArray<ProcessInfo> {
    private static final String TAG = "ProcessInfoX";
    private final static String REFIX = "Device.DeviceInfo.ProcessStatus.Process.";
    private static ProcessManager mProcessManager = null;

    @Tr369Get("Device.DeviceInfo.ProcessStatus.Process.")
    public String SK_TR369_GetProcessInfo(String path) {
        LogUtils.d(TAG, "GetProcessInfo path: " + path);
        return handleProcessInfoX(path);
    }

    private String handleProcessInfoX(String path) {
        return ProtocolPathUtils.getInfoFromArray(REFIX, path, this);
    }

    public static List<ProcessInfo> getProcessInfo() {
        if (mProcessManager != null && !mProcessManager.isEmpty()) {
            return mProcessManager.getList();
        }
        mProcessManager = new ProcessManager(GlobalContext.getContext());
        return mProcessManager.getList();
    }

    @Override
    public List<ProcessInfo> getArray() {
        return getProcessInfo();
    }

    @Override
    public String getValue(ProcessInfo t, @NotNull String[] paramsArr) {
        if (paramsArr.length < 2) {
            return null;
        }
        String secondParam = paramsArr[1];
        if (TextUtils.isEmpty(secondParam)) {
            //Todo report error.
            return null;
        }
        switch (secondParam) {
            case "PID":
                return String.valueOf(t.getPid());
            case "Command":
                return t.getCommand();
            case "Size":
                return String.valueOf(t.getSize());
            case "Priority":
                return String.valueOf(t.getPriority());
            case "CPUTime":
                return String.valueOf(t.getCpuTime());
            case "State":
                return t.getState();
            default:
                break;
        }
        return null;
    }

    public static void updateProcessList() {
        if (mProcessManager != null) {
            if (!mProcessManager.isEmpty()) {
                mProcessManager.clear();
            }
            mProcessManager = null;
        }
        mProcessManager = new ProcessManager(GlobalContext.getContext());
        int size = mProcessManager.getList().size();
        LogUtils.d(TAG, "Get the number of Process list: " + size);
        if (size > 0) {
            DbManager.updateMultiObject("Device.DeviceInfo.ProcessStatus.Process", size);
        } else {
            DbManager.delMultiObject("Device.DeviceInfo.ProcessStatus.Process");
        }
    }
}
