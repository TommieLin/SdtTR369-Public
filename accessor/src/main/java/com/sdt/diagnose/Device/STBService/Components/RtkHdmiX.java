package com.sdt.diagnose.Device.STBService.Components;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.provider.Settings;
import android.view.Display;

import androidx.annotation.NonNull;

import com.realtek.hardware.OutputFormat;
import com.realtek.hardware.RtkHDMIManager2;
import com.realtek.hardware.RtkTVSystem;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.log.LogUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Pattern;

public class RtkHdmiX {
    private static final String TAG = "RtkHdmiX";
    private static Context mContext;
    private static RtkHdmiX mRtkHdmiX;
    private final Vector<RtkHDMIManager2.TVSystemInfo> sTVSystemInfos = RtkHDMIManager2.getTVSystemInfos();

    RtkHdmiX(Context context) {
        mContext = context;
    }

    public static RtkHdmiX getInstance(@NonNull Context context) {
        if (null == mRtkHdmiX) {
            mRtkHdmiX = new RtkHdmiX(context);
        }
        return mRtkHdmiX;
    }

    public static RtkHdmiX getInstance() {
        if (null == mRtkHdmiX) {
            mRtkHdmiX = new RtkHdmiX(GlobalContext.getContext());
        }
        return mRtkHdmiX;
    }

    public boolean isHDMIPluggedByRtk() {
        boolean isPlugged = false;
        try {
            isPlugged = RtkHDMIManager2.getRtkHDMIManager(mContext).checkIfHDMIPlugged();
        } catch (NoClassDefFoundError e) {
            LogUtils.e(TAG, "isHDMIPluggedByRtk: RtkHDMIManager2 call failed, " + e.getMessage());
        }
        return isPlugged;
    }

    public String getHdmiEnableByRtk() {
        String status = Boolean.toString(false);
        try {
            boolean isEnabled = RtkHDMIManager2.getRtkHDMIManager(mContext).hdmiIsEnabled();
            status = Boolean.toString(isEnabled);
        } catch (NoClassDefFoundError e) {
            LogUtils.e(TAG, "getHdmiEnableByRtk: RtkHDMIManager2 call failed, " + e.getMessage());
        }
        return status;
    }

    public boolean setHdmiEnableByRtk(boolean isEnable) {
        try {
            RtkHDMIManager2.getRtkHDMIManager(mContext).setHDMIEnable(isEnable);
        } catch (NoClassDefFoundError e) {
            LogUtils.e(TAG, "setHdmiEnableByRtk: RtkHDMIManager2 call failed, " + e.getMessage());
            return false;
        }
        return true;
    }

    public boolean getHdmiResolutionModeByRtk() {
        boolean isBest = false;
        try {
            OutputFormat currentOutputFormat =
                    RtkHDMIManager2.getRtkHDMIManager(mContext).getCurrentOutputFormat();
            OutputFormat autoOutputFormat =
                    RtkHDMIManager2.getRtkHDMIManager(mContext).getResolvedAutoOutputFormat();
            if (currentOutputFormat.mVIC == autoOutputFormat.mVIC) {
                isBest = true;
            }
        } catch (NoClassDefFoundError e) {
            LogUtils.e(TAG, "getHdmiResolutionModeByRtk: RtkHDMIManager2 call failed, " + e.getMessage());
        }
        return isBest;
    }

    public String getHdmiNameByRtk() {
        String name = "";
        try {
            name = RtkHDMIManager2.getRtkHDMIManager(mContext).getHDMIProductInfo();
        } catch (NoClassDefFoundError e) {
            LogUtils.e(TAG, "getHdmiNameByRtk: RtkHDMIManager2 call failed, " + e.getMessage());
        }
        return name;
    }

    public String getHdmiResolutionValueByRtk() {
        String mode = "";
        try {
            OutputFormat curOutputFormat =
                    RtkHDMIManager2.getRtkHDMIManager(mContext).getCurrentOutputFormat();
            LogUtils.d(TAG, "getHdmiResolutionValueByRtk curOutputFormat"
                    + ": mVIC: " + curOutputFormat.mVIC
                    + ", mFreqShift: " + curOutputFormat.mFreqShift
                    + ", mColor: " + curOutputFormat.mColor
                    + ", mColorDepth: " + curOutputFormat.mColorDepth
                    + ", m3DFormat: " + curOutputFormat.m3DFormat
                    + ", mHDR: " + curOutputFormat.mHDR);
            RtkHDMIManager2.TVSystemInfo info =
                    RtkHDMIManager2.getRtkHDMIManager(mContext)
                            .findTVSystemInfoViaOutputFormat(curOutputFormat);
            if (info.mSettingValue == 1) {
                mode = "NTSC";
            } else if (info.mSettingValue == 2) {
                mode = "PAL";
            } else if (info.mSettingValue == 3) {
                mode = "480P";
            } else if (info.mSettingValue == 4) {
                mode = "576P";
            } else {
                int Fps = (info.mFreqShift == 0) ? info.mFps : info.mFps - 1;
                if (info.mHeight < 2160) {
                    if (!info.mProgress) {
                        mode = info.mHeight + "I @ " + Fps + "Hz";
                    } else {
                        mode = info.mHeight + "P @ " + Fps + "Hz";
                    }
                } else {
                    mode = info.mWidth + "x" + info.mHeight + "P @ " + Fps + "Hz";
                }
            }
        } catch (NoClassDefFoundError e) {
            LogUtils.e(TAG, "getHdmiResolutionValueByRtk: RtkHDMIManager2 call failed, " + e.getMessage());
        }
        return mode;
    }

