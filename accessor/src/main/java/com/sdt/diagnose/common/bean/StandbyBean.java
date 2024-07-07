package com.sdt.diagnose.common.bean;

import android.provider.Settings;
import android.text.TextUtils;

import com.sdt.diagnose.common.GlobalContext;

public class StandbyBean {
    private boolean isEnable = false;
    public String urlKey = "sdt.standby.url";
    public static StandbyBean instance;

    public static StandbyBean getInstance() {
        synchronized (StandbyBean.class) {
            if (instance == null) {
                instance = new StandbyBean();
            }
        }
        return instance;
    }

    public boolean isEnable() {
        return isEnable;
    }

    public void setEnable(String enable) {
        isEnable = TextUtils.equals(enable, "1");
    }

    public String getUpdateUrl() {
        return Settings.Global.getString(GlobalContext.getContext().getContentResolver(),
                StandbyBean.getInstance().urlKey);
    }

    public void setUpdateUrl(String updateUrl) {
        Settings.Global.putString(GlobalContext.getContext().getContentResolver(),
                StandbyBean.getInstance().urlKey, updateUrl);
    }
}
