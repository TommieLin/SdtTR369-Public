package com.skyworth.scrrtcsrv;

import org.webrtc.VideoCodecInfo;

import java.util.HashMap;
import java.util.Map;

class H264Utils {
    public static final String H264_FMTP_PROFILE_LEVEL_ID = "profile-level-id";
    public static final String H264_FMTP_LEVEL_ASYMMETRY_ALLOWED = "level-asymmetry-allowed";
    public static final String H264_FMTP_PACKETIZATION_MODE = "packetization-mode";
    public static final String H264_PROFILE_CONSTRAINED_BASELINE = "42e0";
    public static final String H264_PROFILE_CONSTRAINED_HIGH = "640c";
    public static final String H264_LEVEL_3_1 = "1f";
    public static final String H264_CONSTRAINED_HIGH_3_1 = "640c1f";
    public static final String H264_CONSTRAINED_BASELINE_3_1 = "42e01f";
    public static VideoCodecInfo DEFAULT_H264_BASELINE_PROFILE_CODEC = new VideoCodecInfo("H264", getDefaultH264Params(false));
    public static VideoCodecInfo DEFAULT_H264_HIGH_PROFILE_CODEC = new VideoCodecInfo("H264", getDefaultH264Params(true));

    H264Utils() {
    }

    public static Map<String, String> getDefaultH264Params(boolean isHighProfile) {
        Map<String, String> params = new HashMap<>();
        params.put(H264_FMTP_LEVEL_ASYMMETRY_ALLOWED, "1");
        params.put(H264_FMTP_PACKETIZATION_MODE, "1");
        params.put(H264_FMTP_PROFILE_LEVEL_ID, isHighProfile ? H264_CONSTRAINED_HIGH_3_1 : H264_CONSTRAINED_BASELINE_3_1);
        return params;
    }

    public static boolean isSameH264Profile(Map<String, String> params1, Map<String, String> params2) {
        String profile_level_id1 = params1.get(H264_FMTP_PROFILE_LEVEL_ID);
        String profile_level_id2 = params2.get(H264_FMTP_PROFILE_LEVEL_ID);
        if (profile_level_id1 == null && profile_level_id2 == null) {
            return true;
        }
        if (profile_level_id1 == null || profile_level_id2 == null) {
            return false;
        }
        H264Profile profile1 = parseH264Profile(profile_level_id1);
        H264Profile profile2 = parseH264Profile(profile_level_id2);
        return profile1 == profile2 && profile1 != H264Profile.kProfileNone;
    }

    private enum H264Profile {
        kProfileNone,
        kProfileConstrainedBaseline,
        kProfileBaseline,
        kProfileMain,
        kProfileConstrainedHigh,
        kProfileHigh,
        kProfilePredictiveHigh444,
    }

    private static class BitPattern {
        private final byte mask;
        private final byte mask_value;

        public BitPattern(String str) {
            mask = (byte) ~byteMaskString((byte) 'x', str.getBytes());
            mask_value = byteMaskString((byte) '1', str.getBytes());
        }

        public boolean isMatch(byte value) {
            return mask_value == (value & mask);
        }
    }

    // Table for converting between profile_idc/profile_iop to H264Profile.
    private static class ProfilePattern {
        public final byte profile_idc;
        public final BitPattern profile_iop;
        public final H264Profile profile;

        public ProfilePattern(byte idc, BitPattern iop, H264Profile profile) {
            this.profile_idc = idc;
            this.profile_iop = iop;
            this.profile = profile;
        }
    }

    ;

    // This is from https://tools.ietf.org/html/rfc6184#section-8.1.
    private static final ProfilePattern[] kProfilePatterns = {
            new ProfilePattern((byte) 0x42, new BitPattern("x1xx0000"), H264Profile.kProfileConstrainedBaseline),
            new ProfilePattern((byte) 0x4D, new BitPattern("1xxx0000"), H264Profile.kProfileConstrainedBaseline),
            new ProfilePattern((byte) 0x58, new BitPattern("11xx0000"), H264Profile.kProfileConstrainedBaseline),
            new ProfilePattern((byte) 0x42, new BitPattern("x0xx0000"), H264Profile.kProfileBaseline),
            new ProfilePattern((byte) 0x58, new BitPattern("10xx0000"), H264Profile.kProfileBaseline),
            new ProfilePattern((byte) 0x4D, new BitPattern("0x0x0000"), H264Profile.kProfileMain),
            new ProfilePattern((byte) 0x64, new BitPattern("00000000"), H264Profile.kProfileHigh),
            new ProfilePattern((byte) 0x64, new BitPattern("00001100"), H264Profile.kProfileConstrainedHigh),
            new ProfilePattern((byte) 0xF4, new BitPattern("00000000"), H264Profile.kProfilePredictiveHigh444)
    };

    private static H264Profile parseH264Profile(final String str) {
        if (str.length() != 6) {
            return H264Profile.kProfileNone;
        }

        int profile_level_idc;
        try {
            profile_level_idc = Integer.parseInt(str, 16);
        } catch (Exception e) {
            e.printStackTrace();
            return H264Profile.kProfileNone;
        }

        byte profile_idc = (byte) (profile_level_idc >> 16);
        byte profile_iop = (byte) (profile_level_idc >> 8);

        for (final ProfilePattern pattern : kProfilePatterns) {
            if (profile_idc == pattern.profile_idc &&
                    pattern.profile_iop.isMatch(profile_iop)) {
                return pattern.profile;
            }
        }

        return H264Profile.kProfileNone;
    }

    // Convert a string of 8 characters into a byte where the positions containing
    // character c will have their bit set. For example, c = 'x', str = "x1xx0000"
    // will return 0b10110000.
    private static byte byteMaskString(byte c, final byte[] str) {
        return (byte) ((str[0] == c ? 1 : 0) << 7 | (str[1] == c ? 1 : 0) << 6 | (str[2] == c ? 1 : 0) << 5 |
                (str[3] == c ? 1 : 0) << 4 | (str[4] == c ? 1 : 0) << 3 | (str[5] == c ? 1 : 0) << 2 |
                (str[6] == c ? 1 : 0) << 1 | (str[7] == c ? 1 : 0));
    }
}
