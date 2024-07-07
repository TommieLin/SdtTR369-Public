package com.skyworth.scrrtcsrv;

import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.util.Log;

import org.webrtc.EglBase;
import org.webrtc.EglBase14;
import org.webrtc.Predicate;
import org.webrtc.VideoCodecInfo;
import org.webrtc.VideoEncoder;
import org.webrtc.VideoEncoderFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import androidx.annotation.Nullable;

class HardwareVideoEncoderFactory implements VideoEncoderFactory {
    private static final String TAG = "TR369 HardwareVideoEncoderFactory";
    private static final List<String> H264_HW_EXCEPTION_MODELS = Arrays.asList("SAMSUNG-SGH-I337", "Nexus 7", "Nexus 4");

    @Nullable
    private final EglBase14.Context sharedContext;
    private final boolean enableIntelVp8Encoder;
    private final boolean enableH264HighProfile;
    @Nullable
    private final Predicate<MediaCodecInfo> codecAllowedPredicate;

    public HardwareVideoEncoderFactory(EglBase.Context sharedContext, boolean enableIntelVp8Encoder, boolean enableH264HighProfile) {
        this(sharedContext, enableIntelVp8Encoder, enableH264HighProfile, (Predicate) null);
    }

    public HardwareVideoEncoderFactory(EglBase.Context sharedContext, boolean enableIntelVp8Encoder, boolean enableH264HighProfile, @Nullable Predicate<MediaCodecInfo> codecAllowedPredicate) {
        if (sharedContext instanceof EglBase14.Context) {
            this.sharedContext = (EglBase14.Context) sharedContext;
        } else {
            Log.w(TAG, "No shared EglBase.Context. Encoders will not use texture mode.");
            this.sharedContext = null;
        }

        this.enableIntelVp8Encoder = enableIntelVp8Encoder;
        this.enableH264HighProfile = enableH264HighProfile;
        this.codecAllowedPredicate = codecAllowedPredicate;
    }

    @Nullable
    public VideoEncoder createEncoder(VideoCodecInfo input) {
        VideoCodecMimeType type = VideoCodecMimeType.valueOf(input.name);
        MediaCodecInfo info = this.findCodecForType(type);
        if (info == null) {
            return null;
        }
        String codecName = info.getName();
        String mime = type.mimeType();
        Integer surfaceColorFormat = MediaCodecUtils.selectColorFormat(MediaCodecUtils.TEXTURE_COLOR_FORMATS, info.getCapabilitiesForType(mime));
        Integer yuvColorFormat = MediaCodecUtils.selectColorFormat(MediaCodecUtils.ENCODER_COLOR_FORMATS, info.getCapabilitiesForType(mime));
        if (type == VideoCodecMimeType.H264) {
            boolean isHighProfile = H264Utils.isSameH264Profile(input.params, MediaCodecUtils.getCodecProperties(type, true));
            boolean isBaselineProfile = H264Utils.isSameH264Profile(input.params, MediaCodecUtils.getCodecProperties(type, false));
            if (!isHighProfile && !isBaselineProfile) {
                return null;
            }

            if (isHighProfile && !this.isH264HighProfileSupported(info)) {
                return null;
            }
        }

        return new HardwareVideoEncoder(new MediaCodecWrapperFactoryImpl(), codecName, type, surfaceColorFormat, yuvColorFormat, input.params, this.getKeyFrameIntervalSec(type), this.getForcedKeyFrameIntervalMs(type, codecName), this.createBitrateAdjuster(type, codecName), this.sharedContext);
    }

    public VideoCodecInfo[] getSupportedCodecs() {

        List<VideoCodecInfo> supportedCodecInfos = new ArrayList<>();
        VideoCodecMimeType[] mimeTypes = new VideoCodecMimeType[]{VideoCodecMimeType.VP8, VideoCodecMimeType.VP9, VideoCodecMimeType.H264};
        int length = mimeTypes.length;

        for (int i = 0; i < length; ++i) {
            VideoCodecMimeType type = mimeTypes[i];
            MediaCodecInfo codec = this.findCodecForType(type);
            if (codec != null) {
                String name = type.name();
                if (type == VideoCodecMimeType.H264 && this.isH264HighProfileSupported(codec)) {
                    supportedCodecInfos.add(new VideoCodecInfo(name, MediaCodecUtils.getCodecProperties(type, true)));
                }

                supportedCodecInfos.add(new VideoCodecInfo(name, MediaCodecUtils.getCodecProperties(type, false)));
            }
        }

        return supportedCodecInfos.toArray(new VideoCodecInfo[supportedCodecInfos.size()]);
    }

