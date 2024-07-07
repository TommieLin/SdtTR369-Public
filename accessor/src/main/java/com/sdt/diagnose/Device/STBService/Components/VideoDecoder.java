package com.sdt.diagnose.Device.STBService.Components;

import com.sdt.annotations.Tr369Get;
import com.sdt.diagnose.common.FileUtils;
import com.sdt.diagnose.common.log.LogUtils;

public class VideoDecoder {
    private static final String TAG = "VideoDecoder";
    private static final String KEY_WORD_DEV_NAME = "device name: ";
    private static final String KEY_WORD_FRAME_WIDTH = "frame width: ";

    private static final String FILE_NODE_VDEC_STATUS = "/sys/class/vdec/vdec_status";
    private static final String FILE_NODE_VDEC_PROFILE = "/sys/class/vdec/profile_idc";
    private static final String FILE_NODE_VDEC_LEVEL = "/sys/class/vdec/level_idc";
    private static final String FILE_NODE_ASPECT_RATIO = "/sys/class/video/frame_aspect_ratio";

    private static final String ERROR_VDEC_NOT_SUPPORT = "Not Support";
    private static final String ERROR_VDEC_EMPTY_LIST = "connected vdec list empty";
    private static final String[] MPEG2_PART2_PROFILE =
            new String[]{"Simple Profile", "Main Profile"};
    private static final String[] MPEG2_PART2_LEVEL =
            new String[]{"Low Level", "Main Level", "High-1440", "High Level"};
    private static final String[] MPEG4_PART2_PROFILE =
            new String[]{"Simple Profile", "Simple Profile"};
    private static final String[] MPEG4_PART2_LEVEL =
            new String[]{"L0", "L0B", "L1", "L2", "L3", "L4a", "L5", "L6"};
    private static final String[] MPEG4_PART10_PROFILE =
            new String[]{"Baseline Profile", "Main Profile", "Extended Profile", "High Profile"};
    private static final String[] MPEG4_PART10_LEVEL =
            new String[]{"1", "1b", "1.1", "1.2", "1.3", "2", "2.1", "2.2", "3", "3.1", "3.2", "4", "4.1",
                    "4.2", "5", "5.1"};


    private int getProfileIndex(String[] profilesArray) {
        int curIndex = -1;
        String profileContent = FileUtils.readFileToStr(FILE_NODE_VDEC_PROFILE);
        LogUtils.d(TAG, "profileContent: " + profileContent);
        if (null != profileContent && profilesArray != null) {
            if (!profileContent.contains(ERROR_VDEC_NOT_SUPPORT) && !profileContent
                    .contains(ERROR_VDEC_EMPTY_LIST)) {
                for (int i = 0; i < profilesArray.length; i++) {
                    if (profileContent.contains(profilesArray[i])) {
                        curIndex = i;
                    }
                }
            }
        }
        return curIndex;
    }

    private int getLevelIndex(String[] levelsArray) {
        int curLevelIndex = -1;
        String levelContent = FileUtils.readFileToStr(FILE_NODE_VDEC_LEVEL);
        LogUtils.d(TAG, "levelContent: " + levelContent);
        if (null != levelContent && levelsArray != null) {
            if (!levelContent.contains(ERROR_VDEC_NOT_SUPPORT) && !levelContent
                    .contains(ERROR_VDEC_EMPTY_LIST)) {
                for (int i = 0; i < levelsArray.length; i++) {
                    if (levelContent.contains(levelsArray[i])) {
                        curLevelIndex = i;
                    }
                }
            }
        }
        return curLevelIndex;
    }

    @Tr369Get("Device.Services.STBService.1.Components.VideoDecoder.1.Name")
    public String SK_TR369_GetVdecName() {
        String videoDecoderName = "";
        String decoderInfo = FileUtils.readFileToStr(FILE_NODE_VDEC_STATUS);
        if (null != decoderInfo && !decoderInfo.contains("No vdec")) {
            int Start = decoderInfo.indexOf("device name");
            int EndIndex = decoderInfo.indexOf("frame width");
            String nameStr = decoderInfo.substring(Start, EndIndex);
            int nameEnd = nameStr.indexOf("\n");
            videoDecoderName = nameStr.substring(KEY_WORD_DEV_NAME.length(), nameEnd - 1);
        }
        LogUtils.d(TAG, "videoDecoderName: " + videoDecoderName);
        return videoDecoderName;
    }

    @Tr369Get("Device.Services.STBService.1.Components.VideoDecoder.1.Status")
    public String SK_TR369_GetVdecStatus() {
        String videoDecoderStatus = "";
        String decoderInfo = FileUtils.readFileToStr(FILE_NODE_VDEC_STATUS);
        if (null != decoderInfo) {
            if (decoderInfo.contains("No vdec")) {
                videoDecoderStatus = "Disabled";
            } else {
                videoDecoderStatus = "Enabled";
            }
        }
        return videoDecoderStatus;
    }

    @Tr369Get("Device.Services.STBService.1.Components.VideoDecoder.1.MPEG2Part2")
    public String SK_TR369_GetVdecMpeg2Part2() {
        String profileLevelValue = "";
        int profileLevelIndex = -1;
        int curProfileIndex = getProfileIndex(MPEG2_PART2_PROFILE);
        if (-1 != curProfileIndex) {
            int curLevelIndex = getLevelIndex(MPEG2_PART2_LEVEL);
            if (curLevelIndex != -1) {
                profileLevelIndex =
                        curProfileIndex * MPEG2_PART2_LEVEL.length + curLevelIndex + 1;
                profileLevelValue = String.valueOf(profileLevelIndex);
            }
        }

        return profileLevelValue;
    }

    @Tr369Get("Device.Services.STBService.1.Components.VideoDecoder.1.MPEG4Part2")
    public String SK_TR369_GetVdecMpeg4Part2() {
        String profileLevelValue = "";
        int profileLevelIndex = -1;
        int curProfileIndex = getProfileIndex(MPEG4_PART2_PROFILE);
        if (-1 != curProfileIndex) {
            int curLevelIndex = getLevelIndex(MPEG4_PART2_LEVEL);
            if (curLevelIndex != -1) {
                profileLevelIndex =
                        curProfileIndex * MPEG4_PART2_LEVEL.length + curLevelIndex + 1;
                profileLevelValue = String.valueOf(profileLevelIndex);
            }
        }

        return profileLevelValue;
    }

    @Tr369Get("Device.Services.STBService.1.Components.VideoDecoder.1.MPEG4Part10")
    public String SK_TR369_GetVdecMpeg4Part10() {
        String profileLevelValue = "";
        int profileLevelIndex = -1;
        int curProfileIndex = getProfileIndex(MPEG4_PART10_PROFILE);
        if (-1 != curProfileIndex) {
            int curLevelIndex = getLevelIndex(MPEG4_PART10_LEVEL);
            if (curLevelIndex != -1) {
                profileLevelIndex =
                        curProfileIndex * MPEG4_PART10_LEVEL.length + curLevelIndex + 1;
                profileLevelValue = String.valueOf(profileLevelIndex);
            }
        }

        return profileLevelValue;
    }

    @Tr369Get("Device.Services.STBService.1.Components.VideoDecoder.1.ContentAspectRatio")
    public String SK_TR369_GetVdecAspectRatio() {
        String aspectRatio = FileUtils.readFileToStr(FILE_NODE_ASPECT_RATIO);
        LogUtils.d(TAG, "GetVdecAspectRatio: " + aspectRatio);
        if (null == aspectRatio || aspectRatio.contains("NA")) {
            return "";
        } else {
            return aspectRatio.replace("\n", "");
        }
    }

}
