package com.sdt.diagnose.Device.X_Skyworth;

import android.database.ContentObserver;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;

import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.log.LogUtils;

public class FTIContentObserver extends ContentObserver {
    private static final String TAG = "FTIContentObserver";
    private final Handler mHandler;

    /**
     * Creates a content observer.
     *
     * @param handler The handler to run {@link #onChange} on, or null if none.
     */
    public FTIContentObserver(Handler handler) {
        super(handler);
        mHandler = handler;
    }

    /**
     * This method is called when a content change occurs.
     * <p>
     * Subclasses should override this method to handle content changes.
     * </p>
     *
     * @param selfChange True if this is a self-change notification.
     */
    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        int flag = Settings.Secure.getIntForUser(GlobalContext.getContext().getContentResolver(),
                Settings.Secure.USER_SETUP_COMPLETE, 0, UserHandle.USER_CURRENT);
        LogUtils.d(TAG, "user_setup_complete onChange: " + flag);
        if (flag == 1) {
            mHandler.obtainMessage(FTIMonitor.MSG_STOP_MONITOR_FTI_DURATION).sendToTarget();
        }
    }
}