    @Nullable
    private MediaCodecInfo findCodecForType(VideoCodecMimeType type) {
        for (int i = 0; i < MediaCodecList.getCodecCount(); ++i) {
            MediaCodecInfo info = null;

            try {
                info = MediaCodecList.getCodecInfoAt(i);
            } catch (IllegalArgumentException var5) {
                Log.e("HardwareVideoEncoderFactory", "Cannot retrieve encoder codec info", var5);
            }

            if (info != null && info.isEncoder() && this.isSupportedCodec(info, type)) {
                return info;
            }
        }

        return null;
    }

    private boolean isSupportedCodec(MediaCodecInfo info, VideoCodecMimeType type) {
        if (!MediaCodecUtils.codecSupportsType(info, type)) {
            return false;
        } else if (MediaCodecUtils.selectColorFormat(MediaCodecUtils.ENCODER_COLOR_FORMATS, info.getCapabilitiesForType(type.mimeType())) == null) {
            return false;
        } else {
            return this.isHardwareSupportedInCurrentSdk(info, type) && this.isMediaCodecAllowed(info);
        }
    }

    private boolean isHardwareSupportedInCurrentSdk(MediaCodecInfo info, VideoCodecMimeType type) {
        switch (type) {
            case VP8:
                return this.isHardwareSupportedInCurrentSdkVp8(info);
            case VP9:
                return this.isHardwareSupportedInCurrentSdkVp9(info);
            case H264:
                return this.isHardwareSupportedInCurrentSdkH264(info);
            default:
                return false;
        }
    }

    private boolean isHardwareSupportedInCurrentSdkVp8(MediaCodecInfo info) {
        String name = info.getName();
        return name.startsWith(MediaCodecUtils.QCOM_PREFIX)
                || name.startsWith(MediaCodecUtils.EXYNOS_PREFIX)
                || name.startsWith(MediaCodecUtils.INTEL_PREFIX) && this.enableIntelVp8Encoder;
    }

    private boolean isHardwareSupportedInCurrentSdkVp9(MediaCodecInfo info) {
        String name = info.getName();
        return name.startsWith(MediaCodecUtils.QCOM_PREFIX)
                || name.startsWith(MediaCodecUtils.EXYNOS_PREFIX);
    }

    private boolean isHardwareSupportedInCurrentSdkH264(MediaCodecInfo info) {
        if (H264_HW_EXCEPTION_MODELS.contains(Build.MODEL)) {
            return false;
        } else {
            String name = info.getName();
            return name.startsWith(MediaCodecUtils.QCOM_PREFIX)
                    || name.startsWith(MediaCodecUtils.EXYNOS_PREFIX)
                    || name.startsWith(MediaCodecUtils.AML_PREFIX);
        }
    }

    private boolean isMediaCodecAllowed(MediaCodecInfo info) {
        return this.codecAllowedPredicate == null || this.codecAllowedPredicate.test(info);
    }

    private int getKeyFrameIntervalSec(VideoCodecMimeType type) {
        switch (type) {
            case VP8:
            case VP9:
                return 100;
            case H264:
                return 20;
            default:
                throw new IllegalArgumentException("Unsupported VideoCodecMimeType " + type);
        }
    }

    private int getForcedKeyFrameIntervalMs(VideoCodecMimeType type, String codecName) {
        if (type == VideoCodecMimeType.VP8 && codecName.startsWith(MediaCodecUtils.QCOM_PREFIX)) {
            return 15000;
        }
        return 0;
    }

    private BitrateAdjuster createBitrateAdjuster(VideoCodecMimeType type, String codecName) {
        if (codecName.startsWith(MediaCodecUtils.EXYNOS_PREFIX)) {
            return type == VideoCodecMimeType.VP8 ? new DynamicBitrateAdjuster() : new FramerateBitrateAdjuster();
        } else {
            return new BaseBitrateAdjuster();
        }
    }

    private boolean isH264HighProfileSupported(MediaCodecInfo info) {
        return this.enableH264HighProfile
                && (info.getName().startsWith(MediaCodecUtils.EXYNOS_PREFIX)
                || info.getName().startsWith(MediaCodecUtils.AML_PREFIX));
    }
}
