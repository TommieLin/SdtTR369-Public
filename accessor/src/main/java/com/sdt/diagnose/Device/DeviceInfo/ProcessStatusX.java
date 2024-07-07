package com.sdt.diagnose.Device.DeviceInfo;

import com.sdt.annotations.Tr369Get;
import com.sdt.diagnose.Device.Platform.ModelX;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.bean.ProcessInfo;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.extra.CmsExtraServiceManager;

import java.util.List;

public class ProcessStatusX {
    private static final String TAG = "ProcessStatusX";
    private ModelX.Type mStbModelType = null;

    @Tr369Get("Device.DeviceInfo.ProcessStatus.CPUUsage")
    public String SK_TR369_GetProcessStatusCPUUsage() {
        double rate = 0;
        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            rate = AmlProcessStatusX.getInstance().getCpuUsageByAml();
        } else if (mStbModelType == ModelX.Type.Realtek) {
            rate = RtkProcessStatusX.getInstance().getCpuUsageByRtk();
        } else {
            try {
                CmsExtraServiceManager mCmsExtraServiceManager = CmsExtraServiceManager.getInstance(GlobalContext.getContext());
                if (null != mCmsExtraServiceManager) {
                    rate = mCmsExtraServiceManager.getCpuUsage();
                } else {
                    LogUtils.e(TAG, "getCPUUsage: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "getCPUUsage: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }
        return String.valueOf((int) rate);
    }

    @Tr369Get("Device.DeviceInfo.ProcessStatus.ProcessNumberOfEntries")
    public String SK_TR369_GetProcessNumber() {
        int processNum = 0;
        List<ProcessInfo> mProcessList = ProcessInfoX.getProcessInfo();
        if (mProcessList != null) {
            processNum = mProcessList.size();
            LogUtils.d(TAG, "GetProcessNumber: " + processNum);
        }
        return String.valueOf(processNum);
    }

}
