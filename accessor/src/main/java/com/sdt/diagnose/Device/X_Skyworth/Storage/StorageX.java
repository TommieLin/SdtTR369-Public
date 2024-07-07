package com.sdt.diagnose.Device.X_Skyworth.Storage;

import android.app.ActivityManager;
import android.app.usage.ExternalStorageStats;
import android.app.usage.StorageStats;
import android.app.usage.StorageStatsManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.UserHandle;
import android.os.storage.StorageManager;
import android.os.storage.VolumeInfo;
import android.text.TextUtils;
import android.text.format.Formatter;
import android.util.SparseArray;
import android.util.SparseLongArray;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sdt.annotations.Tr369Get;
import com.sdt.annotations.Tr369Set;
import com.sdt.diagnose.common.ApplicationUtils;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.log.LogUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * This class for protocol:
 * Device.X_Skyworth.Storage.Info               - get接口：获取Storage列表信息
 * Device.X_Skyworth.Storage.AppsInfo           - get接口：获取Storage中各APPs列表信息
 * Device.X_Skyworth.Storage.Clear              - set接口：清理所有Apps的Cache数据，用作全局清理Storage
 * Device.X_Skyworth.Storage.ClearAppsData      - set接口：清理单个或多个指定App的Data数据
 * Device.X_Skyworth.Storage.ClearAppsCache     - set接口：清理单个或多个指定App的Cache数据
 */
public class StorageX {
    private static final String TAG = "StorageX";
    private static final String STR_APPS_USAGE = "Apps";
    private static final String STR_DCIM_USAGE = "Photos & Videos";
    private static final String STR_MUSIC_USAGE = "Audio";
    private static final String STR_DOWNLOADS_USAGE = "Downloads";
    private static final String STR_CACHE_USAGE = "Cached data";
    private static final String STR_MISC_USAGE = "Misc.";
    private static final String STR_AVAILABLE = "Available";

    public static class MeasurementDetails {
        /**
         * Size of storage device.
         */
        public long totalSize;
        /**
         * Size of available space.
         */
        public long availSize;
        /**
         * Size of all cached data.
         */
        public long cacheSize;

        /**
         * Total disk space used by everything.
         * <p>
         * Key is {@link UserHandle}.
         */
        public SparseLongArray usersSize = new SparseLongArray();

        /**
         * Total disk space used by apps.
         * <p>
         * Key is {@link UserHandle}.
         */
        public SparseLongArray appsSize = new SparseLongArray();

        /**
         * Total disk space used by media on shared storage.
         * <p>
         * First key is {@link UserHandle}. Second key is media type, such as
         * {@link Environment#DIRECTORY_PICTURES}.
         */
        public SparseArray<HashMap<String, Long>> mediaSize = new SparseArray<>();

        /**
         * Total disk space used by non-media on shared storage.
         * <p>
         * Key is {@link UserHandle}.
         */
        public SparseLongArray miscSize = new SparseLongArray();

        @NonNull
        @Override
        public String toString() {
            return "MeasurementDetails: [totalSize: " + totalSize + " availSize: " + availSize
                    + " cacheSize: " + cacheSize + " mediaSize: " + mediaSize
                    + " miscSize: " + miscSize + "usersSize: " + usersSize + "]";
        }
    }

    private void addValue(SparseLongArray array, int key, long value) {
        array.put(key, array.get(key) + value);
    }

    private static long totalValues(HashMap<String, Long> map, String... keys) {
        long total = 0;
        if (map != null) {
            for (String key : keys) {
                if (map.containsKey(key)) {
                    total += map.get(key);
                }
            }
        } else {
            LogUtils.e(TAG, "MeasurementDetails mediaSize array does not have key for current user " +
                    ActivityManager.getCurrentUser());
        }
        return total;
    }

    private HashMap<String, String> setStorageInfoMap(String name, long size) {
        final HashMap<String, String> map = new LinkedHashMap<>();
        map.put("Name", name);
        map.put("Size", Formatter.formatShortFileSize(GlobalContext.getContext(), size));
        return map;
    }

    private HashMap<String, String> setAppsInfoMap(String name, String pkgName, long size) {
        final HashMap<String, String> map = new LinkedHashMap<>();
        map.put("Name", name);
        map.put("PackageName", pkgName);
        map.put("Size", Formatter.formatShortFileSize(GlobalContext.getContext(), size));
        return map;
    }

