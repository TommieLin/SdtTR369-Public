package com.sdt.diagnose.common.log;

import static com.sdt.diagnose.common.log.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.FormatStrategy;
import com.orhanobut.logger.LogAdapter;
import com.orhanobut.logger.PrettyFormatStrategy;

public class SimpleLogAdapter implements LogAdapter {

    @NonNull
    private FormatStrategy formatStrategy;

    public SimpleLogAdapter() {
        this.formatStrategy = PrettyFormatStrategy.newBuilder().build();
    }

    public SimpleLogAdapter(@NonNull FormatStrategy formatStrategy) {
        this.formatStrategy = checkNotNull(formatStrategy);
    }

    public void setFormatStrategy(@NonNull FormatStrategy formatStrategy) {
        this.formatStrategy = formatStrategy;
    }

    @NonNull
    public FormatStrategy getFormatStrategy() {
        return formatStrategy;
    }

    @Override
    public boolean isLoggable(int priority, @Nullable String tag) {
        return true;
    }

    @Override
    public void log(int priority, @Nullable String tag, @NonNull String message) {
        formatStrategy.log(priority, tag, message);
    }
}
