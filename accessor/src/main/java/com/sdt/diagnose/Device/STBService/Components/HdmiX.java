package com.sdt.diagnose.Device.STBService.Components;

import android.os.Build;
import android.provider.Settings;

import com.sdt.annotations.Tr369Get;
import com.sdt.annotations.Tr369Set;
import com.sdt.diagnose.Device.Platform.ModelX;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.extra.CmsExtraServiceManager;

import java.util.List;


public class HdmiX {
    private static final String TAG = "HdmiX";
    public static final String SETTINGS_HDMI_CONTROL_ENABLED = "hdmi_control_enabled";
    public static final String SETTINGS_ONE_TOUCH_PLAY = "hdmi_control_one_touch_play_enabled";
    public static final String SETTINGS_AUTO_POWER_OFF = "hdmi_control_auto_device_off_enabled";
    public static final String SETTINGS_AUTO_LANGUAGE_CHANGE = "hdmi_control_auto_language_change_enabled";
    public static final String SETTINGS_HDMI_VOLUME_CONTROL = "hdmi_control_volume_control_enabled";
    private static final int HDMI_NUMBER_ENTRIES = 1;
    CmsExtraServiceManager mCmsExtraServiceManager = CmsExtraServiceManager.getInstance(GlobalContext.getContext());
    private ModelX.Type mStbModelType = null;

    @Tr369Get("Device.Services.STBService.1.Components.HDMINumberOfEntries")
    public String SK_TR369_GetHdmiNumberEntries() {
        return String.valueOf(HDMI_NUMBER_ENTRIES);
    }

