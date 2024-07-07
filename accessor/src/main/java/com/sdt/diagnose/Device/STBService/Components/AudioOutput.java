package com.sdt.diagnose.Device.STBService.Components;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.provider.Settings;

import com.sdt.accessor.R;
import com.sdt.annotations.Tr369Get;
import com.sdt.diagnose.common.GlobalContext;

import java.util.Iterator;
import java.util.Map;

public class AudioOutput {
    private static final String TAG = "AudioOutput";
    public static final String AUDIO_MIXING = "audio_mixing";
    private static final String PARA_AUDIO_MIXING_ON = "disable_pcm_mixing=0";
    private static final String PARA_AUDIO_MIXING_OFF = "disable_pcm_mixing=1";
    public static final int AUDIO_MIXING_OFF = 0;
    public static final int AUDIO_MIXING_ON = 1;
    public static final int AUDIO_MIXING_DEFAULT = AUDIO_MIXING_ON;

    @Tr369Get("Device.Services.STBService.1.Components.AudioOutput.1.AudioLevel")
    public String SK_TR369_GetAudioOutputLevel() {
        AudioManager audioManager =
                (AudioManager) GlobalContext.getContext().getSystemService(Context.AUDIO_SERVICE);
        int streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (streamMaxVolume <= 0) {
            return "Error!!! Max volume is 0";
        }
        int streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        return String.valueOf(streamVolume * 100 / streamMaxVolume);
    }

    @Tr369Get("Device.Services.STBService.1.Components.AudioOutput.1.Status")
    public String SK_TR369_GetAudioOutputStatus() {
        AudioManager audioManager =
                (AudioManager) GlobalContext.getContext().getSystemService(Context.AUDIO_SERVICE);
        int streamMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        if (streamMaxVolume <= 0) {
            return "Error";
        }
        int streamVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (streamVolume == 0 || audioManager.isStreamMute(AudioManager.STREAM_MUSIC)) {
            return "Mute";
        }
        if (streamVolume > 0) {
            return "Enabled";
        }
        return "Disabled";
    }

    @Tr369Get("Device.UserInterface.X_TELECOM-COM-AR_AVSettings.AudioMode")
    public String SK_TR369_GetAudioOutputMode() {
        return String.valueOf(
                Settings.Global.getInt(
                        GlobalContext.getContext().getContentResolver(),
                        AUDIO_MIXING,
                        AUDIO_MIXING_DEFAULT) == AUDIO_MIXING_ON);
    }

    @Tr369Get("Device.UserInterface.X_TELECOM-COM-AR_AVSettings.AudioAuto")
    public String SK_TR369_GetAudioOutputAuto() {
        AudioManager audioManager =
                (AudioManager) GlobalContext.getContext().getSystemService(Context.AUDIO_SERVICE);
        Map<Integer, Boolean> reportedSurroundFormats = audioManager.getReportedSurroundFormats();
        Iterator<Map.Entry<Integer, Boolean>> rpsfIterator = reportedSurroundFormats.entrySet().iterator();
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        while (rpsfIterator.hasNext()) {
            Map.Entry<Integer, Boolean> next = rpsfIterator.next();
            String formatDisplayName = getFormatDisplayName(next.getKey());
            builder.append(formatDisplayName).append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append("]");
        return builder.toString();
    }

    /**
     * @return the display name for each surround sound format.
     */
    private String getFormatDisplayName(int formatId) {
        switch (formatId) {
            case AudioFormat.ENCODING_AC3:
                return GlobalContext.getContext().getResources().getString(R.string.surround_sound_format_ac3);
            case AudioFormat.ENCODING_E_AC3:
                return GlobalContext.getContext().getResources().getString(R.string.surround_sound_format_e_ac3);
            case AudioFormat.ENCODING_DTS:
                return GlobalContext.getContext().getResources().getString(R.string.surround_sound_format_dts);
            case AudioFormat.ENCODING_DTS_HD:
                return GlobalContext.getContext().getResources().getString(R.string.surround_sound_format_dts_hd);
            default:
                // Fallback in case new formats have been added that we don't know of.
                return AudioFormat.toDisplayName(formatId);
        }
    }
}