    public boolean setHdmiResolutionValueByRtk(String mode) {
        int value = getTVSystemValueViaString(mode);
        LogUtils.d(TAG, "setHdmiResolutionValueByRtk value: " + value);
        if (value >= 0) setHDMITVSystem(value);
        return true;
    }

    private int getTVSystemValueViaString(String mode) {
        if (mode.contains("AUTO")) return 0;

        int fps, height, width = 0, freqShift = 0;
        boolean isProgress = mode.contains("P");
        boolean is2160p = false;

        try {
            if (mode.contains("x")) {
                is2160p = true;
                width = Integer.parseInt(mode.split("x")[0]);
                height = Integer.parseInt(mode.split("x")[1].split("P")[0]);
            } else if (isProgress) {
                height = Integer.parseInt(mode.split("P")[0]);
            } else {
                height = Integer.parseInt(mode.split("I")[0]);
            }
            fps = Integer.parseInt(mode.split("@ ")[1].split("Hz")[0]);
        } catch (Exception e) {
            LogUtils.e(TAG, "getTVSystemValueViaString execute failed, " + e.getMessage());
            return -1;
        }

        if (fps == 23 || fps == 29 || fps == 59) {
            fps += 1;
            freqShift = 1;
        }
        LogUtils.d(TAG, "getTVSystemValueViaString height: " + height
                + ", width: " + width
                + ", fps: " + fps
                + ", freqShift: " + freqShift
                + ", isProgress: " + isProgress
                + ", is2160p: " + is2160p);

        for (int i = 0; i < sTVSystemInfos.size(); i++) {
            RtkHDMIManager2.TVSystemInfo info = (RtkHDMIManager2.TVSystemInfo) sTVSystemInfos.get(i);
            if ((!is2160p || info.mWidth == width)
                    && info.mHeight == height
                    && info.mProgress == isProgress
                    && info.mFps == fps
                    && info.mFreqShift == freqShift) {
                return info.mSettingValue;
            }
        }
        return -1;
    }

