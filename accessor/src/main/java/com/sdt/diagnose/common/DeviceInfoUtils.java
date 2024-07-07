package com.sdt.diagnose.common;

import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.icu.text.TimeZoneFormat;
import android.icu.text.TimeZoneNames;
import android.os.Build;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.provider.Settings;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.text.style.TtsSpan;
import android.view.View;

import androidx.core.text.BidiFormatter;
import androidx.core.text.TextDirectionHeuristicsCompat;

import com.android.internal.app.LocalePicker;
import com.sdt.diagnose.common.configuration.Config;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.common.net.HttpsUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DeviceInfoUtils {
    private static final String TAG = "DeviceInfoUtils";
    private static final String CONFIG_DEVICE_OPERATOR = "tms_operator_name";
    private static final String DEFAULT_DEVICE_OPERATOR = "";

    public static String getDeviceName(Context context) {
        return Settings.Global.getString(context.getContentResolver(), Settings.Global.DEVICE_NAME);
    }

    public static String getOperatorName() {
        String operatorName = Config.getString(CONFIG_DEVICE_OPERATOR, DEFAULT_DEVICE_OPERATOR);
        LogUtils.d(TAG, "getOperatorName: " + operatorName);
        return operatorName;
    }

    public static String getManufacturer() {
        return Build.MANUFACTURER;
    }

    public static String getSerialNumber() {
        return Build.getSerial();
    }

    public static String getChipID() {
        try {
            InputStream is = Files.newInputStream(Paths.get("/proc/cpuinfo"));
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            while (line != null) {
                // LogUtils.d(TAG, "Read a line from the /proc/cpuinfo file: " + line);
                if (line.startsWith("Serial")) {
                    int index = line.indexOf(":");
                    if (index != -1) {
                        String serial = line.substring(index + 1).trim();
                        LogUtils.d(TAG, "Serial: " + serial);
                        return serial;
                    }
                }
                line = reader.readLine(); // 读取下一行
            }

        } catch (IOException e) {
            LogUtils.e(TAG, "getChipID error: " + e.getMessage());
        }
        return "";
    }

    public static String extractNumbers(String input, int desiredLength) {
        // 定义正则表达式匹配数字
        String regex = "\\d+";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        // 使用StringBuilder来构建结果字符串
        StringBuilder result = new StringBuilder();

        // 找到匹配的数字并追加到结果字符串中
        while (matcher.find()) {
            result.append(matcher.group());
        }

        // 根据指定长度进行处理
        return adjustLength(result.toString(), desiredLength);
    }

    private static String adjustLength(String input, int desiredLength) {
        int inputLength = input.length();

        // 如果长度不足，前面补0
        if (inputLength < desiredLength) {
            StringBuilder zeros = new StringBuilder();
            for (int i = 0; i < desiredLength - inputLength; i++) {
                zeros.append("0");
            }
            return zeros + input;
        }
        // 如果长度超过，只取末尾的指定长度
        else if (inputLength > desiredLength) {
            return input.substring(inputLength - desiredLength);
        }
        // 如果长度正好，直接返回
        else {
            return input;
        }
    }

    public static String getUspAgentID() {
        String sn = extractNumbers(getSerialNumber(), 12);
        LogUtils.d(TAG, "getUspAgentID: " + sn);
        return "os::309176-" + sn;
    }

    /**
     * Reads a line from the specified file.
     *
     * @param filename the file to read from
     * @return the first line, if any.
     * @throws IOException if the file couldn't be read
     */
    private static String readLine(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename), 256)) {
            return reader.readLine();
        }
    }

    private static String getMsvSuffix() {
        // Production devices should have a non-zero value. If we can't read it, assume it's a
        // production device so that we don't accidentally show that it's an ENGINEERING device.
        try {
            String msv = readLine("/sys/board_properties/soc/msv");
            // Parse as a hex number. If it evaluates to a zero, then it's an engineering build.
            if (Long.parseLong(msv, 16) == 0) {
                return " (ENGINEERING)";
            }
        } catch (IOException | NumberFormatException e) {
            // Fail quietly, as the file may not exist on some devices, or may be unreadable
        }
        return "";
    }

    public static String getDeviceModel() {
        return Build.MODEL + getMsvSuffix();
    }

    public static String getDeviceFirmwareVersion() {
        return Build.VERSION.RELEASE;
    }

    public static String getBuildInfo() {
        return Build.DISPLAY;
    }

    public static String getBuildVersion() {
        return Build.VERSION.INCREMENTAL;
    }

    public static String getHardware() {
        return Build.HARDWARE;
    }

    static String getSecurityPatch() {
        String patch = Build.VERSION.SECURITY_PATCH;
        if (!"".equals(patch)) {
            try {
                SimpleDateFormat template = new SimpleDateFormat("yyyy-MM-dd");
                Date patchDate = template.parse(patch);
                if (patchDate == null) return "";
                String format = DateFormat.getBestDateTimePattern(Locale.getDefault(), "dMMMMyyyy");
                patch = DateFormat.format(format, patchDate).toString();
            } catch (ParseException e) {
                // broken parse; fall through and use the raw string
            }
            return patch;
        } else {
            return "";
        }
    }

    public static String getAndroidSecurityPatchLevel() {
        return getSecurityPatch();
    }

    public static String getScreenSaver(Context context) {
        String result = "";
        IDreamManager mDreamManager = IDreamManager.Stub.asInterface(
                ServiceManager.getService(DreamService.DREAM_SERVICE));
        if (mDreamManager == null)
            return result;
        try {
            ComponentName[] dreams = mDreamManager.getDreamComponents();
            ComponentName cn = dreams != null && dreams.length > 0 ? dreams[0] : null;
            if (cn != null) {
                PackageManager pm = context.getPackageManager();
                try {
                    ServiceInfo ri = pm.getServiceInfo(cn, 0);
                    if (ri != null) {
                        result = ri.loadLabel(pm).toString();
                    }
                } catch (PackageManager.NameNotFoundException exc) {
                    LogUtils.e(TAG, "Failed to get service info, " + exc.getMessage());
                }
            }
        } catch (RemoteException e) {
            LogUtils.e(TAG, "Failed to get default dream, " + e.getMessage());
        }
        return result;
    }

    public static String getBluetoothMac(Context context) {
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        String result = "unavailable";
        if (bluetooth != null) {
            String address = bluetooth.isEnabled() ? bluetooth.getAddress() : "";
            if (!TextUtils.isEmpty(address)) {
                // Convert the address to lowercase for consistency with the wifi MAC address.
                result = address.toLowerCase();
            }
        }
        return result;
    }

    public static void updateStandbyStatus(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        boolean isScreenOn = powerManager.isInteractive();
        if (isScreenOn) {
            // 设备处于唤醒状态
            HttpsUtils.uploadStandbyStatus(1);
        } else {
            // 设备处于待机状态
            HttpsUtils.uploadStandbyStatus(0);
        }
    }

    public static int getAutoDateTimeType(Context context) {
        return Settings.Global.getInt(context.getContentResolver(),
                Settings.Global.AUTO_TIME, 0);
    }

    public static boolean is24Hour() {
        ContentResolver cv = GlobalContext.getContext().getContentResolver();
        String strTimeFormat = android.provider.Settings.System.getString(cv,
                android.provider.Settings.System.TIME_12_24);
        LogUtils.d(TAG, "is24Hour: " + strTimeFormat);
        return Objects.equals("24", strTimeFormat);
    }

    public static String getTime() {
        final Calendar now = Calendar.getInstance();
        StringBuilder sb = new StringBuilder();

        if (is24Hour()) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
            return sdf.format(new Date());
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss", Locale.getDefault());
            return sdf.format(new Date());
        }
    }

    private static void appendWithTtsSpan(SpannableStringBuilder builder, CharSequence content, TtsSpan span) {
        int start = builder.length();
        builder.append(content);
        builder.setSpan(span, start, builder.length(), 0);
    }

    // Input must be positive. minDigits must be 1 or 2.
    private static String formatDigits(int input, int minDigits, String localizedDigits) {
        final int tens = input / 10;
        final int units = input % 10;
        StringBuilder builder = new StringBuilder(minDigits);
        if (input >= 10 || minDigits == 2) {
            builder.append(localizedDigits.charAt(tens));
        }
        builder.append(localizedDigits.charAt(units));
        return builder.toString();
    }

    /**
     * Get the GMT offset text label for the given time zone, in the format "GMT-08:00". This will
     * also add TTS spans to give hints to the text-to-speech engine for the type of data it is.
     *
     * @param tzFormatter The timezone formatter to use.
     * @param locale      The locale which the string is displayed in. This should be the same as the
     *                    locale of the time zone formatter.
     * @param tz          Time zone to get the GMT offset from.
     * @param now         The current time, used to tell whether daylight savings is active.
     * @return A CharSequence suitable for display as the offset label of {@code tz}.
     */
    private static CharSequence getGmtOffsetText(TimeZoneFormat tzFormatter, Locale locale,
                                                 TimeZone tz, Date now) {
        final SpannableStringBuilder builder = new SpannableStringBuilder();

        final String gmtPattern = tzFormatter.getGMTPattern();
        final int placeholderIndex = gmtPattern.indexOf("{0}");
        final String gmtPatternPrefix, gmtPatternSuffix;
        if (placeholderIndex == -1) {
            // Bad pattern. Replace with defaults.
            gmtPatternPrefix = "GMT";
            gmtPatternSuffix = "";
        } else {
            gmtPatternPrefix = gmtPattern.substring(0, placeholderIndex);
            gmtPatternSuffix = gmtPattern.substring(placeholderIndex + 3); // After the "{0}".
        }

        if (!gmtPatternPrefix.isEmpty()) {
            appendWithTtsSpan(builder, gmtPatternPrefix,
                    new TtsSpan.TextBuilder(gmtPatternPrefix).build());
        }

        int offsetMillis = tz.getOffset(now.getTime());
        final boolean negative = offsetMillis < 0;
        final TimeZoneFormat.GMTOffsetPatternType patternType;
        if (negative) {
            offsetMillis = -offsetMillis;
            patternType = TimeZoneFormat.GMTOffsetPatternType.NEGATIVE_HM;
        } else {
            patternType = TimeZoneFormat.GMTOffsetPatternType.POSITIVE_HM;
        }
        final String gmtOffsetPattern = tzFormatter.getGMTOffsetPattern(patternType);
        final String localizedDigits = tzFormatter.getGMTOffsetDigits();

        final int offsetHours = (int) (offsetMillis / DateUtils.HOUR_IN_MILLIS);
        final int offsetMinutes = (int) (offsetMillis / DateUtils.MINUTE_IN_MILLIS);
        final int offsetMinutesRemaining = Math.abs(offsetMinutes) % 60;

        for (int i = 0; i < gmtOffsetPattern.length(); i++) {
            char c = gmtOffsetPattern.charAt(i);
            if (c == '+' || c == '-' || c == '\u2212' /* MINUS SIGN */) {
                final String sign = String.valueOf(c);
                appendWithTtsSpan(builder, sign, new TtsSpan.VerbatimBuilder(sign).build());
            } else if (c == 'H' || c == 'm') {
                final int numDigits;
                if (i + 1 < gmtOffsetPattern.length() && gmtOffsetPattern.charAt(i + 1) == c) {
                    numDigits = 2;
                    i++; // Skip the next formatting character.
                } else {
                    numDigits = 1;
                }
                final int number;
                final String unit;
                if (c == 'H') {
                    number = offsetHours;
                    unit = "hour";
                } else { // c == 'm'
                    number = offsetMinutesRemaining;
                    unit = "minute";
                }
                appendWithTtsSpan(builder, formatDigits(number, numDigits, localizedDigits),
                        new TtsSpan.MeasureBuilder().setNumber(number).setUnit(unit).build());
            } else {
                builder.append(c);
            }
        }

        if (!gmtPatternSuffix.isEmpty()) {
            appendWithTtsSpan(builder, gmtPatternSuffix,
                    new TtsSpan.TextBuilder(gmtPatternSuffix).build());
        }

        CharSequence gmtText = new SpannableString(builder);

        // Ensure that the "GMT+" stays with the "00:00" even if the digits are RTL.
        final BidiFormatter bidiFormatter = BidiFormatter.getInstance();
        boolean isRtl = TextUtils.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL;
        gmtText = bidiFormatter.unicodeWrap(gmtText,
                isRtl ? TextDirectionHeuristicsCompat.RTL : TextDirectionHeuristicsCompat.LTR);
        return gmtText;
    }

    /**
     * Returns the long name for the timezone for the given locale at the time specified.
     * Can return {@code null}.
     */
    private static String getZoneLongName(TimeZoneNames names, TimeZone tz, Date now) {
        final TimeZoneNames.NameType nameType =
                tz.inDaylightTime(now) ? TimeZoneNames.NameType.LONG_DAYLIGHT
                        : TimeZoneNames.NameType.LONG_STANDARD;
        return names.getDisplayName(tz.getID(), nameType, now.getTime());
    }

    public static String getTimeZone(Context context) {
        final Calendar now = Calendar.getInstance();
        TimeZone tz = now.getTimeZone();
        Date date = now.getTime();
        Locale locale = context.getResources().getConfiguration().locale;
        TimeZoneFormat tzFormatter = TimeZoneFormat.getInstance(locale);
        CharSequence gmtText = getGmtOffsetText(tzFormatter, locale, tz, date);
        TimeZoneNames timeZoneNames = TimeZoneNames.getInstance(locale);
        String zoneNameString = getZoneLongName(timeZoneNames, tz, date);
        if (zoneNameString == null) {
            return gmtText.toString();
        }
        // We don't use punctuation here to avoid having to worry about localizing that too!
        return TextUtils.concat(gmtText, " ", zoneNameString).toString();
    }

    public static String getLanguage() {
        Locale currentLocale = null;
        try {
            currentLocale = ActivityManager.getService().getConfiguration().getLocales().get(0);
        } catch (RemoteException e) {
            LogUtils.e(TAG, "Could not retrieve locale, " + e.getMessage());
        }
        if (currentLocale == null) {
            return new Locale("en-US").getDisplayName();
        }
        return currentLocale.getDisplayName();
    }

    public static boolean changeSystemLanguage(String language) {
        final List<LocalePicker.LocaleInfo> localeInfoList =
                LocalePicker.getAllAssetLocales(GlobalContext.getContext(), false);
        for (final LocalePicker.LocaleInfo localeInfo : localeInfoList) {
            Locale locale = localeInfo.getLocale();
            if (locale.toLanguageTag().equals(language)) {
                LogUtils.d(TAG, "The language that will be switched to is: " + locale.toLanguageTag());
                LocalePicker.updateLocale(locale);
                return true;
            }
        }
        LogUtils.e(TAG, "The specified language cannot be found: " + language);
        return false;
    }

}
