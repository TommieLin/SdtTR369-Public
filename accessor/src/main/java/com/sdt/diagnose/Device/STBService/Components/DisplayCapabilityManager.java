package com.sdt.diagnose.Device.STBService.Components;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.view.Display;

import com.droidlogic.app.OutputModeManager;
import com.droidlogic.app.SystemControlManager;
import com.google.common.collect.ImmutableMap;
import com.sdt.diagnose.common.log.LogUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DisplayCapabilityManager {
    private static final String TAG = "DisplayCapabilityManager";
    private static final String DISPLAY_MODE_TRUE = "true";
    private static final String DISPLAY_MODE_FALSE = "false";
    private static final String ENV_IS_BEST_MODE = "ubootenv.var.is.bestmode";
    private final DisplayManager mDisplayManager;
    private final Context mContext;
    private static DisplayCapabilityManager mDisplayCapabilityManager = null;

    public DisplayCapabilityManager(final Context context) {
        this.mContext = context;
        this.mDisplayManager = (DisplayManager) context.getSystemService(Context.DISPLAY_SERVICE);
    }

    public static DisplayCapabilityManager getInstance(Context context) {
        synchronized (OutputModeManager.class) {
            if (mDisplayCapabilityManager == null) {
                mDisplayCapabilityManager = new DisplayCapabilityManager(context);
            }
        }
        return mDisplayCapabilityManager;
    }

    private static final ImmutableMap<String, Display.Mode> USER_PREFERRED_MODE_BY_MODE =
            new ImmutableMap.Builder<String, Display.Mode>()
                    .put("2160p60hz", new Display.Mode(3840, 2160, 60.000004f))
                    .put("2160p59.94hz", new Display.Mode(3840, 2160, 59.94006f))
                    .put("2160p50hz", new Display.Mode(3840, 2160, 50.0f))
                    .put("2160p30hz", new Display.Mode(3840, 2160, 30.000002f))
                    .put("2160p29.97hz", new Display.Mode(3840, 2160, 29.97003f))
                    .put("2160p25hz", new Display.Mode(3840, 2160, 25.0f))
                    .put("2160p24hz", new Display.Mode(3840, 2160, 24.000002f))
                    .put("2160p23.976hz", new Display.Mode(3840, 2160, 23.976025f))
                    .put("smpte24hz", new Display.Mode(4096, 2160, 24.000002f))
                    .put("smpte23.976hz", new Display.Mode(4096, 2160, 23.976025f))
                    .put("1080p60hz", new Display.Mode(1920, 1080, 60.000004f))
                    .put("1080i60hz", new Display.Mode(1920, 1080, 60.000004f))
                    .put("1080p59.94hz", new Display.Mode(1920, 1080, 59.94006f))
                    .put("1080i59.94hz", new Display.Mode(1920, 1080, 59.94006f))
                    .put("1080p50hz", new Display.Mode(1920, 1080, 50.0f))
                    .put("1080i50hz", new Display.Mode(1920, 1080, 50.0f))
                    .put("1080p24hz", new Display.Mode(1920, 1080, 24.000002f))
                    .put("1080p23.976hz", new Display.Mode(1920, 1080, 23.976025f))
                    .put("720p60hz", new Display.Mode(1280, 720, 60.000004f))
                    .put("720p59.94hz", new Display.Mode(1280, 720, 59.94006f))
                    .put("720p50hz", new Display.Mode(1280, 720, 50.0f))
                    .put("576p50hz", new Display.Mode(720, 576, 50.0f))
                    .put("480p60hz", new Display.Mode(720, 480, 60.000004f))
                    .put("480p59.94hz", new Display.Mode(720, 480, 59.94006f))
                    .put("576cvbs", new Display.Mode(720, 576, 50.0f))
                    .put("480cvbs", new Display.Mode(720, 480, 60.000004f))
                    .put("pal_m", new Display.Mode(720, 480, 60.000004f))
                    .put("pal_n", new Display.Mode(720, 576, 50.0f))
                    .put("ntsc_m", new Display.Mode(720, 480, 60.000004f))
                    .build();

    private boolean isContainsInFW(String sysCtrlMode) {
        // CVBS mode does not do mode display contrast filtering.
        if (isCvbsMode()) {
            return true;
        }

        Display.Mode[] frameworkSupportedModes = mDisplayManager.getDisplay(0).getSupportedModes();
        LogUtils.d(TAG, "framework support mode: " + Arrays.toString(frameworkSupportedModes));
        Map<String, Display.Mode> modeMapTmp = USER_PREFERRED_MODE_BY_MODE;

        boolean matched = false;
        try {
            for (Display.Mode mode2 : frameworkSupportedModes) {
                if (mode2.matches(
                        Objects.requireNonNull(modeMapTmp.get(sysCtrlMode)).getPhysicalWidth(),
                        Objects.requireNonNull(modeMapTmp.get(sysCtrlMode)).getPhysicalHeight(),
                        Objects.requireNonNull(modeMapTmp.get(sysCtrlMode)).getRefreshRate())) {
                    matched = true;
                    break;
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to process framework support mode, " + e.getMessage());
        }

        return matched;
    }

    public void filterNoSupportMode(List<String> systemControlModeList) {
        Iterator<String> sysHdmiModeIterator = systemControlModeList.iterator();
        while (sysHdmiModeIterator.hasNext()) {
            String hdmiModeTmp = filterHdmiModes(sysHdmiModeIterator.next());
            if (!hdmiModeTmp.isEmpty() && !isContainsInFW(hdmiModeTmp)) {
                sysHdmiModeIterator.remove();
            }
        }
        LogUtils.d(TAG, "filterNoSupportModeList: " + systemControlModeList);
    }

    private String filterHdmiModes(String filterHdmiMode) {
        LogUtils.d(TAG, "filterHdmiMode: " + filterHdmiMode);
        try {
            if (!OutputModeManager.getInstance(mContext).getFrameRateOffset().contains("1")
                    || filterHdmiMode == null) {
                return filterHdmiMode;
            }
        } catch (NoClassDefFoundError e) {
            LogUtils.e(TAG, "filterHdmiModes: OutputModeManager call failed, " + e.getMessage());
            return filterHdmiMode;
        }

        String filterHdmiModeStr = filterHdmiMode;
        if (filterHdmiMode.contains("60Hz")) {
            filterHdmiModeStr = filterHdmiMode.replace("60Hz", "59.94Hz");
        } else if (filterHdmiMode.contains("30Hz")) {
            filterHdmiModeStr = filterHdmiMode.replace("30Hz", "29.97Hz");
        } else if (filterHdmiMode.contains("24Hz")) {
            filterHdmiModeStr = filterHdmiMode.replace("24Hz", "23.976Hz");
        } else if (filterHdmiMode.contains("60hz")) {
            filterHdmiModeStr = filterHdmiMode.replace("60hz", "59.94hz");
        } else if (filterHdmiMode.contains("30hz")) {
            filterHdmiModeStr = filterHdmiMode.replace("30hz", "29.97hz");
        } else if (filterHdmiMode.contains("24hz")) {
            filterHdmiModeStr = filterHdmiMode.replace("24hz", "23.976hz");
        }

        LogUtils.d(TAG, "filterHdmiModeStr: " + filterHdmiModeStr);
        return filterHdmiModeStr;
    }

    public boolean isCvbsMode() {
        String outputMode = "";
        try {
            outputMode = OutputModeManager.getInstance(mContext).getCurrentOutputMode();
        } catch (NoClassDefFoundError e) {
            LogUtils.e(TAG, "isCvbsMode: OutputModeManager call failed, " + e.getMessage());
        }
        return outputMode.contains("cvbs");
    }

    private Display.Mode checkUserPreferredMode(Display.Mode[] modeArr, Display.Mode mode) {
        for (Display.Mode mode2 : modeArr) {
        /* In cvbs mode, the value of the resolution rate reported by hwc to the framework is fake
        data (Google[b/229605079] needs to filter 16:9 mode), so only fps verification is performed in this mode
        (the width and height of cvbs mode are determined, only fps is unique).
        * */
            boolean refreshRate = (Float.floatToIntBits(mode2.getRefreshRate())
                    == Float.floatToIntBits(mode.getRefreshRate()));
            if ((isCvbsMode() && refreshRate)
                    || (!isCvbsMode()
                    && (mode2.matches(
                    mode.getPhysicalWidth(),
                    mode.getPhysicalHeight(),
                    mode.getRefreshRate())))) {
                return mode2;
            }
        }

        throw new IllegalArgumentException("Unrecognized user preferred mode, invalid mode!!");
    }

    private boolean checkSysCurrentMode(Display.Mode sysMode, Display.Mode userSetMode) {
        return sysMode.matches(
                userSetMode.getPhysicalWidth(),
                userSetMode.getPhysicalHeight(),
                userSetMode.getRefreshRate());
    }

    private Display.Mode getPreferredByMode(String userSetMode) {
        Map<String, Display.Mode> modeMap = USER_PREFERRED_MODE_BY_MODE;
        return modeMap.get(userSetMode);
    }

    public void setResolutionAndRefreshRateByMode(final String mode) {
        try {
            if (!DISPLAY_MODE_FALSE.equals(SystemControlManager.getInstance().getBootenv(ENV_IS_BEST_MODE, DISPLAY_MODE_TRUE))) {
                SystemControlManager.getInstance().setBootenv(ENV_IS_BEST_MODE, DISPLAY_MODE_FALSE);
            }
            setUserPreferredDisplayMode(mode);
        } catch (NoClassDefFoundError e) {
            LogUtils.e(TAG, "setResolutionAndRefreshRateByMode: SystemControlManager call failed, " + e.getMessage());
        }
    }

    private void setUserPreferredDisplayMode(String userSetMode) {
        LogUtils.d(TAG, "userSetMode: " + userSetMode);
        String mode = filterHdmiModes(userSetMode);

        // The framework filters when the system is at the current resolution, so use SystemControl to set it.
        // Note: Mode filtering is not required when the systemcontrol is used to set resolution
        boolean isSystemHdmiDispMode =
                checkSysCurrentMode(mDisplayManager.getDisplay(0).getMode(), getPreferredByMode(mode));
        if (isSystemHdmiDispMode) {
            LogUtils.d(TAG, "setMboxOutputMode");
            try {
                SystemControlManager.getInstance().setMboxOutputMode(userSetMode);
            } catch (NoClassDefFoundError e) {
                LogUtils.e(TAG, "setUserPreferredDisplayMode: SystemControlManager call failed, " + e.getMessage());
            }
            return;
        }

        Display.Mode[] supportedModes = mDisplayManager.getDisplay(0).getSupportedModes();
        Display.Mode matcherMode = checkUserPreferredMode(supportedModes, USER_PREFERRED_MODE_BY_MODE.get(mode));
        Display.Mode userPreferredDisplayMode = mDisplayManager.getUserPreferredDisplayMode();
        if (userPreferredDisplayMode != null) {
            LogUtils.d(TAG, "userPreferredDisplayMode: " + userPreferredDisplayMode);
            if (checkSysCurrentMode(userPreferredDisplayMode, matcherMode)) {
                mDisplayManager.clearUserPreferredDisplayMode();
            }
        }

        // set resolution
        LogUtils.d(TAG, "matcherMode: " + matcherMode);
        mDisplayManager.setUserPreferredDisplayMode(matcherMode);
    }
}
