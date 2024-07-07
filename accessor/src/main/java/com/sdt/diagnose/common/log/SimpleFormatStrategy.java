package com.sdt.diagnose.common.log;

import static com.sdt.diagnose.common.log.Utils.checkNotNull;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.LogStrategy;
import com.orhanobut.logger.LogcatLogStrategy;

public class SimpleFormatStrategy implements FormatStrategy {

    public static final String LOGGER_PRINTER = "LoggerPrinter";
    /**
     * Android's max limit for a log entry is ~4076 bytes, so 4000 bytes is used as chunk size since
     * default charset is UTF-8
     */
    private static final int CHUNK_SIZE = 4000;
    /**
     * The minimum stack trace index, starts at this class after two native calls.
     */
    private static final int MIN_STACK_OFFSET = 5;
    /**
     * Drawing toolbox
     */
    private static final char TOP_LEFT_CORNER = '┌';

    private static final char BOTTOM_LEFT_CORNER = '└';
    private static final char MIDDLE_CORNER = '├';
    private static final char HORIZONTAL_LINE = '│';
    private static final String DOUBLE_DIVIDER =
            "────────────────────────────────────────────────────────";
    private static final String SINGLE_DIVIDER =
            "┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄";
    private static final String TOP_BORDER = TOP_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
    private static final String BOTTOM_BORDER =
            BOTTOM_LEFT_CORNER + DOUBLE_DIVIDER + DOUBLE_DIVIDER;
    private static final String MIDDLE_BORDER = MIDDLE_CORNER + SINGLE_DIVIDER + SINGLE_DIVIDER;
    @NonNull
    private final LogStrategy logStrategy;
    private int methodCount;
    private int methodOffset;
    private boolean showThreadInfo;
    @Nullable
    private String tag;

    private SimpleFormatStrategy(@NonNull SimpleFormatStrategy.Builder builder) {
        checkNotNull(builder);

        methodCount = builder.methodCount;
        methodOffset = builder.methodOffset;
        showThreadInfo = builder.showThreadInfo;
        logStrategy = builder.logStrategy;
        tag = builder.tag;
    }

    public SimpleFormatStrategy copy() {
        SimpleFormatStrategy newOne = new SimpleFormatStrategy(SimpleFormatStrategy.newBuilder().logStrategy(logStrategy));
        newOne.methodCount = methodCount;
        newOne.methodOffset = methodOffset;
        newOne.showThreadInfo = showThreadInfo;
        newOne.tag = tag;
        return newOne;
    }

    @NonNull
    public static SimpleFormatStrategy.Builder newBuilder() {
        return new SimpleFormatStrategy.Builder();
    }

    @Override
    public void log(int priority, @Nullable String onceOnlyTag, @NonNull String message) {
        checkNotNull(message);

        String tag = formatTag(onceOnlyTag);
        byte[] bytes = message.getBytes();
        int length = bytes.length;
        String[] lines = message.split(System.getProperty("line.separator"));
        boolean paintDivider =
                methodCount > 0 || length > CHUNK_SIZE || showThreadInfo || lines.length > 1;

        if (paintDivider) {
            logTopBorder(priority, tag);
            logHeaderContent(priority, tag, methodCount);
        }

        // get bytes of message with system's default charset (which is UTF-8 for Android)

        if (!paintDivider) {
            logContent(priority, paintDivider, tag, lines);
//            logBottomBorder(priority, tag);
            return;
        }
        if (methodCount > 0) {
            logDivider(priority, tag);
        }
        for (int i = 0; i < length; i += CHUNK_SIZE) {
            int count = Math.min(length - i, CHUNK_SIZE);
            // create a new String with system's default charset (which is UTF-8 for Android)
            logContent(priority, paintDivider, tag, new String[]{new String(bytes, i, count)});
        }
        logBottomBorder(priority, tag);
    }

    private void logTopBorder(int logType, @Nullable String tag) {
        logChunk(logType, tag, TOP_BORDER);
    }