    private MeasurementDetails measureExactStorage() {
        final MeasurementDetails details = new MeasurementDetails();
        final StorageManager sm = GlobalContext.getContext().getSystemService(StorageManager.class);
        final StorageStatsManager ssm = GlobalContext.getContext().getSystemService(StorageStatsManager.class);
        if (sm == null || ssm == null) return details;

        final int userId = UserHandle.myUserId();

        final List<VolumeInfo> volumes = sm.getVolumes();
        VolumeInfo volume = null;
        for (VolumeInfo vol : volumes) {
            if (vol.getType() == VolumeInfo.TYPE_PRIVATE && vol.isMountedReadable()) {
                volume = vol;
            }
        }
        if (volume == null) return details;

        try {
            details.totalSize = volume.getPath().getTotalSpace();
            details.availSize = sm.getAllocatableBytes(StorageManager.convert(volume.fsUuid));
        } catch (IOException e) {
            // The storage volume became null while we were measuring it.
            LogUtils.e(TAG, "Failed to get total bytes and free bytes. " + e.getMessage());
            return details;
        }

        {
            final ExternalStorageStats stats;
            final HashMap<String, Long> mediaMap = new HashMap<>();
            details.mediaSize.put(userId, mediaMap);

            try {
                stats = ssm.queryExternalStatsForUser(volume.fsUuid, UserHandle.of(userId));
            } catch (IOException e) {
                LogUtils.e(TAG, "Failed to execute queryExternalStatsForUser. " + e.getMessage());
                return details;
            }

            addValue(details.usersSize, userId, stats.getTotalBytes());

            // Track detailed data types
            mediaMap.put(Environment.DIRECTORY_MUSIC, stats.getAudioBytes());
            mediaMap.put(Environment.DIRECTORY_MOVIES, stats.getVideoBytes());
            mediaMap.put(Environment.DIRECTORY_PICTURES, stats.getImageBytes());

            final long miscBytes = stats.getTotalBytes() - stats.getAudioBytes()
                    - stats.getVideoBytes() - stats.getImageBytes();
            addValue(details.miscSize, userId, miscBytes);
        }

        {
            final StorageStats stats;
            try {
                stats = ssm.queryStatsForUser(volume.fsUuid, UserHandle.of(userId));
            } catch (IOException e) {
                LogUtils.e(TAG, "Failed to execute queryStatsForUser. " + e.getMessage());
                return details;
            }

            addValue(details.usersSize, userId, stats.getAppBytes());
            addValue(details.usersSize, userId, stats.getDataBytes());
            addValue(details.appsSize, userId, stats.getAppBytes() + stats.getDataBytes());

            details.cacheSize += stats.getCacheBytes();
        }

        return details;
    }

    @Tr369Get("Device.X_Skyworth.Storage.Info")
    public String SK_TR369_GetStorageInfo() {
        MeasurementDetails details = measureExactStorage();
        LogUtils.d(TAG, "Get storage details: " + details);

        final int currentUser = ActivityManager.getCurrentUser();
        final long dcimSize = totalValues(details.mediaSize.get(currentUser),
                Environment.DIRECTORY_DCIM,
                Environment.DIRECTORY_MOVIES, Environment.DIRECTORY_PICTURES);

        final long musicSize = totalValues(details.mediaSize.get(currentUser),
                Environment.DIRECTORY_MUSIC,
                Environment.DIRECTORY_ALARMS, Environment.DIRECTORY_NOTIFICATIONS,
                Environment.DIRECTORY_RINGTONES, Environment.DIRECTORY_PODCASTS);

        final long downloadsSize = totalValues(details.mediaSize.get(currentUser),
                Environment.DIRECTORY_DOWNLOADS);

        final long appsSize = details.appsSize.get(currentUser);
        final long availSize = details.availSize;
        final long cacheSize = details.cacheSize;
        final long miscSize = details.miscSize.get(currentUser);

        final List<HashMap<String, String>> storageInfo = new ArrayList<>();
        storageInfo.add(setStorageInfoMap(STR_APPS_USAGE, appsSize));
        storageInfo.add(setStorageInfoMap(STR_DCIM_USAGE, dcimSize));
        storageInfo.add(setStorageInfoMap(STR_MUSIC_USAGE, musicSize));
        storageInfo.add(setStorageInfoMap(STR_DOWNLOADS_USAGE, downloadsSize));
        storageInfo.add(setStorageInfoMap(STR_CACHE_USAGE, cacheSize));
        storageInfo.add(setStorageInfoMap(STR_MISC_USAGE, miscSize));
        storageInfo.add(setStorageInfoMap(STR_AVAILABLE, availSize));

        return storageInfo.toString();
    }

