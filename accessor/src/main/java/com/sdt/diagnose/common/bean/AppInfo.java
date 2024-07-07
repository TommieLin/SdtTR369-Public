package com.sdt.diagnose.common.bean;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.UserHandle;

import com.sdt.diagnose.common.ApplicationUtils;
import com.sdt.diagnose.common.GlobalContext;

import java.text.SimpleDateFormat;

public class AppInfo {
    private PackageInfo packageInfo;
    private String name;
    private String packageName;
    private String version;
    private boolean isSystem;
    private boolean isInternel;
    private boolean isSystemSign;
    private boolean canStop;
    private boolean canOpen = true;
    private boolean canUninstall;        // 与 canUninstallUpdates 互斥
    private boolean canUninstallUpdates; // 与 canUninstall 互斥
    private boolean canShowEnable;       // 当此值为true时,才能执行enable\disable的操作
    private boolean enable;              // enable\disable 状态
    private boolean isUpdatedSystemApp;  //判断是否为系统apk升级
    private long lastUpdateTime;
    private long storageUsed;
    private long data;
    private long cache;

    private int index;

    public AppInfo(Context context, PackageManager pm, PackageInfo info) {
        packageInfo = info;
        name = loadLabel(pm);
        packageName = packageInfo.packageName;
        version = packageInfo.versionName;
        lastUpdateTime = packageInfo.lastUpdateTime;
        isSystem = isSystemApp();
        isInternel = packageInfo.applicationInfo.isInternal();
        canStop = canStop(context);
        canUninstall = canUninstall();
        canUninstallUpdates = canUninstallUpdates();
        enable = isEnabled();
        isSystemSign = isSignedWithPlatformKey();
        isUpdatedSystemApp = isUpdatedSystemApp();
        // showAble 表示是否应该显示 Enable/Disable 功能, isEnabled 表示当前应用是否 enable状态
        canShowEnable = canShowEnble(context, pm);
        storageUsed = getStorageUsed();
    }

    public String getName() {
        return name;
    }

    public String getPackageName() {
        return packageName;
    }

    public int getUid() {
        return packageInfo.applicationInfo.uid;
    }

    public String getVersion() {
        return version;
    }

    public boolean isRunning() {
        return ApplicationUtils.isAppRunning(GlobalContext.getContext(), packageName);
    }

    public boolean isCanStop() {
        return canStop;
    }

    public boolean isCanOpen() {
        return canOpen;
    }

    public boolean isCanUninstall() {
        return canUninstall;
    }

    public boolean isCanUninstallUpdates() {
        return canUninstallUpdates;
    }

    public boolean isCanShowEnable() {
        return canShowEnable;
    }

    public boolean isEnable() {
        return enable;
    }

    public boolean isSystem() {
        return isSystem;
    }

    public boolean isUpdatedSystem() {
        return isUpdatedSystemApp;
    }

    public String getLastUpdateTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return formatter.format(lastUpdateTime);
    }

    public long getStorageUsed() {
        return storageUsed;
    }

    public long getTotalSize() {
        return (storageUsed + data + cache);
    }

    public void setStorageUsed(long storageUsed) {
        this.storageUsed = storageUsed / 1024;
    }

    public long getData() {
        return data;
    }

    public void setData(long data) {
        this.data = data / 1024;
    }

    public long getCache() {
        return cache;
    }

    public void setCache(long cache) {
        this.cache = cache / 1024;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public String toString() {
        return "AppInfo{" +
                "packageName='" + packageName + "', name='" + name + "', version='" + version + '\'' +
                ", isSystem=" + isSystem +
                ", isInternel=" + isInternel +
                ", isSystemSign=" + isSystemSign +
                ", isUpdatedSystemApp=" + isUpdatedSystemApp +
                ", canStop=" + canStop +
                ", canOpen=" + canOpen +
                ", canUninstall=" + canUninstall +
                ", canUninstallUpdates=" + canUninstallUpdates +
                ", canShowEnable=" + canShowEnable +
                ", enable=" + enable +
                ", storageUsed='" + storageUsed + '\'' +
                ", data=" + data +
                ", cache=" + cache +
                ", index=" + index +
                '}';
    }

    private String loadLabel(PackageManager pm) {
        CharSequence label = packageInfo.applicationInfo.loadLabel(pm);
        if (label == null) {
            return packageName;
        }
        return label.toString();
    }

    boolean isSignedWithPlatformKey() {
        return packageInfo.applicationInfo.isSignedWithPlatformKey();
    }

    private boolean isSystemApp() {
        return packageInfo.applicationInfo.isSystemApp();
    }

    private boolean isUpdatedSystemApp() {
        return packageInfo.applicationInfo.isUpdatedSystemApp();
    }

    private boolean canStop(Context context) {
        if (packageInfo.applicationInfo.packageName.contains("tr069")
                || packageInfo.applicationInfo.packageName.contains("diagnose")
                || packageInfo.applicationInfo.packageName.contains("tr369"))
            return false;
        DevicePolicyManager dpm =
                (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        if (dpm.packageHasActiveAdmins(packageName)) {
            // User can't force stop device admin.
            return false;
        } else if ((packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_STOPPED) == 0) {
            // If the app isn't explicitly stopped, then always show the
            // force stop action.
            return true;
        } else {
            Intent intent = new Intent(Intent.ACTION_QUERY_PACKAGE_RESTART,
                    Uri.fromParts("package", packageName, null));
            intent.putExtra(Intent.EXTRA_PACKAGES, new String[]{packageName});
            intent.putExtra(Intent.EXTRA_UID, packageInfo.applicationInfo.uid);
            intent.putExtra(Intent.EXTRA_USER_HANDLE,
                    UserHandle.getUserId(packageInfo.applicationInfo.uid));
            context.sendOrderedBroadcast(intent, null, new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    canStop = getResultCode() != Activity.RESULT_CANCELED;
                }
            }, null, Activity.RESULT_CANCELED, null, null);
        }
        return false;
    }

    private boolean canUninstall() {
        return (packageInfo.applicationInfo.flags &
                (ApplicationInfo.FLAG_UPDATED_SYSTEM_APP | ApplicationInfo.FLAG_SYSTEM)) == 0;
    }

    private boolean canUninstallUpdates() {
        return (packageInfo.applicationInfo.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
    }

    private boolean isEnabled() {
        return packageInfo.applicationInfo.enabled;
    }

    private boolean canShowEnble(Context context, PackageManager pm) {
        return !canUninstall && ApplicationUtils.canDisable(context, pm, packageName);
    }

    public PackageInfo getPackageInfo() {
        return packageInfo;
    }
}
