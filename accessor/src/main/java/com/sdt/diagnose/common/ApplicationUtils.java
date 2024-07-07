package com.sdt.diagnose.common;

import static android.content.Context.ACTIVITY_SERVICE;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.Signature;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.print.PrintManager;
import android.text.TextUtils;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.nano.MetricsProto;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.common.net.HttpsUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ApplicationUtils {
    private static final String TAG = "ApplicationUtils";
    private static HandlerThread mHandlerThread = null;
    private static Handler mHandler = null;

    public static boolean uninstall(String pkg) {
        if (mHandlerThread == null) {
            mHandlerThread = new HandlerThread("AppUtilsThread", Process.THREAD_PRIORITY_BACKGROUND);
            mHandlerThread.start();
        }
        if (mHandler == null) {
            mHandler = new Handler(mHandlerThread.getLooper());
        }

        Context context = GlobalContext.getContext();
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent sender = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        PackageInstaller mPackageInstaller = context.getPackageManager().getPackageInstaller();
        // 卸载APK
        mPackageInstaller.uninstall(pkg, sender.getIntentSender());
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                HttpsUtils.uploadNotificationStatus(true);
            }
        }, 3 * 1000);
        return true;
    }

    public static boolean uninstallApps(String pkgs) {
        if (pkgs == null || pkgs.isEmpty()) return false;

        try {
            Gson gson = new Gson();
            ArrayList<String> packageNames = gson.fromJson(pkgs, new TypeToken<List<String>>(){}.getType());
            if (packageNames == null) {
                LogUtils.e(TAG, "The JSON data is empty");
                return false;
            }
            // 过滤空字符串或为null的元素
            packageNames.removeIf(TextUtils::isEmpty);
            LogUtils.i(TAG, "Waiting to uninstall apps: " + packageNames);
            if (packageNames.isEmpty()) {
                LogUtils.i(TAG, "The packageNames content is empty and no subsequent operations are required");
                return true;
            }

            final PackageManager pm = GlobalContext.getContext().getPackageManager();
            List<PackageInfo> packlist = pm.getInstalledPackages(0);
            for (int i = 0; i < packlist.size(); i++) {
                PackageInfo pkgInfo = packlist.get(i);
                final String pkgName = pkgInfo.packageName;
                if (packageNames.contains(pkgName)) {
                    if (!pkgName.contains("sdt")) {
                        uninstall(pkgName);
                        LogUtils.d(TAG, "Uninstallation process completed.");
                    } else {
                        LogUtils.i(TAG, "This application cannot be uninstalled: " + pkgName);
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to execute uninstallApps. " + e.getMessage());
            return false;
        }

        return true;
    }

    public static boolean stopApplication(String pkg) {
        Context context = GlobalContext.getContext();
        MetricsLogger.action(context, MetricsProto.MetricsEvent.ACTION_APP_FORCE_STOP, pkg);
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        am.forceStopPackage(pkg);
        return true;
    }

    public static boolean ableApplication(String pkg, boolean able) {
        Context context = GlobalContext.getContext();
        context.getPackageManager().setApplicationEnabledSetting(pkg, able
                        ? PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
                        : PackageManager.COMPONENT_ENABLED_STATE_DISABLED_USER,
                0);
        return true;
    }

    public static boolean openApp(String pkg) {
        Context context = GlobalContext.getContext();
        PackageManager pm = context.getPackageManager();
        Intent appLaunchIntent = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (appLaunchIntent == null) {
            appLaunchIntent = pm.getLaunchIntentForPackage(pkg);
        }
        context.startActivity(appLaunchIntent);
        return true;
    }

    public static boolean forceStopApp(String pkg) {
        Context context = GlobalContext.getContext();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        am.forceStopPackage(pkg);
        return true;
    }

    public static boolean clearData(String pkg) {
        Context context = GlobalContext.getContext();
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        return am.clearApplicationUserData(pkg, null);
    }

    public static boolean canOpen(PackageManager pm, String pkg) {
        Intent appLaunchIntent = pm.getLeanbackLaunchIntentForPackage(pkg);
        if (appLaunchIntent == null) {
            appLaunchIntent = pm.getLaunchIntentForPackage(pkg);
        }
        return appLaunchIntent != null;
    }

    public static boolean canDisable(Context context, PackageManager pm, String pkg) {
        final HashSet<String> homePackages = getHomePackages(pm);
        PackageInfo packageInfo;
        try {
            packageInfo = pm.getPackageInfo(pkg,
                    PackageManager.GET_DISABLED_COMPONENTS
                            | PackageManager.GET_UNINSTALLED_PACKAGES
                            | PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            LogUtils.e(TAG, "getPackageInfo error, " + e.getMessage());
            return false;
        }
        return !(homePackages.contains(pkg) || isSystemPackage(context.getResources(), pm, packageInfo));
    }

    public static boolean isDisable(PackageManager pm, String pkg) {
        PackageInfo packageInfo;
        try {
            packageInfo = pm.getPackageInfo(pkg,
                    PackageManager.GET_DISABLED_COMPONENTS
                            | PackageManager.GET_UNINSTALLED_PACKAGES
                            | PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            LogUtils.e(TAG, "getPackageInfo error, " + e.getMessage());
            return false;
        }
        return packageInfo.applicationInfo.enabled;
    }

    private static HashSet<String> getHomePackages(PackageManager pm) {
        HashSet<String> homePackages = new HashSet<>();
        // Get list of "home" apps and trace through any meta-data references
        List<ResolveInfo> homeActivities = new ArrayList<>();
        pm.getHomeActivities(homeActivities);
        for (ResolveInfo ri : homeActivities) {
            final String activityPkg = ri.activityInfo.packageName;
            homePackages.add(activityPkg);
            // Also make sure to include anything proxying for the home app
            final Bundle metadata = ri.activityInfo.metaData;
            if (metadata != null) {
                final String metaPkg = metadata.getString(ActivityManager.META_HOME_ALTERNATE);
                if (signaturesMatch(pm, metaPkg, activityPkg)) {
                    homePackages.add(metaPkg);
                }
            }
        }
        return homePackages;
    }

    private static boolean signaturesMatch(PackageManager pm, String pkg1, String pkg2) {
        if (pkg1 != null && pkg2 != null) {
            try {
                final int match = pm.checkSignatures(pkg1, pkg2);
                if (match >= PackageManager.SIGNATURE_MATCH) {
                    return true;
                }
            } catch (Exception e) {
                // e.g. named alternate package not found during lookup;
                // this is an expected case sometimes
                LogUtils.e(TAG, "signaturesMatch checkSignatures error, " + e.getMessage());
            }
        }
        return false;
    }

    private static Signature[] sSystemSignature;
    private static String sPermissionControllerPackageName;
    private static String sServicesSystemSharedLibPackageName;
    private static String sSharedSystemSharedLibPackageName;

    /**
     * Determine whether a package is a "system package", in which case certain things (like
     * disabling notifications or disabling the package altogether) should be disallowed.
     */
    private static boolean isSystemPackage(Resources resources, PackageManager pm, PackageInfo pkg) {
        if (sSystemSignature == null) {
            sSystemSignature = new Signature[]{getSystemSignature(pm)};
        }
        if (sPermissionControllerPackageName == null) {
            sPermissionControllerPackageName = pm.getPermissionControllerPackageName();
        }
        if (sServicesSystemSharedLibPackageName == null) {
            sServicesSystemSharedLibPackageName = pm.getServicesSystemSharedLibraryPackageName();
        }
        if (sSharedSystemSharedLibPackageName == null) {
            sSharedSystemSharedLibPackageName = pm.getSharedSystemSharedLibraryPackageName();
        }
        return (sSystemSignature[0] != null
                && sSystemSignature[0].equals(getFirstSignature(pkg)))
                || pkg.packageName.equals(sPermissionControllerPackageName)
                || pkg.packageName.equals(sServicesSystemSharedLibPackageName)
                || pkg.packageName.equals(sSharedSystemSharedLibPackageName)
                || pkg.packageName.equals(PrintManager.PRINT_SPOOLER_PACKAGE_NAME)
                || isDeviceProvisioningPackage(resources, pkg.packageName);
    }

    private static Signature getFirstSignature(PackageInfo pkg) {
        if (pkg != null && pkg.signatures != null && pkg.signatures.length > 0) {
            return pkg.signatures[0];
        }
        return null;
    }

    private static Signature getSystemSignature(PackageManager pm) {
        try {
            final PackageInfo sys = pm.getPackageInfo("android", PackageManager.GET_SIGNATURES);
            return getFirstSignature(sys);
        } catch (PackageManager.NameNotFoundException e) {
            LogUtils.e(TAG, "SystemSignature getPackageInfo error, " + e.getMessage());
        }
        return null;
    }

    /**
     * Returns {@code true} if the supplied package is the device provisioning app. Otherwise,
     * returns {@code false}.
     */
    private static boolean isDeviceProvisioningPackage(Resources resources, String packageName) {
        String deviceProvisioningPackage = resources.getString(
                com.android.internal.R.string.config_deviceProvisioningPackage);
        return deviceProvisioningPackage != null && deviceProvisioningPackage.equals(packageName);
    }

    public static boolean isAppRunning(Context context, String packageName) {
        PackageInfo pi = new PackageInfo();
        ActivityManager am = (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runs = am.getRunningAppProcesses();
        LogUtils.d(TAG, "runs.size(): " + runs.size());
        for (ActivityManager.RunningAppProcessInfo run : runs) {
            for (String pkg : run.pkgList) {
                if (pkg.contains(packageName)) {
                    return true;
                }
            }
        }
        return false;
    }

}
