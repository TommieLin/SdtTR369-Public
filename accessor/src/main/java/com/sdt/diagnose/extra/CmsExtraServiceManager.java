package com.sdt.diagnose.extra;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.sdtcmsextra.ICmsExtraService;

public class CmsExtraServiceManager {
    private static final String TAG = "CmsExtraServiceManager";
    private static ICmsExtraService mCmsExtraService;
    private static Context mContext;
    private static CmsExtraServiceManager mCmsExtraServiceManager;
    private final static String CMS_EXTRA_SERVICE_ACTION = "com.sdt.sdtcmsextra.callback";
    private final static String CMS_EXTRA_SERVICE_PACKAGE_NAME = "com.sdt.sdtcmsextra";

    private CmsExtraServiceManager() {
        connectCmsExtraService();
    }

    private void connectCmsExtraService() {
        try {
            Intent intent = buildCmsExtraServiceIntent();
            LogUtils.d(TAG, "Begin to bindService " + intent.getPackage() + " with action " + intent.getAction());
            boolean ret = mContext.bindService(intent, mServiceConnection, Service.BIND_AUTO_CREATE);
            if (ret) {
                LogUtils.d(TAG, "BindService finished");
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "CmsExtraService build failed, " + e.getMessage());
        }
    }

    @NonNull
    private Intent buildCmsExtraServiceIntent() {
        Intent intent = new Intent();
        intent.setAction(CMS_EXTRA_SERVICE_ACTION);
        intent.setPackage(CMS_EXTRA_SERVICE_PACKAGE_NAME);
        return intent;
    }