    @Tr369Get("Device.X_Skyworth.Storage.AppsInfo")
    public String SK_TR369_GetStorageAppsInfo() {
        final List<HashMap<String, String>> appsInfo = new ArrayList<>();
        final PackageManager pm = GlobalContext.getContext().getPackageManager();
        final StorageStatsManager ssm = GlobalContext.getContext().getSystemService(StorageStatsManager.class);
        final List<PackageInfo> infos = pm.getInstalledPackages(0);

        for (PackageInfo info : infos) {
            final String name = info.applicationInfo.loadLabel(pm).toString();
            final String pkgName = info.packageName;

            int userId = UserHandle.myUserId();
            final StorageStats storageStats;
            try {
                storageStats = ssm.queryStatsForPackage(info.applicationInfo.storageUuid,
                        pkgName, UserHandle.of(userId));
                long data = storageStats.getDataBytes();
                long cache = storageStats.getCacheBytes();
                long storageUsed = storageStats.getAppBytes();
                appsInfo.add(setAppsInfoMap(name, pkgName, (data + cache + storageUsed)));
            } catch (Exception e) {
                LogUtils.e(TAG, "Failed to execute queryStatsForPackage. " + e.getMessage());
            }
        }

        return appsInfo.toString();
    }

    @Tr369Set("Device.X_Skyworth.Storage.Clear")
    public boolean SK_TR369_HandleClearStorageCache(String path, String value) {
        if (value == null || value.isEmpty()) return false;
        if (TextUtils.equals(value, "1") || TextUtils.equals(value, "true")) {
            final PackageManager pm = GlobalContext.getContext().getPackageManager();
            final List<PackageInfo> infos = pm.getInstalledPackages(0);
            for (PackageInfo info : infos) {
                LogUtils.d(TAG, "handleClearStorageCache pkgName: " + info.packageName);
                pm.deleteApplicationCacheFiles(info.packageName, null);
            }
            return true;
        }
        return false;
    }

    @Tr369Set("Device.X_Skyworth.Storage.ClearAppsData")
    public boolean SK_TR369_HandleClearAppsData(String path, String value) {
        if (value == null || value.isEmpty()) return false;

        try {
            Gson gson = new Gson();
            ArrayList<String> packageNames = gson.fromJson(value, new TypeToken<List<String>>(){}.getType());
            if (packageNames == null) {
                LogUtils.e(TAG, "The JSON data is empty");
                return false;
            }
            // 过滤空字符串或为null的元素
            packageNames.removeIf(TextUtils::isEmpty);
            LogUtils.i(TAG, "Waiting to handle apps: " + packageNames);
            if (packageNames.isEmpty()) {
                LogUtils.i(TAG, "The packageNames content is empty and no subsequent operations are required");
                return true;
            }

            final PackageManager pm = GlobalContext.getContext().getPackageManager();
            final List<PackageInfo> infos = pm.getInstalledPackages(0);
            for (PackageInfo info : infos) {
                final String pkgName = info.packageName;
                if (packageNames.contains(pkgName)) {
                    LogUtils.d(TAG, "Waiting to clear app data: " + pkgName);
                    ApplicationUtils.clearData(pkgName);
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to execute handleClearAppsData. " + e.getMessage());
            return false;
        }

        return true;
    }

    @Tr369Set("Device.X_Skyworth.Storage.ClearAppsCache")
    public boolean SK_TR369_HandleClearAppsCache(String path, String value) {
        if (value == null || value.isEmpty()) return false;

        try {
            Gson gson = new Gson();
            ArrayList<String> packageNames = gson.fromJson(value, new TypeToken<List<String>>(){}.getType());
            if (packageNames == null) {
                LogUtils.e(TAG, "The JSON data is empty");
                return false;
            }
            // 过滤空字符串或为null的元素
            packageNames.removeIf(TextUtils::isEmpty);
            LogUtils.i(TAG, "Waiting to handle apps: " + packageNames);
            if (packageNames.isEmpty()) {
                LogUtils.i(TAG, "The packageNames content is empty and no subsequent operations are required");
                return true;
            }

            final PackageManager pm = GlobalContext.getContext().getPackageManager();
            final List<PackageInfo> infos = pm.getInstalledPackages(0);
            for (PackageInfo info : infos) {
                String pkgName = info.packageName;
                if (packageNames.contains(pkgName)) {
                    LogUtils.d(TAG, "Waiting to clear app cache: " + pkgName);
                    pm.deleteApplicationCacheFiles(pkgName, null);
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "Failed to execute handleClearAppsCache. " + e.getMessage());
            return false;
        }

        return true;
    }

}
