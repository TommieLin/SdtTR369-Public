package com.sdt.diagnose.common.log;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.DiskLogAdapter;

import java.io.File;
import java.util.Optional;
import java.util.function.Function;

/**
 * e.g LogUtils.i("hello"); TV_Stack-(SectionBufferTest.java:90): hello
 *
 * <p>LogUtils.i("enter info:%d",i); TV_Stack-(SectionBufferTest.java:90): enter info:10000
 *
 * <p>LogUtils.i("enter info:%s",map); TV_Stack-(SectionBufferTest.java:90): enter
 * info:{item1=good1, item2=good2, item3=good3}
 */
public class LogUtils {
    private static final SimpleFormatStrategy DEF_STRATEGY;
    private static final LogUtilsWrapper LOGGER_PRINTER;
    private static final SimpleLogAdapter ANDROID_LOG_ADAPTER;
    private static final Context CONTEXT_INSTANCE;

    private static volatile boolean isDebug;

    static {
        CONTEXT_INSTANCE = InnerUtils.getApp();
        String tag = Optional.ofNullable(CONTEXT_INSTANCE)
                .map(new Function<Context, String>() {
                    @Override
                    public String apply(Context context) {
                        String tag = InnerUtils.getAppName(context.getPackageName());
                        if (TextUtils.isEmpty(tag)) {
                            tag = context.getApplicationInfo().packageName;
                        }
                        return tag;
                    }
                })
                .orElse(LogUtils.class.getSimpleName());
        isDebug = InnerUtils.isAppDebug();
        DEF_STRATEGY =
                SimpleFormatStrategy.newBuilder()
                        .showThreadInfo(false)
                        .methodCount(0)
                        .tag(tag)
                        .build();

        ANDROID_LOG_ADAPTER = new SimpleLogAdapter(DEF_STRATEGY);
        LOGGER_PRINTER = new LogUtilsWrapper(new SimpleLoggerPrinter(), ANDROID_LOG_ADAPTER);
        LOGGER_PRINTER.addAdapter(ANDROID_LOG_ADAPTER);
    }

    public static void saveToFile(File target) {
        LOGGER_PRINTER.addAdapter(new DiskLogAdapter());
    }

    public static LogUtilsWrapper showThreadInfo(boolean show) {
        SimpleFormatStrategy strategy = DEF_STRATEGY.copy();
        ANDROID_LOG_ADAPTER.setFormatStrategy(strategy);
        return LOGGER_PRINTER.showThreadInfo(show);
    }

    public static LogUtilsWrapper methodStackCount(int count) {
        SimpleFormatStrategy strategy = DEF_STRATEGY.copy();
        ANDROID_LOG_ADAPTER.setFormatStrategy(strategy);
        return LOGGER_PRINTER.methodStackCount(count);
    }

    public static PrinterProxy t(String tag) {
        return LOGGER_PRINTER.t(tag);
    }

    public static void tag(String tag) {
        DEF_STRATEGY.setTag(tag);
    }

    public static void d(@NonNull String message, @Nullable Object... args) {
        resetCallerStrategy();
        LOGGER_PRINTER.d(message, args);
    }

    public static void d(@NonNull String tag, @NonNull String message, @Nullable Object... args) {
        resetCallerStrategy();
        LOGGER_PRINTER.d(tag + ": " + message, args);
    }

    public static void d(@Nullable Object object) {
        resetCallerStrategy();
        LOGGER_PRINTER.d(object);
    }

    public static void e(@NonNull String message, @Nullable Object... args) {
        resetCallerStrategy();
        LOGGER_PRINTER.e(null, message, args);
    }

    public static void e(@NonNull String tag, @NonNull String message, @Nullable Object... args) {
        resetCallerStrategy();
        LOGGER_PRINTER.e(null, tag + ": " + message, args);
    }

    public static void e(
            @Nullable Throwable throwable, @NonNull String message, @Nullable Object... args) {
        resetCallerStrategy();
        LOGGER_PRINTER.e(throwable, message, args);
    }

    public static void i(@NonNull String message, @Nullable Object... args) {
        resetCallerStrategy();
        LOGGER_PRINTER.i(message, args);
    }

    public static void i(@NonNull String tag, @NonNull String message, @Nullable Object... args) {
        resetCallerStrategy();
        LOGGER_PRINTER.i(tag + ": " + message, args);
    }

    public static void v(@NonNull String message, @Nullable Object... args) {
        resetCallerStrategy();
        LOGGER_PRINTER.v(message, args);
    }

    public static void v(@NonNull String tag, @NonNull String message, @Nullable Object... args) {
        resetCallerStrategy();
        LOGGER_PRINTER.v(tag + ": " + message, args);
    }

    public static void w(@NonNull String message, @Nullable Object... args) {
        resetCallerStrategy();
        LOGGER_PRINTER.w(message, args);
    }

    public static void w(@NonNull String tag, @NonNull String message, @Nullable Object... args) {
        resetCallerStrategy();
        LOGGER_PRINTER.w(tag + ": " + message, args);
    }

    /**
     * Tip: Use this for exceptional situations to log ie: Unexpected errors etc
     */
    public static void wtf(@NonNull String message, @Nullable Object... args) {
        resetCallerStrategy();
        LOGGER_PRINTER.wtf(message, args);
    }

    public static void wtf(@NonNull String tag, @NonNull String message, @Nullable Object... args) {
        resetCallerStrategy();
        LOGGER_PRINTER.wtf(tag + ": " + message, args);
    }

    /**
     * Formats the given json content and print it
     */
    public static void json(@Nullable String json) {
        resetCallerStrategy();
        LOGGER_PRINTER.json(json);
    }

    /**
     * Formats the given xml content and print it
     */
    public static void xml(@Nullable String xml) {
        resetCallerStrategy();
        LOGGER_PRINTER.xml(xml);
    }

    private static String getCallerClass() {
        StackTraceElement traceElement =
                Utils.getExternalCallerStackTrace(LogUtils.class.getName());
        return traceElement.getClassName();
    }

//    private static SimpleFormatStrategy getStrategy() {
//        String callerClass = getCallerClass();
//        SimpleFormatStrategy strategy = CLASS_STRATEGY_MAP.get(callerClass);
//        if (strategy == null) {
//            strategy = DEF_STRATEGY;
//        }
//        return strategy;
//    }

    private static void resetCallerStrategy() {
//        SimpleFormatStrategy strategy = getStrategy();
        ANDROID_LOG_ADAPTER.setFormatStrategy(DEF_STRATEGY);
//        return strategy;
    }

    static boolean isDebug() {
        return isDebug || Log.isLoggable(DEF_STRATEGY.getTag(), Log.VERBOSE);
    }

    @Deprecated
    public static void closeLog(Object o) {
//        LOGGER_PRINTER.addBlackList(o.getClass());
    }

    @Deprecated
    public static void openLog(Object o) {
//        LOGGER_PRINTER.removeBlackList(o.getClass());
    }

    public static void setDebuggable(boolean debuggable) {
        isDebug = debuggable;
    }
}
