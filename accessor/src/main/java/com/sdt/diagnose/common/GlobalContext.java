package com.sdt.diagnose.common;

import android.content.Context;

public class GlobalContext {
    static Context mContext;

    public static void setContext(Context context) {
        mContext = context;
    }

    public static Context getContext() {
        return mContext;
    }
}
