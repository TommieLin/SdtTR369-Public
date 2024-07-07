package com.sdt.diagnose.common.log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.orhanobut.logger.LogAdapter;

public class LogUtilsWrapper implements PrinterProxy {
    private final PrinterProxy mPrinterProxy;
    private final SimpleLogAdapter mSimpleLogAdapter;

    public LogUtilsWrapper(PrinterProxy printerProxy,
                           SimpleLogAdapter simpleLogAdapter) {
        mPrinterProxy = printerProxy;
        mSimpleLogAdapter = simpleLogAdapter;
    }

    /**
     * show current thread info
     *
     * @param show
     * @return
     */
    LogUtilsWrapper showThreadInfo(boolean show) {
        SimpleFormatStrategy strategy = (SimpleFormatStrategy) mSimpleLogAdapter.getFormatStrategy();
        strategy.setShowThreadInfo(show);
        return this;
    }

    /**
     * set caller StackTrace
     *
     * @param count
     * @return
     */
    LogUtilsWrapper methodStackCount(int count) {
        SimpleFormatStrategy strategy = (SimpleFormatStrategy) mSimpleLogAdapter.getFormatStrategy();
        strategy.setMethodCount(count);
        return this;
    }

    @Override
    public void addAdapter(@NonNull LogAdapter adapter) {
        mPrinterProxy.addAdapter(adapter);
    }

    @Override
    public PrinterProxy t(@Nullable String tag) {
        return mPrinterProxy.t(tag);
    }

    @Override
    public void d(@NonNull String message, @Nullable Object... args) {
        mPrinterProxy.d(message, args);
    }

    @Override
    public void d(@Nullable Object object) {
        mPrinterProxy.d(object);
    }

    @Override
    public void e(@NonNull String message, @Nullable Object... args) {
        mPrinterProxy.e(message, args);
    }

    @Override
    public void e(@Nullable Throwable throwable, @NonNull String message,
                  @Nullable Object... args) {
        mPrinterProxy.e(throwable, message, args);
    }

    @Override
    public void w(@NonNull String message, @Nullable Object... args) {
        mPrinterProxy.w(message, args);
    }

    @Override
    public void i(@NonNull String message, @Nullable Object... args) {
        mPrinterProxy.i(message, args);
    }

    @Override
    public void v(@NonNull String message, @Nullable Object... args) {
        mPrinterProxy.v(message, args);
    }

    @Override
    public void wtf(@NonNull String message, @Nullable Object... args) {
        mPrinterProxy.wtf(message, args);
    }

    @Override
    public void json(@Nullable String json) {
        mPrinterProxy.json(json);
    }

    @Override
    public void xml(@Nullable String xml) {
        mPrinterProxy.xml(xml);
    }

    @Override
    public void log(int priority, @Nullable String tag, @Nullable String message,
                    @Nullable Throwable throwable) {
        mPrinterProxy.log(priority, tag, message, throwable);
    }

    @Override
    public void clearLogAdapters() {
        mPrinterProxy.clearLogAdapters();
    }
}
