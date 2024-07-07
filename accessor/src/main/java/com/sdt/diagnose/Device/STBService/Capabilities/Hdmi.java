package com.sdt.diagnose.Device.STBService.Capabilities;

import android.os.Build;

import com.sdt.annotations.Tr369Get;
import com.sdt.diagnose.Device.Platform.ModelX;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.Device.STBService.Components.AmlHdmiX;
import com.sdt.diagnose.Device.STBService.Components.DisplayCapabilityManager;
import com.sdt.diagnose.Device.STBService.Components.RtkHdmiX;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.extra.CmsExtraServiceManager;

import java.util.List;

public class Hdmi {
    private static final String TAG = "Hdmi";
    CmsExtraServiceManager mCmsExtraServiceManager = CmsExtraServiceManager.getInstance(GlobalContext.getContext());
    private ModelX.Type mStbModelType = null;

    private boolean isHDMIPlugged() {
        // HDMI 插入状态
        boolean isPlugged = false;
        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            isPlugged = AmlHdmiX.getInstance().isHDMIPluggedByAml();
        } else if (mStbModelType == ModelX.Type.Realtek) {
            isPlugged = RtkHdmiX.getInstance().isHDMIPluggedByRtk();
        } else {
            try {
                if (null != mCmsExtraServiceManager) {
                    isPlugged = mCmsExtraServiceManager.isHdmiPlugged();
                } else {
                    LogUtils.e(TAG, "isHDMIPlugged: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "isHDMIPlugged: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }
        return isPlugged;
    }

    @Tr369Get("Device.Services.STBService.1.Capabilities.HDMI.SupportedResolutions")
    public String SK_TR369_GetCapHdmiSupportResolution() {
        if (!isHDMIPlugged()) return "";

        String supportList = "";
        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            List<String> listHdmiMode = AmlHdmiX.getInstance().getHdmiSupportListByAml();
            if (Build.VERSION.SDK_INT > 30) {
                DisplayCapabilityManager.getInstance(GlobalContext.getContext()).filterNoSupportMode(listHdmiMode);
            }
            if (listHdmiMode.size() != 0)
                supportList = listHdmiMode.toString();
        } else if (mStbModelType == ModelX.Type.Realtek) {
            List<String> listHdmiMode = RtkHdmiX.getInstance().getHdmiSupportListByRtk();
            if (listHdmiMode.size() != 0)
                supportList = listHdmiMode.toString();
        } else {
            try {
                if (null != mCmsExtraServiceManager) {
                    supportList = mCmsExtraServiceManager.getHdmiSupportResolution();
                } else {
                    LogUtils.e(TAG, "getCapHdmiSupportResolution: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "getCapHdmiSupportResolution: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }
        return supportList;
    }

    @Tr369Get("Device.Services.STBService.1.Capabilities.HDMI.CECSupport")
    public String SK_TR369_GetCapHdmiCecSupport() {
        if (!isHDMIPlugged()) return "";

        boolean isSupport = false;
        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            isSupport = AmlHdmiX.getInstance().getCapHdmiCecSupportByAml();
        } else if (mStbModelType == ModelX.Type.Realtek) {
            isSupport = RtkHdmiX.getInstance().getCapHdmiCecSupportByRtk();
        } else {
            try {
                if (null != mCmsExtraServiceManager) {
                    isSupport = mCmsExtraServiceManager.isHdmiCecSupport();
                } else {
                    LogUtils.e(TAG, "getCapHdmiCecSupport: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "getCapHdmiCecSupport: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }
        return Boolean.toString(isSupport);
    }

}
