package com.sdt.diagnose.common;

import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageStats;
import android.os.UserHandle;
import android.text.format.Formatter;

import com.sdt.diagnose.common.bean.AppInfo;
import com.sdt.diagnose.common.log.LogUtils;

import java.io.IOException;
import java.text.Collator;
import java.util.Comparator;
import java.util.List;

public class AppsManager extends AbstractCachedArray<AppInfo> {
    private static final String TAG = "AppsManager";

    public AppsManager(Context context) {
        super(context);
    }

    @Override
    void buildList(Context context) {
        final PackageManager pm = context.getPackageManager();
        final StorageStatsManager mStats = context.getSystemService(StorageStatsManager.class);

        // 获取所有带<activity>的应用
        List<PackageInfo> packlist = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES);
        LogUtils.d(TAG, "InstalledPackages size: " + packlist.size());
        for (int i = 0; i < packlist.size(); i++) {
            PackageInfo pkgInfo = packlist.get(i);
            final ApplicationInfo info = pkgInfo.applicationInfo;
            if (info == null) {
                LogUtils.d(TAG, "PackageInfo is null.");
                continue;
            }
            boolean disable = ApplicationUtils.isDisable(pm, info.packageName);
            // 过滤掉无Activity的 Package
            if (disable && (pkgInfo.activities == null || pkgInfo.activities.length < 1)) {
                continue;
            }
            final String pkgName = pkgInfo.packageName;
            // 过滤掉不可以被open的应用
            if (disable && !ApplicationUtils.canOpen(pm, pkgName)) {
                continue;
            }
            // 过滤掉TR069和Diagnose应用
            if (pkgName.contains("tr069") || pkgName.contains("diagnose")) {
                continue;
            }

            AppInfo appInfo = new AppInfo(context, pm, pkgInfo);

            // storage
            int userId = UserHandle.myUserId();
            final StorageStats storageStats;
            try {
                storageStats = mStats.queryStatsForPackage(info.storageUuid, pkgName, UserHandle.of(userId));
                final long cacheQuota = mStats.getCacheQuotaBytes(info.storageUuid.toString(), info.uid);

//                /** 8.0 一下用PackageStats 获取*/
//                final PackageStats stats = new PackageStats(pkgName, userId);
//                stats.codeSize = storageStats.getAppBytes();
//                stats.dataSize = storageStats.getDataBytes();
//                stats.cacheSize = Math.min(storageStats.getCacheBytes(), cacheQuota);
//                long externalCodeSize = stats.externalCodeSize + stats.externalObbSize;
//                long externalDataSize = stats.externalDataSize + stats.externalMediaSize;
//                long externalCacheSize = stats.externalCacheSize;
//                long newSize = externalCodeSize + externalDataSize + getTotalInternalSize(stats);
//                long dataSize = stats.dataSize + externalDataSize;
//                long cacheSize = stats.cacheSize + externalCacheSize;
//                String sizeStr = getSizeStr(newSize);

                appInfo.setData(storageStats.getDataBytes());
                appInfo.setCache(storageStats.getCacheBytes());
                appInfo.setStorageUsed(storageStats.getAppBytes());

            } catch (PackageManager.NameNotFoundException | IOException e) {
                LogUtils.e(TAG, "Build APP list error, " + e.getMessage());
            }
            add(appInfo);
            LogUtils.d(TAG, "AppInfo: " + appInfo);
        }

        if (mList != null) {
            mList.sort(COMPARATOR);
            int size = mList.size();
            for (int i = 0; i < size; i++) {
                mList.get(i).setIndex(i);
            }
        }
    }

    private long getTotalInternalSize(PackageStats ps) {
        if (ps != null) {
            // We subtract the cache size because the system can clear it automatically and
            // |dataSize| is a superset of |cacheSize|.
            return ps.codeSize + ps.dataSize - ps.cacheSize;
        }
        return -2;
    }

    private String getSizeStr(Context context, long size) {
        if (size >= 0) {
            return Formatter.formatFileSize(context, size);
        }
        return null;
    }

    /**
     * Compare by label, then package name, then uid.
     */
    private static final Comparator<AppInfo> COMPARATOR = new Comparator<AppInfo>() {
        private final Collator sCollator = Collator.getInstance();

        @Override
        public int compare(AppInfo object1, AppInfo object2) {
            int compareResult = sCollator.compare(object1.getName(), object2.getName());
            if (compareResult != 0) {
                return compareResult;
            }
            compareResult =
                    sCollator.compare(object1.getPackageName(), object2.getPackageName());
            if (compareResult != 0) {
                return compareResult;
            }
            return object1.getUid() - object2.getUid();
        }
    };

}