    @SuppressWarnings("StringBufferReplaceableByString")
    private void logHeaderContent(int logType, @Nullable String tag, int methodCount) {
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        if (showThreadInfo) {
            logChunk(
                    logType, tag, HORIZONTAL_LINE + " Thread: " + Thread.currentThread().getName());
            logDivider(logType, tag);
        }
        String level = "";

        int stackTopOffset = getStackOffset(trace) + methodOffset;

        // corresponding method count with the current stack may exceeds the stack trace. Trims the
        // count
        if (methodCount + stackTopOffset > trace.length) {
            methodCount = trace.length - stackTopOffset;
        }

        for (int i = stackTopOffset; i < methodCount + stackTopOffset; i++) {
            if (i >= trace.length) {
                continue;
            }
            StringBuilder builder = new StringBuilder();
            builder.append(HORIZONTAL_LINE)
                    .append(' ')
                    .append(level)
                    .append(getSimpleClassName(trace[i].getClassName()))
                    .append(".")
                    .append(trace[i].getMethodName())
                    .append(" ")
                    .append(" (")
                    .append(trace[i].getFileName())
                    .append(":")
                    .append(trace[i].getLineNumber())
                    .append(")");
            level += "   ";
            logChunk(logType, tag, builder.toString());
        }
    }

    private void logBottomBorder(int logType, @Nullable String tag) {
        logChunk(logType, tag, BOTTOM_BORDER);
    }

    private void logDivider(int logType, @Nullable String tag) {
        logChunk(logType, tag, MIDDLE_BORDER);
    }

    private void logContent(
            int logType, boolean multiline, @Nullable String tag, @NonNull String[] chunk) {
        checkNotNull(chunk);

        if (chunk.length > 1 || multiline) {
            for (String line : chunk) {
                logChunk(logType, tag, HORIZONTAL_LINE + " " + line);
            }
        } else {
            logChunk(logType, tag, chunk[0]);
        }
    }

    private void logChunk(int priority, @Nullable String tag, @NonNull String chunk) {
        checkNotNull(chunk);

        logStrategy.log(priority, tag, chunk);
    }

    private String getSimpleClassName(@NonNull String name) {
        checkNotNull(name);

        int lastIndex = name.lastIndexOf(".");
        return name.substring(lastIndex + 1);
    }

    /**
     * Determines the starting index of the stack trace, after method calls made by this class.
     *
     * @param trace the stack trace
     * @return the stack offset
     */
    private int getStackOffset(@NonNull StackTraceElement[] trace) {
        checkNotNull(trace);

//        for (int i = MIN_STACK_OFFSET; i < trace.length; i++) {
//            StackTraceElement e = trace[i];
//            String name = e.getClassName();
//            if (name.equals(LOGGER_PRINTER) || name.equals(Logger.class.getName())) {
//                return --i;
//            }
//        }
        return Utils.getLogUtilsCaller(trace);
    }

    @Nullable
    private String formatTag(@Nullable String onecetag) {
        if (!TextUtils.isEmpty(onecetag) && !TextUtils.equals(this.tag, onecetag)) {
            return onecetag;
        }
        return this.tag;
    }

    public void setTag(@Nullable String tag) {
        this.tag = tag;
    }

    @Nullable
    public String getTag() {
        return tag;
    }

    public void setMethodCount(int methodCount) {
        this.methodCount = methodCount;
    }

    public void setMethodOffset(int methodOffset) {
        this.methodOffset = methodOffset;
    }

    public void setShowThreadInfo(boolean showThreadInfo) {
        this.showThreadInfo = showThreadInfo;
    }

    public static class Builder {
        int methodCount = 2;
        int methodOffset = 0;
        boolean showThreadInfo = true;
        @Nullable
        LogStrategy logStrategy;
        @Nullable
        String tag = "PRETTY_LOGGER";

        private Builder() {
        }

        @NonNull
        public SimpleFormatStrategy.Builder methodCount(int val) {
            methodCount = val;
            return this;
        }

        @NonNull
        public SimpleFormatStrategy.Builder methodOffset(int val) {
            methodOffset = val;
            return this;
        }

        @NonNull
        public SimpleFormatStrategy.Builder showThreadInfo(boolean val) {
            showThreadInfo = val;
            return this;
        }

        @NonNull
        public SimpleFormatStrategy.Builder logStrategy(@Nullable LogStrategy val) {
            logStrategy = val;
            return this;
        }

        @NonNull
        public SimpleFormatStrategy.Builder tag(@Nullable String tag) {
            this.tag = tag;
            return this;
        }

        @NonNull
        public SimpleFormatStrategy build() {
            if (logStrategy == null) {
                logStrategy = new LogcatLogStrategy();
            }
            return new SimpleFormatStrategy(this);
        }
    }
}