    /**
     * /sys/class/amhdmitx/amhdmitx0/hpd_state
     *
     * @return
     */
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

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.Enable")
    public String SK_TR369_GetHdmiEnable() {
        String status = Boolean.toString(false);

        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            status = AmlHdmiX.getInstance().getHdmiEnableByAml();
        } else if (mStbModelType == ModelX.Type.Realtek) {
            status = RtkHdmiX.getInstance().getHdmiEnableByRtk();
        } else {
            try {
                if (null != mCmsExtraServiceManager) {
                    status = mCmsExtraServiceManager.getHdmiStatus();
                } else {
                    LogUtils.e(TAG, "getHdmiEnable: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "getHdmiEnable: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }
        return status;
    }

    @Tr369Set("Device.Services.STBService.1.Components.HDMI.1.Enable")
    public boolean SK_TR369_SetHdmiEnable(String path, String val) {
        boolean isEnable = false;
        try {
            isEnable = "1".equals(val) || "true".equals(val);
        } catch (Exception e) {
            LogUtils.e(TAG, "setHdmiEnable: parseBoolean failed, " + e.getMessage());
        }

        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            return AmlHdmiX.getInstance().setHdmiEnableByAml(isEnable);
        } else if (mStbModelType == ModelX.Type.Realtek) {
            return RtkHdmiX.getInstance().setHdmiEnableByRtk(isEnable);
        } else {
            try {
                if (null != mCmsExtraServiceManager) {
                    return (mCmsExtraServiceManager.setHdmiStatus(isEnable) == 0);
                } else {
                    LogUtils.e(TAG, "setHdmiEnable: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "setHdmiEnable: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }
        return false;
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.Status")
    public String SK_TR369_GetHdmiStatus() {
        return isHDMIPlugged() ? "Plugged" : "Unplugged";
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.ResolutionMode")
    public String SK_TR369_GetHdmiResolutionMode() {
        if (!isHDMIPlugged()) return "";

        boolean isBest = false;
        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            isBest = AmlHdmiX.getInstance().getHdmiResolutionModeByAml();
        } else if (mStbModelType == ModelX.Type.Realtek) {
            isBest = RtkHdmiX.getInstance().getHdmiResolutionModeByRtk();
        } else {
            try {
                if (null != mCmsExtraServiceManager) {
                    isBest = mCmsExtraServiceManager.isBestOutputMode();
                } else {
                    LogUtils.e(TAG, "getHdmiResolutionMode: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "getHdmiResolutionMode: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }
        return isBest ? "Best" : "Manual";
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.Name")
    public String SK_TR369_GetHdmiName() {
        if (!isHDMIPlugged()) return "";

        String name = "";
        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            name = AmlHdmiX.getInstance().getHdmiNameByAml();
        } else if (mStbModelType == ModelX.Type.Realtek) {
            name = RtkHdmiX.getInstance().getHdmiNameByRtk();
        } else {
            try {
                if (null != mCmsExtraServiceManager) {
                    name = mCmsExtraServiceManager.getHdmiProductName();
                } else {
                    LogUtils.e(TAG, "getHdmiName: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "getHdmiName: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }
        return name;
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.ResolutionValue")
    public String SK_TR369_GetHdmiResolutionValue() {
        if (!isHDMIPlugged()) return "";

        String mode = "";
        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            mode = AmlHdmiX.getInstance().getHdmiResolutionValueByAml();
        } else if (mStbModelType == ModelX.Type.Realtek) {
            mode = RtkHdmiX.getInstance().getHdmiResolutionValueByRtk();
        } else {
            try {
                if (null != mCmsExtraServiceManager) {
                    mode = mCmsExtraServiceManager.getHdmiResolutionValue();
                } else {
                    LogUtils.e(TAG, "getHdmiResolutionValue: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "getHdmiResolutionValue: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }
        return mode;
    }

    @Tr369Set("Device.Services.STBService.1.Components.HDMI.1.ResolutionValue")
    public boolean SK_TR369_SetHdmiResolutionValue(String path, String value) {
        if (!isHDMIPlugged()) return false;

        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            return AmlHdmiX.getInstance().setHdmiResolutionValueByAml(value);
        } else if (mStbModelType == ModelX.Type.Realtek) {
            return RtkHdmiX.getInstance().setHdmiResolutionValueByRtk(value);
        } else {
            try {
                if (null != mCmsExtraServiceManager) {
                    String supportList = mCmsExtraServiceManager.getHdmiSupportResolution();
                    if (!supportList.contains(value)) {
                        LogUtils.e(TAG, "This resolution is not supported!");
                        return false;
                    }
                    mCmsExtraServiceManager.setHdmiResolutionValue(value);
                    return true;
                } else {
                    LogUtils.e(TAG, "setHdmiResolutionValue: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "setHdmiResolutionValue: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }

        return false;
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.DisplayDevice.SupportedResolutions")
    public String SK_TR369_GetHdmiDisplayDevSupportedResolutions() {
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
                    LogUtils.e(TAG, "getHdmiDisplayDevSupportedResolutions: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "getHdmiDisplayDevSupportedResolutions: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }
        return supportList;
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.DisplayDevice.Status")
    public String SK_TR369_GetHdmiDisplayDevStatus() {
        return isHDMIPlugged() ? "Present" : "None";
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.DisplayDevice.Name")
    public String SK_TR369_GetHdmiDisplayDevName() {
        if (!isHDMIPlugged()) return "";

        String name = "";
        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            name = AmlHdmiX.getInstance().getHdmiNameByAml();
        } else if (mStbModelType == ModelX.Type.Realtek) {
            name = RtkHdmiX.getInstance().getHdmiNameByRtk();
        } else {
            try {
                if (null != mCmsExtraServiceManager) {
                    name = mCmsExtraServiceManager.getHdmiProductName();
                } else {
                    LogUtils.e(TAG, "getHdmiDisplayDevName: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "getHdmiDisplayDevName: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }
        return name;
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.DisplayDevice.EEDID")
    public String SK_TR369_GetHdmiDisplayDevEEDID() {
        if (!isHDMIPlugged()) return "";

        String edid = "";
        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            edid = AmlHdmiX.getInstance().getHdmiEdidByAml();
        } else if (mStbModelType == ModelX.Type.Realtek) {
            edid = RtkHdmiX.getInstance().getHdmiEdidByRtk();
        } else {
            try {
                if (null != mCmsExtraServiceManager) {
                    edid = mCmsExtraServiceManager.getHdmiEdidVersion();
                } else {
                    LogUtils.e(TAG, "getHdmiDisplayDevEEDID: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "getHdmiDisplayDevEEDID: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }
        return edid;
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.DisplayDevice.PreferredResolution")
    public String SK_TR369_GetHdmiDisplayDevPreferredResolution() {
        if (!isHDMIPlugged()) return "";

        String mode = "";
        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            mode = AmlHdmiX.getInstance().getHdmiResolutionValueByAml();
        } else if (mStbModelType == ModelX.Type.Realtek) {
            mode = RtkHdmiX.getInstance().getHdmiResolutionValueByRtk();
        } else {
            try {
                if (null != mCmsExtraServiceManager) {
                    mode = mCmsExtraServiceManager.getHdmiResolutionValue();
                } else {
                    LogUtils.e(TAG, "getHdmiDisplayDevPreferredResolution: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "getHdmiDisplayDevPreferredResolution: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }
        return mode;
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.DisplayDevice.CECSupport")
    public String SK_TR369_GetHdmiDisplayDevCECSupport() {
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
                    LogUtils.e(TAG, "getHdmiDisplayDevCECSupport: CmsExtraServiceManager is null");
                }
            } catch (NullPointerException e) {
                LogUtils.e(TAG, "getHdmiDisplayDevCECSupport: CmsExtraServiceManager call failed, " + e.getMessage());
            }
        }
        return Boolean.toString(isSupport);
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.CecSwitch")
    public String SK_TR369_GetHdmiCecSwitchStatus() {
        int enable = Settings.Global.getInt(
                GlobalContext.getContext().getContentResolver(), SETTINGS_HDMI_CONTROL_ENABLED, 1);
        return String.valueOf(enable);
    }

    @Tr369Set("Device.Services.STBService.1.Components.HDMI.1.CecSwitch")
    public boolean SK_TR369_SetHdmiCecSwitchStatus(String path, String value) {
        try {
            if (Boolean.parseBoolean(value) || Integer.parseInt(value) == 1) {
                return Settings.Global.putInt(
                        GlobalContext.getContext().getContentResolver(), SETTINGS_HDMI_CONTROL_ENABLED, 1);
            } else if ((!Boolean.parseBoolean(value)) || Integer.parseInt(value) == 0) {
                return Settings.Global.putInt(
                        GlobalContext.getContext().getContentResolver(), SETTINGS_HDMI_CONTROL_ENABLED, 0);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "setHdmiCecSwitchStatus: parse value failed, " + e.getMessage());
        }
        return false;
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.CecOneKeyPlay")
    public String SK_TR369_GetHdmiCecOneKeyPlayStatus() {
        int enable = Settings.Global.getInt(
                GlobalContext.getContext().getContentResolver(), SETTINGS_ONE_TOUCH_PLAY, 0);
        return String.valueOf(enable);
    }

    @Tr369Set("Device.Services.STBService.1.Components.HDMI.1.CecOneKeyPlay")
    public boolean SK_TR369_SetHdmiCecOneKeyPlayStatus(String path, String value) {
        try {
            if (Boolean.parseBoolean(value) || Integer.parseInt(value) == 1) {
                return Settings.Global.putInt(
                        GlobalContext.getContext().getContentResolver(), SETTINGS_ONE_TOUCH_PLAY, 1);
            } else if ((!Boolean.parseBoolean(value)) || Integer.parseInt(value) == 0) {
                return Settings.Global.putInt(
                        GlobalContext.getContext().getContentResolver(), SETTINGS_ONE_TOUCH_PLAY, 0);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "setHdmiCecOneKeyPlayStatus: parse value failed, " + e.getMessage());
        }
        return false;
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.CecAutoPowerOff")
    public String SK_TR369_GetHdmiCecAutoPowerOffStatus() {
        int enable = Settings.Global.getInt(
                GlobalContext.getContext().getContentResolver(), SETTINGS_AUTO_POWER_OFF, 0);
        return String.valueOf(enable);
    }

    @Tr369Set("Device.Services.STBService.1.Components.HDMI.1.CecAutoPowerOff")
    public boolean SK_TR369_SetHdmiCecAutoPowerOffStatus(String path, String value) {
        try {
            if (Boolean.parseBoolean(value) || Integer.parseInt(value) == 1) {
                return Settings.Global.putInt(
                        GlobalContext.getContext().getContentResolver(), SETTINGS_AUTO_POWER_OFF, 1);
            } else if ((!Boolean.parseBoolean(value)) || Integer.parseInt(value) == 0) {
                return Settings.Global.putInt(
                        GlobalContext.getContext().getContentResolver(), SETTINGS_AUTO_POWER_OFF, 0);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "setHdmiCecAutoPowerOffStatus: parse value failed, " + e.getMessage());
        }
        return false;
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.CecVolumeControl")
    public String SK_TR369_GetHdmiCecVolumeControlStatus() {
        int enable = Settings.Global.getInt(
                GlobalContext.getContext().getContentResolver(), SETTINGS_HDMI_VOLUME_CONTROL, 0);
        return String.valueOf(enable);
    }

    @Tr369Set("Device.Services.STBService.1.Components.HDMI.1.CecVolumeControl")
    public boolean SK_TR369_SetHdmiCecVolumeControlStatus(String path, String value) {
        try {
            if (Boolean.parseBoolean(value) || Integer.parseInt(value) == 1) {
                return Settings.Global.putInt(
                        GlobalContext.getContext().getContentResolver(), SETTINGS_HDMI_VOLUME_CONTROL, 1);
            } else if ((!Boolean.parseBoolean(value)) || Integer.parseInt(value) == 0) {
                return Settings.Global.putInt(
                        GlobalContext.getContext().getContentResolver(), SETTINGS_HDMI_VOLUME_CONTROL, 0);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "setHdmiCecVolumeControlStatus: parse value failed, " + e.getMessage());
        }
        return false;
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.CecAutoChangeLanguage")
    public String SK_TR369_GetHdmiCecAutoChangeLanguageStatus() {
        int enable = Settings.Global.getInt(
                GlobalContext.getContext().getContentResolver(), SETTINGS_AUTO_LANGUAGE_CHANGE, 0);
        return String.valueOf(enable);
    }

    @Tr369Set("Device.Services.STBService.1.Components.HDMI.1.CecAutoChangeLanguage")
    public boolean SK_TR369_SetHdmiCecAutoChangeLanguageStatus(String path, String value) {
        try {
            if (Boolean.parseBoolean(value) || Integer.parseInt(value) == 1) {
                return Settings.Global.putInt(
                        GlobalContext.getContext().getContentResolver(), SETTINGS_AUTO_LANGUAGE_CHANGE, 1);
            } else if ((!Boolean.parseBoolean(value)) || Integer.parseInt(value) == 0) {
                return Settings.Global.putInt(
                        GlobalContext.getContext().getContentResolver(), SETTINGS_AUTO_LANGUAGE_CHANGE, 0);
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "setHdmiCecAutoChangeLanguageStatus: parse value failed, " + e.getMessage());
        }
        return false;
    }

    @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.DisplayDevice.HDMI3DPresent")
    public String SK_TR369_GetHdmiDisplayHDMI3DPresent() {
        //TODO amlogic 不支持
        return Boolean.FALSE.toString();
    }

    // @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.DisplayDevice.VideoLatency")
    public String SK_TR369_GetHdmiDisplayDevVideoLatency() {
        //TODO amlogic 不支持
        return Boolean.FALSE.toString();
    }

    // @Tr369Get("Device.Services.STBService.1.Components.HDMI.1.DisplayDevice.AutoLipSyncSupport")
    public String SK_TR369_GetHdmiDisplayDevAutoLipSyncSupport() {
        //TODO amlogic 不支持
        return Boolean.FALSE.toString();
    }
}
