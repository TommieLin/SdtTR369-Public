package com.sdt.diagnose.common;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.format.DateFormat;

public class TimeFormatUtils {
    private static final String HOURS_12 = "12";
    private static final String HOURS_24 = "24";

    public static boolean is24Hour(Context context) {
        return DateFormat.is24HourFormat(context);
    }

    public static void timeUpdated(Context context, boolean use24Hour) {
        Intent timeChanged = new Intent(Intent.ACTION_TIME_CHANGED);
        int timeFormatPreference = use24Hour
                ? Intent.EXTRA_TIME_PREF_VALUE_USE_24_HOUR
                : Intent.EXTRA_TIME_PREF_VALUE_USE_12_HOUR;
        timeChanged.putExtra(Intent.EXTRA_TIME_PREF_24_HOUR_FORMAT, timeFormatPreference);
        context.sendBroadcast(timeChanged);
    }

    public static void set24Hour(Context context, boolean use24Hour) {
        if (use24Hour && is24Hour(context)) return;
        if (!use24Hour && !is24Hour(context)) return;

        Settings.System.putString(context.getContentResolver(),
                Settings.System.TIME_12_24,
                use24Hour ? HOURS_24 : HOURS_12);
    }

    public static void set24HourAndTimeUpdated(Context context, boolean use24Hour) {
        if (use24Hour && is24Hour(context)) return;
        if (!use24Hour && !is24Hour(context)) return;

        set24Hour(context, use24Hour);
        timeUpdated(context, use24Hour);
    }
}