    public static CmsExtraServiceManager getInstance(@NonNull Context context) {
        if (null == mCmsExtraServiceManager || null == mCmsExtraService) {
            synchronized (CmsExtraServiceManager.class) {
                LogUtils.d(TAG, "Binding failed, we need to renew a service manager");
                mContext = context;
                mCmsExtraServiceManager = new CmsExtraServiceManager();
            }
        }
        return mCmsExtraServiceManager;
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            LogUtils.d(TAG, "ICmsExtraService connected");
            mCmsExtraService = ICmsExtraService.Stub.asInterface(service);
            try {
                mCmsExtraService.asBinder().linkToDeath(mBinderPoolDeathRecipient, 0);
            } catch (RemoteException e) {
                LogUtils.e(TAG, "mCmsExtraService linkToDeath failed, " + e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            LogUtils.d(TAG, "ICmsExtraService Disconnected");
        }

        @Override
        public void onBindingDied(ComponentName name) {
            LogUtils.d(TAG, "ICmsExtraService BindingDied");
        }

        @Override
        public void onNullBinding(ComponentName name) {
            LogUtils.d(TAG, "ICmsExtraService NullBinding");
        }
    };

    private IBinder.DeathRecipient mBinderPoolDeathRecipient = new IBinder.DeathRecipient() {
        @Override
        public void binderDied() {
            synchronized (CmsExtraServiceManager.class) {
                LogUtils.e(TAG, "CmsExtraService Died");
                mCmsExtraService.asBinder().unlinkToDeath(mBinderPoolDeathRecipient, 0);
                mCmsExtraService = null;
                connectCmsExtraService();
            }
        }
    };

    public double getCpuUsage() {
        if (null == mCmsExtraService) {
            throw new NullPointerException("mCmsExtraService is null");
        }

        double ret = 0;
        synchronized (CmsExtraServiceManager.class) {
            try {
                if (null != mCmsExtraService) {
                    ret = mCmsExtraService.getCpuUsage();
                } else {
                    LogUtils.e(TAG, "getCpuUsageRate: mCmsExtraService is null");
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "getCpuUsageRate execution failed, " + e.getMessage());
            }
        }
        return ret;
    }

    public double getCpuTemp() {
        if (null == mCmsExtraService) {
            throw new NullPointerException("mCmsExtraService is null");
        }

        double ret = 0;
        synchronized (CmsExtraServiceManager.class) {
            try {
                if (null != mCmsExtraService) {
                    ret = mCmsExtraService.getCpuTemp();
                } else {
                    LogUtils.e(TAG, "getCpuTemperature: mCmsExtraService is null");
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "getCpuTemperature execution failed, " + e.getMessage());
            }
        }
        return ret;
    }

    public boolean isHdmiPlugged() {
        if (null == mCmsExtraService) {
            throw new NullPointerException("mCmsExtraService is null");
        }

        boolean ret = false;
        synchronized (CmsExtraServiceManager.class) {
            try {
                if (null != mCmsExtraService) {
                    ret = mCmsExtraService.isHdmiPlugged();
                } else {
                    LogUtils.e(TAG, "isHDMIPlugged: mCmsExtraService is null");
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "isHDMIPlugged execution failed, " + e.getMessage());
            }
        }
        return ret;
    }

    public String getHdmiStatus() {
        if (null == mCmsExtraService) {
            throw new NullPointerException("mCmsExtraService is null");
        }

        String ret = "";
        synchronized (CmsExtraServiceManager.class) {
            try {
                if (null != mCmsExtraService) {
                    ret = mCmsExtraService.isHdmiEnabled() ? "Enabled" : "Disabled";
                } else {
                    LogUtils.e(TAG, "getHDMIStatus: mCmsExtraService is null");
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "getHDMIStatus execution failed, " + e.getMessage());
            }
        }
        return ret;
    }

    public int setHdmiStatus(boolean isEnable) {
        if (null == mCmsExtraService) {
            throw new NullPointerException("mCmsExtraService is null");
        }

        int ret = 0;
        synchronized (CmsExtraServiceManager.class) {
            try {
                if (null != mCmsExtraService) {
                    ret = mCmsExtraService.setHdmiStatus(isEnable);
                } else {
                    LogUtils.e(TAG, "setHDMIStatus: mCmsExtraService is null");
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "setHDMIStatus execution failed, " + e.getMessage());
            }
        }
        return ret;
    }

    public String getHdmiSupportResolution() {
        if (null == mCmsExtraService) {
            throw new NullPointerException("mCmsExtraService is null");
        }

        String ret = "";
        synchronized (CmsExtraServiceManager.class) {
            try {
                if (null != mCmsExtraService) {
                    ret = mCmsExtraService.getHdmiSupportResolution();
                } else {
                    LogUtils.e(TAG, "getHdmiSupportResolution: mCmsExtraService is null");
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "getHdmiSupportResolution execution failed, " + e.getMessage());
            }
        }
        return ret;
    }

    public String getHdmiResolutionValue() {
        if (null == mCmsExtraService) {
            throw new NullPointerException("mCmsExtraService is null");
        }

        String ret = "";
        synchronized (CmsExtraServiceManager.class) {
            try {
                if (null != mCmsExtraService) {
                    ret = mCmsExtraService.getHdmiResolutionValue();
                } else {
                    LogUtils.e(TAG, "getHdmiResolutionValue: mCmsExtraService is null");
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "getHdmiResolutionValue execution failed, " + e.getMessage());
            }
        }
        return ret;
    }

    public int setHdmiResolutionValue(String mode) {
        if (null == mCmsExtraService) {
            throw new NullPointerException("mCmsExtraService is null");
        }

        int ret = 0;
        synchronized (CmsExtraServiceManager.class) {
            try {
                if (null != mCmsExtraService) {
                    ret = mCmsExtraService.setHdmiResolutionValue(mode);
                } else {
                    LogUtils.e(TAG, "setHdmiResolutionValue: mCmsExtraService is null");
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "setHdmiResolutionValue execution failed, " + e.getMessage());
            }
        }
        return ret;
    }

    public boolean isBestOutputMode() {
        if (null == mCmsExtraService) {
            throw new NullPointerException("mCmsExtraService is null");
        }

        boolean ret = false;
        synchronized (CmsExtraServiceManager.class) {
            try {
                if (null != mCmsExtraService) {
                    ret = mCmsExtraService.isBestOutputMode();
                } else {
                    LogUtils.e(TAG, "isBestOutputmode: mCmsExtraService is null");
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "isBestOutputmode execution failed, " + e.getMessage());
            }
        }
        return ret;
    }

    public boolean isHdmiCecSupport() {
        if (null == mCmsExtraService) {
            throw new NullPointerException("mCmsExtraService is null");
        }

        boolean ret = false;
        synchronized (CmsExtraServiceManager.class) {
            try {
                if (null != mCmsExtraService) {
                    ret = mCmsExtraService.isHdmiCecSupport();
                } else {
                    LogUtils.e(TAG, "isHdmiCecSupport: mCmsExtraService is null");
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "isHdmiCecSupport execution failed, " + e.getMessage());
            }
        }
        return ret;
    }

    public String getHdmiProductName() {
        if (null == mCmsExtraService) {
            throw new NullPointerException("mCmsExtraService is null");
        }

        String ret = "";
        synchronized (CmsExtraServiceManager.class) {
            try {
                if (null != mCmsExtraService) {
                    ret = mCmsExtraService.getHdmiProductName();
                } else {
                    LogUtils.e(TAG, "getHdmiProductName: mCmsExtraService is null");
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "getHdmiProductName execution failed, " + e.getMessage());
            }
        }
        return ret;
    }

    public String getHdmiEdidVersion() {
        if (null == mCmsExtraService) {
            throw new NullPointerException("mCmsExtraService is null");
        }

        String ret = "";
        synchronized (CmsExtraServiceManager.class) {
            try {
                if (null != mCmsExtraService) {
                    ret = mCmsExtraService.getHdmiEdidVersion();
                } else {
                    LogUtils.e(TAG, "getHdmiEdidVersion: mCmsExtraService is null");
                }
            } catch (Exception e) {
                LogUtils.e(TAG, "getHdmiEdidVersion execution failed, " + e.getMessage());
            }
        }
        return ret;
    }
}
