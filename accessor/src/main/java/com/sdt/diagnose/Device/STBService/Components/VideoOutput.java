package com.sdt.diagnose.Device.STBService.Components;

import com.droidlogic.app.SystemControlManager;
import com.realtek.hardware.RtkHDMIManager2;
import com.sdt.annotations.Tr369Get;
import com.sdt.diagnose.Device.Platform.ModelX;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.log.LogUtils;

public class VideoOutput {
    private static final String TAG = "VideoOutput";
    // HDCP Version
    private static final String RX_HDCP_VER_PATH = "/sys/class/amhdmitx/amhdmitx0/hdcp_ver";
    // HDCP Mode
    public static final String TX_HDCP_MODE_PATH = "/sys/class/amhdmitx/amhdmitx0/hdcp_mode";
    // Uncertain about the intention of this file.
    public static final String HDCP_AUTHENTICATED_PATH =
            "/sys/module/hdmitx20/parameters/hdmi_authenticated";

    private static final String RESULT_SUCCESS = "OK";
    private static final String RESULT_FAILED = "FAILED";
    public static final String HDCP_14 = "HDCP14";
    public static final String HDCP_22 = "HDCP22";
    public static final String HDCP_NONE = "None";

    private ModelX.Type mStbModelType = null;

    @Tr369Get("Device.Services.STBService.1.Components.VideoOutput.1.HDCP.Status")
    public String SK_TR369_GetVideoOutputHDCPStatus() {
        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            // Amlogic平台
            return getHDCPStatusByAml();
        } else if (mStbModelType == ModelX.Type.Realtek) {
            // Realtek平台
            return getHDCPStatusByRtk();
        } else {
            return RESULT_FAILED;
        }
    }

    private String getHDCPStatusByAml() {
        try {
//            String authenticated = SystemControlManager.getInstance().readSysFs(HDCP_AUTHENTICATED_PATH);
            String rxHdcpVer = SystemControlManager.getInstance().readSysFs(RX_HDCP_VER_PATH);
            String txHdcpMode = SystemControlManager.getInstance().readSysFs(TX_HDCP_MODE_PATH);

            if (!rxHdcpVer.isEmpty() && !txHdcpMode.isEmpty()) {
                if (rxHdcpVer.contains("14")
                        && txHdcpMode.contains("14")) {
//                        && authenticated.contains("1")) {
                    return RESULT_SUCCESS;
                } else if (rxHdcpVer.contains("22")
                        && txHdcpMode.contains("22")) {
//                        && authenticated.contains("1")) {
                    return RESULT_SUCCESS;
                }
            }
        } catch (NoClassDefFoundError e) {
            LogUtils.e(TAG, "getHDCPStatusByAml: SystemControlManager call failed, " + e.getMessage());
        }
        return RESULT_FAILED;
    }

    private String getHDCPStatusByRtk() {
        try {
            int hdcp = RtkHDMIManager2.getRtkHDMIManager(GlobalContext.getContext()).getHDCPVersion();
            LogUtils.d(TAG, "Get HDCP version: " + hdcp);
            if (hdcp > RtkHDMIManager2.HDCP_NONE) {
                return RESULT_SUCCESS;
            }
        } catch (NoClassDefFoundError e) {
            LogUtils.e(TAG, "getHDCPStatusByRtk: RtkHDMIManager2 call failed, " + e.getMessage());
        }
        return RESULT_FAILED;
    }

    @Tr369Get("Device.Services.STBService.1.Components.VideoOutput.1.HDCP.Type")
    public String SK_TR369_GetVideoOutputHDCPType() {
        if (mStbModelType == null) mStbModelType = ModelX.getPlatform();
        if (mStbModelType == ModelX.Type.Amlogic) {
            // Amlogic平台
            return getHDCPTypeByAml();
        } else if (mStbModelType == ModelX.Type.Realtek) {
            // Realtek平台
            return getHDCPTypeByRtk();
        } else {
            return HDCP_NONE;
        }
    }

    private String getHDCPTypeByAml() {
        try {
//            String authenticated = SystemControlManager.getInstance().readSysFs(HDCP_AUTHENTICATED_PATH);
            String rxHdcpVer = SystemControlManager.getInstance().readSysFs(RX_HDCP_VER_PATH);
            String txHdcpMode = SystemControlManager.getInstance().readSysFs(TX_HDCP_MODE_PATH);

            if (!rxHdcpVer.isEmpty() && !txHdcpMode.isEmpty()) {
                if (rxHdcpVer.contains("14")
                        && txHdcpMode.contains("14")) {
//                        && authenticated.contains("1")) {
                    return HDCP_14;
                } else if (rxHdcpVer.contains("22")
                        && txHdcpMode.contains("22")) {
//                        && authenticated.contains("1")) {
                    return HDCP_22;
                }
            }
        } catch (NoClassDefFoundError e) {
            LogUtils.e(TAG, "getHDCPTypeByAml: SystemControlManager call failed, " + e.getMessage());
        }
        return HDCP_NONE;
    }

    private String getHDCPTypeByRtk() {
        try {
            int hdcp = RtkHDMIManager2.getRtkHDMIManager(GlobalContext.getContext()).getHDCPVersion();
            LogUtils.d(TAG, "Get HDCP version: " + hdcp);
            if (hdcp == RtkHDMIManager2.HDCP_14) {
                return HDCP_14;
            } else if (hdcp == RtkHDMIManager2.HDCP_22) {
                return HDCP_22;
            }
        } catch (NoClassDefFoundError e) {
            LogUtils.e(TAG, "getHDCPTypeByRtk: RtkHDMIManager2 call failed, " + e.getMessage());
        }
        return HDCP_NONE;
    }

}