    /* Realtek API */
    private void updateUserPreferredDisplayMode(int width, int height, int fps) {
        DisplayManager displayManager =
                (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
        Display display = displayManager.getDisplay(Display.DEFAULT_DISPLAY);
        Display.Mode[] modes = display.getSupportedModes();
        Display.Mode mode = null;
        for (Display.Mode value : modes) {
            int w = value.getPhysicalWidth();
            int h = value.getPhysicalHeight();
            int f = (int) value.getRefreshRate();
            if (width == w && height == h && fps == f) {
                mode = value;
            }
        }

        /* update setting */
        if (mode != null) {
            /* check existing user preferred display mode */
            Display.Mode currentMode = displayManager.getUserPreferredDisplayMode();
            if (currentMode != null) {
                LogUtils.d(TAG, "current mode: " + currentMode);
            }
            LogUtils.d(TAG, "updateUserPreferredDisplayMode mode: " + mode);
            displayManager.setUserPreferredDisplayMode(mode);
        } else {
            LogUtils.d(TAG, "updateUserPreferredDisplayMode clear mode");
            displayManager.clearUserPreferredDisplayMode();
        }
    }

    private boolean checkTargetTVSystem(RtkHDMIManager2.TVSystemInfo tInfo) {
        OutputFormat fmt = RtkHDMIManager2.getRtkHDMIManager(mContext).getCurrentOutputFormat();
        RtkHDMIManager2.TVSystemInfo cInfo =
                RtkHDMIManager2.getRtkHDMIManager(mContext).findTVSystemInfoViaOutputFormat(fmt);
        return tInfo.matchDisplayMode(cInfo);
    }

    /* set hdmi, and update setting keys,
     * refer to original implementation to update
     * nts related flags */
    private void setHDMITVSystem(int value) {
        RtkHDMIManager2.getRtkHDMIManager(mContext).setupSettingFlag(1);

        int edidAuto = RtkTVSystem.TV_SYSTEM_AUTO_MODE_HDMI_EDID;
        int edidManual = RtkTVSystem.TV_SYSTEM_AUTO_MODE_OFF;
        int ntsUndefined = RtkHDMIManager2.NTS_VALUE_UNDEFINED;

        if (value == RtkTVSystem.TV_SYS_HDMI_AUTO_DETECT) {
            RtkHDMIManager2.getRtkHDMIManager(mContext).setupGPTValues(
                    edidAuto,
                    ntsUndefined,
                    ntsUndefined);

            OutputFormat fmt =
                    RtkHDMIManager2.getRtkHDMIManager(mContext).getResolvedAutoOutputFormat();
            RtkHDMIManager2.TVSystemInfo info =
                    RtkHDMIManager2.getRtkHDMIManager(mContext).findTVSystemInfoViaOutputFormat(fmt);
            if (info != null) {
                int w = info.mWidth;
                int h = info.mHeight;
                int fps = info.mFps2;
                boolean progressive = info.mProgress;

                boolean useRtkAPI = checkTargetTVSystem(info);
                if (useRtkAPI) {
                    LogUtils.d(TAG, "useRtkAPI to switch to " + fmt.mVIC + " fs: " + fmt.mFreqShift);
                    RtkHDMIManager2.getRtkHDMIManager(mContext).setOutputFormat2(fmt);
                } else {
                    LogUtils.d(TAG, "Not using the Rtk API.");
                    if (!progressive) {
                        RtkHDMIManager2.getRtkHDMIManager(mContext).setupInterlaceFlag();
                    }
                    updateUserPreferredDisplayMode(w, h, fps);
                }
            }
        } else {

            /* none auto detect mode, resolve vic via value */
            RtkHDMIManager2.TVSystemInfo info =
                    RtkHDMIManager2.getRtkHDMIManager(mContext).findTVSystemInfo(value);
            LogUtils.d(TAG, "setHDMITVSystem info: " + info);
            if (info != null) {
                int w = info.mWidth;
                int h = info.mHeight;
                int fps = info.mFps2;

                int vic = info.mVIC;
                int fs = info.mFreqShift;
                boolean progressive = info.mProgress;

                /* switch to edid manual mode */
                RtkHDMIManager2.getRtkHDMIManager(mContext).setupGPTValues(
                        edidManual,
                        vic,
                        fs);

                boolean useRtkAPI = checkTargetTVSystem(info);
                if (useRtkAPI) {
                    LogUtils.d(TAG, "Requires the use of the Rtk API.");
                    OutputFormat cfmt =
                            RtkHDMIManager2.getRtkHDMIManager(mContext).getCurrentOutputFormat();
                    int hdr = cfmt.mHDR;
                    int color = RtkHDMIManager2.ColorNone;
                    int colorDepth = RtkHDMIManager2.DepthNone;
                    int _3DFormat = 0;

                    int flags = RtkHDMIManager2.EXTRA_IGNORE_CURRENT
                            | RtkHDMIManager2.EXTRA_SAVE_TO_FACTORY;

                    LogUtils.d(TAG, "useRtkAPI setOutputFormat"
                            + " vic: " + vic
                            + " fs: " + fs
                            + " color: " + color
                            + " colorDepth: " + colorDepth
                            + " hdr: " + hdr
                            + " _3DFormat: " + _3DFormat
                            + " flags: " + flags);
                    RtkHDMIManager2.getRtkHDMIManager(mContext).setOutputFormat(
                            vic,
                            fs,
                            color,
                            colorDepth,
                            hdr,
                            _3DFormat,
                            flags);

                } else {
                    LogUtils.d(TAG, "Not using the Rtk API.");
                    if (!progressive) {
                        RtkHDMIManager2.getRtkHDMIManager(mContext).setupInterlaceFlag();
                    }
                    updateUserPreferredDisplayMode(w, h, fps);
                }
            }
        }
    }

    private boolean AvailableTvSystemsSort(CharSequence Tvsystem1, CharSequence Tvsystem2) {
        String Tv1 = Tvsystem1.toString();
        String Tv2 = Tvsystem2.toString();
        if (Tv1.matches("[a-zA-Z]+") && Tv2.matches("[a-zA-Z]+")) {
            if (Tv1.charAt(0) > Tv2.charAt(0))
                return true;
        }

        String REGEX = "[^0-9]";
        Tv1 = Pattern.compile(REGEX).matcher(Tv1).replaceAll("").trim();
        Tv2 = Pattern.compile(REGEX).matcher(Tv2).replaceAll("").trim();
        if (Tv1.length() == 0)
            return false;
        if (Tv2.length() == 0)
            return true;
        return Long.parseLong(Tv1) > Long.parseLong(Tv2);
    }

    private <T> void swap(T[] a, int i, int j) {
        T t = a[i];
        a[i] = a[j];
        a[j] = t;
    }

    public List<String> getHdmiSupportListByRtk() {
        int TvSystemSize = sTVSystemInfos.size();
        LogUtils.d(TAG, "getHdmiSupportListByRtk TvSystemSize: " + TvSystemSize);
        String[][] availableTvSystems = new String[TvSystemSize + 1][2];
        availableTvSystems[TvSystemSize][0] = "AUTO";
        availableTvSystems[TvSystemSize][1] = "0";
        for (int i = 0; i < TvSystemSize; i++) {
            RtkHDMIManager2.TVSystemInfo info = sTVSystemInfos.get(i);
            if (info.mSettingValue == 1) {
                availableTvSystems[i][0] = "NTSC";
            } else if (info.mSettingValue == 2) {
                availableTvSystems[i][0] = "PAL";
            } else if (info.mSettingValue == 3) {
                availableTvSystems[i][0] = "480P";
            } else if (info.mSettingValue == 4) {
                availableTvSystems[i][0] = "576P";
            } else {
                int Fps = (info.mFreqShift == 0) ? info.mFps : info.mFps - 1;
                if (info.mHeight < 2160) {
                    if (!info.mProgress) {
                        availableTvSystems[i][0] = info.mHeight + "I @ " + Fps + "Hz";
                    } else {
                        availableTvSystems[i][0] = info.mHeight + "P @ " + Fps + "Hz";
                    }
                } else {
                    availableTvSystems[i][0] =
                            info.mWidth + "x" + info.mHeight + "P @ " + Fps + "Hz";
                }
            }
            availableTvSystems[i][1] = String.valueOf(info.mSettingValue);
            LogUtils.d(TAG, "@@ availableTvSystems[" + i + "][0]: " + availableTvSystems[i][0]
                    + ", availableTvSystems[" + i + "][1]: " + availableTvSystems[i][1]);
        }
        for (int i = 0; i < availableTvSystems.length; i++) {
            for (int j = 1; j < availableTvSystems.length - i; j++) {
                if (AvailableTvSystemsSort(availableTvSystems[j - 1][0], availableTvSystems[j][0]))
                    swap(availableTvSystems, j - 1, j);
            }
        }

        for (int i = 0; i < TvSystemSize; i++) {
            LogUtils.d(TAG, "&& availableTvSystems[" + i + "][0]: " + availableTvSystems[i][0]
                    + ", availableTvSystems[" + i + "][1]: " + availableTvSystems[i][1]);
        }

        int[] supportVideoFormats = RtkHDMIManager2.getRtkHDMIManager(mContext).getVideoFormat();
        int numSupportVideoFormat = 0;

        LogUtils.d(TAG, "supportVideoFormat: " + supportVideoFormats.length
                + ", numSupportVideoFormat: " + numSupportVideoFormat);

        for (int supportVideoFormat : supportVideoFormats) {
            if (supportVideoFormat == 1)
                numSupportVideoFormat++;
        }
        LogUtils.d(TAG, "Num of SupoortVideoFormat: " + numSupportVideoFormat);

        final List<String> listHdmiMode = new ArrayList<>();
        final List<String> listHdmiModeValues = new ArrayList<>();

        for (String[] availableTvSystem : availableTvSystems) {
            int tvSysId = Integer.parseInt(availableTvSystem[1]);
            if ((tvSysId < supportVideoFormats.length) && (supportVideoFormats[tvSysId] == 1)) {
                // 拿掉低于1280x720P50Hz的分辨率
                if (tvSysId == 1   // NTSC
                        || tvSysId == 2   // PAL
                        || tvSysId == 3   // 480P
                        || tvSysId == 4   // 576P
                        || tvSysId == 39  // 1280x720P @ 24Hz
                        || tvSysId == 42  // 1280x720P @ 23Hz
                ) {
                    continue;
                }
                listHdmiMode.add(availableTvSystem[0]);
                listHdmiModeValues.add(availableTvSystem[1]);
            }
        }

        for (int i = 0; i < listHdmiMode.size(); i++) {
            LogUtils.d(TAG, "**** " + listHdmiMode.get(i) + " : " + listHdmiModeValues.get(i) + " ****");
        }
        return listHdmiMode;
    }

    public String getHdmiEdidByRtk() {
        return "HDMI version: 1.x";
    }

    public boolean getCapHdmiCecSupportByRtk() {
        int enabled = Settings.Global.getInt(mContext.getContentResolver(),
                "hdmi_control_enabled", 0);
        return (enabled == 1);
    }

}
