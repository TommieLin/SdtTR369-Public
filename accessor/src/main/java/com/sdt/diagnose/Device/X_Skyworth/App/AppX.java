package com.sdt.diagnose.Device.X_Skyworth.App;

import static com.sdt.diagnose.Device.X_Skyworth.ActivityStartWatcher.TYPE_BLACKLIST;
import static com.sdt.diagnose.Device.X_Skyworth.ActivityStartWatcher.TYPE_UNDEFINED;
import static com.sdt.diagnose.Device.X_Skyworth.ActivityStartWatcher.TYPE_WHITELIST;

import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.ArrayMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.sdt.annotations.Tr369Get;
import com.sdt.annotations.Tr369Set;
import com.sdt.diagnose.Device.X_Skyworth.ActivityStartWatcher;
import com.sdt.diagnose.Device.X_Skyworth.App.PermissionControl.AppPermissionControl;
import com.sdt.diagnose.Device.X_Skyworth.App.PermissionControl.model.AppPermissionGroup;
import com.sdt.diagnose.Device.X_Skyworth.App.PermissionControl.model.Permission;
import com.sdt.diagnose.common.ApplicationUtils;
import com.sdt.diagnose.common.AppsManager;
import com.sdt.diagnose.common.GlobalContext;
import com.sdt.diagnose.common.IProtocolArray;
import com.sdt.diagnose.common.ProtocolPathUtils;
import com.sdt.diagnose.common.bean.AppInfo;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.database.DbManager;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * This class for protocol:
 * Device.X_Skyworth.App.{i}.Type               - 预制(System)\安装(ThirdParty)
 * Device.X_Skyworth.App.{i}.isUpdatedSystem    - 1(系统apk升级)\0（其他apk）
 * Device.X_Skyworth.App.{i}.BlockStatus        - disable \enable
 * Device.X_Skyworth.App.{i}.BlockEnable        - can disable\enable
 * Device.X_Skyworth.App.{i}.PackageName        - 包名
 * Device.X_Skyworth.App.{i}.Size               - 占用空间大小. apk + cache + data
 * Device.X_Skyworth.App.{i}.Version            - 版本
 * Device.X_Skyworth.App.{i}.Name               - 应用名称
 * Device.X_Skyworth.App.{i}.Running            - 是否在运行
 * Device.X_Skyworth.App.{i}.LastUpdatedTime    - 升级时间
 */
public class AppX implements IProtocolArray<AppInfo> {
    private static final String TAG = "AppX";
    private final static String REFIX = "Device.X_Skyworth.App.";
    private static final ArrayMap<String, AppPermissionGroup>
            mPermissionNameToGroup = new ArrayMap<>();
    private static ArrayList<AppPermissionGroup> mGroups = new ArrayList<>();
    public UsageStatsManager mUsageStatsManager =
            (UsageStatsManager) GlobalContext.getContext().getSystemService(
                    Context.USAGE_STATS_SERVICE);
    public List<UsageStats> totalList;
    final static Map<String, ArrayList<AppPermissionGroup>> appPermissionGroup = new HashMap<>();
    private static AppsManager mAppsManager = null;
    public static final String PROP_TMS_APP_FIREWALL_TYPE = "persist.sys.tms.app.firewall.type";
    public static final String PROP_TMS_APP_FIREWALL_NUM = "persist.sys.tms.app.firewall.number";
    public static final String PROP_TMS_APP_FIREWALL_PART = "persist.sys.tms.app.firewall.part";
    public static final String NODE_APP_BATCH_LOCKLIST_LIST = "Device.X_Skyworth.AppBatch.LockList.List";
    public static final String NODE_APP_BATCH_LOCKLIST_ENABLE = "Device.X_Skyworth.AppBatch.LockList.Enable";
    public static final String NODE_APP_BATCH_BLACKLIST_LIST = "Device.X_Skyworth.AppBatch.BlackList.List";
    public static final String NODE_APP_BATCH_BLACKLIST_ENABLE = "Device.X_Skyworth.AppBatch.BlackList.Enable";
    public static final String NODE_APP_BATCH_WHITELIST_LIST = "Device.X_Skyworth.AppBatch.WhiteList.List";
    public static final String NODE_APP_BATCH_WHITELIST_ENABLE = "Device.X_Skyworth.AppBatch.WhiteList.Enable";

    public static void updateAppList() {
        if (mAppsManager != null) {
            if (!mAppsManager.isEmpty()) {
                mAppsManager.clear();
            }
            mAppsManager = null;
        }
        mAppsManager = new AppsManager(GlobalContext.getContext());
        int size = mAppsManager.getList().size();
        LogUtils.d(TAG, "Get the number of App List: " + size);
        if (size > 0) {
            DbManager.updateMultiObject("Device.X_Skyworth.App", size);
            for (int i = 0; i < size; i++) {
                AppInfo appInfo = mAppsManager.getList().get(i);
                addPermission(appInfo, String.valueOf(i + 1), false);
            }
        } else {
            DbManager.delMultiObject("Device.X_Skyworth.App");
        }
    }

    @Tr369Get("Device.X_Skyworth.App.")
    public String SK_TR369_GetAppInfo(String path) {
        return handleAppPath(path);
    }

    private String handleAppPath(String path) {
        return ProtocolPathUtils.getInfoFromArray(REFIX, path, this);
    }

    @Override
    public List<AppInfo> getArray() {
        if (mAppsManager == null) {
            mAppsManager = new AppsManager(GlobalContext.getContext());
        }
        return mAppsManager.getList();
    }

    public static void addPermission(AppInfo t, @NotNull String paramsArr, boolean isAdded) {
        mPermissionNameToGroup.clear();
        mGroups = new ArrayList<>();
        PackageInfo packageInfo = t.getPackageInfo();
        try {
            packageInfo = GlobalContext.getContext().createPackageContextAsUser(
                            packageInfo.packageName, 0,
                            UserHandle.getUserHandleForUid(packageInfo.applicationInfo.uid))
                    .getPackageManager().getPackageInfo(packageInfo.packageName,
                            PackageManager.GET_PERMISSIONS);
        } catch (PackageManager.NameNotFoundException e) {
            LogUtils.e(TAG, "Failed to add permission info. Error: " + e.getMessage());
        }

        if (packageInfo.requestedPermissions != null) {
            for (String requestedPerm : packageInfo.requestedPermissions) {
                if (mPermissionNameToGroup.get(requestedPerm) == null) {
                    AppPermissionGroup group = AppPermissionGroup.create(
                            GlobalContext.getContext(),
                            packageInfo,
                            requestedPerm,
                            false);
                    if (group == null) {
                        continue;
                    }
                    mGroups.add(group);
                    appPermissionGroup.put(paramsArr, mGroups);
                    addAllPermissions(group);
                    AppPermissionGroup backgroundGroup = group.getBackgroundPermissions();
                    if (backgroundGroup != null) {
                        addAllPermissions(backgroundGroup);
                    }
                }
            }
        }
        String path = "Device.X_Skyworth.App." + paramsArr + ".Permissions";
        LogUtils.d(TAG, "addPermission path: " + path + ", Groups size: " + mGroups.size());
        if (mGroups.size() > 0 && !isAdded) {
            DbManager.updateMultiObject(path, mGroups.size());
        }
    }

    private static void addAllPermissions(AppPermissionGroup group) {
        ArrayList<Permission> perms = group.getPermissions();
        int numPerms = perms.size();
        for (int permNum = 0; permNum < numPerms; permNum++) {
            mPermissionNameToGroup.put(perms.get(permNum).getName(), group);
        }
    }

    @Override
    public String getValue(AppInfo t, @NotNull String[] paramsArr) {
        if (paramsArr.length < 2) {
            return null;
        }
        String secondParam = paramsArr[1];
        String thirdParam = paramsArr.length >= 3 ? paramsArr[2] : "";
        String forthParam = paramsArr.length >= 4 ? paramsArr[3] : "";
        if (TextUtils.isEmpty(secondParam)) {
            // Todo report error.
            return null;
        }
        switch (secondParam) {
            case "Type":
                return t.isSystem() ? "System" : "ThirdParty";
            case "PackageName":
                return t.getPackageName();
            case "Name":
                return t.getName();
            case "Version":
                return t.getVersion();
            case "Running":
                return t.isRunning() ? "1" : "0";
            case "BlockStatus":
                return t.isEnable() ? "1" : "0";
            case "BlockEnable":
                return t.isCanShowEnable() ? "1" : "0";
            case "Size":
                return String.valueOf(t.getTotalSize());
            case "isUpdatedSystem":
                LogUtils.d(TAG, "isUpdatedSystem: " + t.isUpdatedSystem());
                return t.isUpdatedSystem() ? "1" : "0";
            case "LastUpdatedTime":
                return t.getLastUpdateTime();
            case "StopEnable":
                return t.isCanStop() ? "1" : "0";
            case "MemoryUsage":
                return t.isRunning() ? getMemoryUsed(t.getPackageName()).trim() : "0";
            case "StorageUsage":
                return String.valueOf(t.getStorageUsed());
            case "Uptime":
            case "TotalUptime":
                UsageStats uptimeUsageStats = getUsageStats(t.getPackageName());
                return uptimeUsageStats != null
                        ? String.valueOf(uptimeUsageStats.getTotalTimeInForeground() / 1000)
                        : "0";
            case "RunningTimes":
                UsageStats runningTimesUsageStats = getUsageStats(t.getPackageName());
                return runningTimesUsageStats != null
                        ? String.valueOf(runningTimesUsageStats.mLaunchCount)
                        : "0";
            case "LastStartTime":
                UsageStats lastStartTimeUsageStats = getUsageStats(t.getPackageName());
                return lastStartTimeUsageStats != null
                        ? String.valueOf(lastStartTimeUsageStats.getLastTimeStamp())
                        : "0";
            case "CpuUsage":
                return getCpuUsed(t.getPackageName()).trim();
            case "ClearData":
                return "0";
            case "PermissionsNumberOfEntries":
                return String.valueOf(mGroups.size());
            case "Permissions":
                if (TextUtils.isEmpty(thirdParam)) {
                    // Todo report error.
                    return null;
                }
                try {
                    switch (forthParam) {
                        case "Name":
                            return Objects.requireNonNull(appPermissionGroup.get(paramsArr[0]))
                                    .get(Integer.parseInt(thirdParam) - 1)
                                    .getName();
                        case "Label":
                            return Objects.requireNonNull(appPermissionGroup.get(paramsArr[0]))
                                    .get(Integer.parseInt(thirdParam) - 1)
                                    .getLabel().toString();
                        case "Granted":
                            boolean isGranted = AppPermissionControl.areRuntimePermissionsGranted(
                                    GlobalContext.getContext(),
                                    t.getPackageName(),
                                    Objects.requireNonNull(appPermissionGroup.get(paramsArr[0]))
                                            .get(Integer.parseInt(thirdParam) - 1)
                                            .getName()
                            );
                            return String.valueOf(isGranted);
                        case "CanModify":
                            return (!Objects.requireNonNull(appPermissionGroup.get(paramsArr[0]))
                                    .get(Integer.parseInt(thirdParam) - 1)
                                    .isSystemFixed()
                                    && !Objects.requireNonNull(
                                            appPermissionGroup.get(paramsArr[0]))
                                    .get(Integer.parseInt(thirdParam) - 1)
                                    .isPolicyFixed()) + "";
                        default:
                            break;
                    }
                } catch (Exception e) {
                    LogUtils.e(TAG, "Failed to obtain permission info. Error: " + e.getMessage());
                    return null;
                }
        }
        return null;
    }

    @Tr369Set("Device.X_Skyworth.App.")
    public boolean SK_TR369_HandleAppInfoSetCmd(String path, String value) {
        LogUtils.d(TAG, "Set the path for app info: " + path);
        AppInfo appInfo = getAppByPath(path);
        if (appInfo == null) return false;

        String pkg = appInfo.getPackageName();
        if (TextUtils.isEmpty(pkg))
            return false;

        if (path.endsWith(".BlockStatus")) {
            if (TextUtils.equals(value, "0")) {
                return ApplicationUtils.ableApplication(pkg, false);
            } else if (TextUtils.equals(value, "1")) {
                return ApplicationUtils.ableApplication(pkg, true);
            }
        } else if (path.endsWith(".Running")) {
            if (DbManager.getDBParam("Device.X_Skyworth.Lock.Enable").equals("1")) {
                return false;
            }
            if (TextUtils.equals(value, "0")) {
                if (pkg.contains("tr069") || pkg.contains("diagnose") || pkg.contains("tr369"))
                    return true;
                return ApplicationUtils.forceStopApp(pkg);
            } else if (TextUtils.equals(value, "1")) {
                return ApplicationUtils.openApp(pkg);
            }
        } else if (path.endsWith(".ClearData")) {
            if (TextUtils.equals(value, "1")) {
                if (pkg.contains("tr069") || pkg.contains("tr369") || pkg.contains("diagnose")) {
                    return true;
                }
                return ApplicationUtils.clearData(pkg);
            }
        } else if (path.contains("Permissions")) {
            if (path.endsWith("Granted")) {
                try {
                    String[] paths = path.split("\\.");
                    boolean success =
                            AppPermissionControl.changeRuntimePermissions(
                                    GlobalContext.getContext(), pkg, Objects.requireNonNull(
                                            appPermissionGroup.get(path.split("\\.")[3])).get(
                                            Integer.parseInt(paths[5]) - 1).getName(),
                                    Boolean.parseBoolean(value));
                    addPermission(appInfo, paths[3], true);
                    return success;
                } catch (Exception e) {
                    LogUtils.e(TAG, "Failed to set permission info. Error: " + e.getMessage());
                }
            }
        }
        return false;
    }

    /**
     * 从 path 中获取到 i ，然后获取到App list 中第 i 个 app
     *
     * @return 第 i 个APP的包名
     */
    private AppInfo getAppByPath(String path) {
        AppInfo appInfo = null;
        String[] params = ProtocolPathUtils.parse(REFIX, path);
        if (params == null || params.length < 1) {
            // Todo report error.
            return null;
        }
        int index = 0;
        try {
            index = Integer.parseInt(params[0]);
            if (index < 1) {
                // Todo report error.
                return null;
            }
        } catch (NumberFormatException e) {
            // Todo report error.
            return null;
        }

        if (mAppsManager == null) {
            mAppsManager = new AppsManager(GlobalContext.getContext());
        }
        List<AppInfo> apps = mAppsManager.getList();
        if (apps == null || apps.size() < 1 || index > apps.size()) {
            return null;
        }

        try {
            appInfo = apps.get(index - 1);
            LogUtils.d(TAG, "Appx: " + appInfo.toString() + ", path: " + path);
        } catch (Exception e) {
            return null;
        }
        return appInfo;
    }

    public UsageStats getUsageStats(String packageName) {
        Calendar calendar = Calendar.getInstance();
        long endTime = calendar.getTimeInMillis();
        calendar.add(Calendar.DAY_OF_WEEK, -2); // 最近两周启动过所用app的List
        long startTime = calendar.getTimeInMillis();
        if (endTime - startTime > 0) {
            LogUtils.d(TAG, "queryUsageStats " + startTime + " ~ " + endTime);
            totalList = mUsageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_MONTHLY,
                    startTime,
                    endTime);
        }
        if (totalList.size() != 0) {
            for (UsageStats usageStats : totalList) {
                if (usageStats.getPackageName().equals(packageName)) {
                    LogUtils.d(TAG, "Found usage of package name(" + packageName
                            + ") within two weeks");
                    return usageStats;
                }
            }
        }
        LogUtils.e(TAG, "No usage of package name(" + packageName + ") was found within two weeks");
        return null;
    }

    public String getMemoryUsed(String packageName) {
        return getUsage("dumpsys meminfo -s " + packageName, packageName);
    }

    public String getCpuUsed(String packageName) {
        return getUsage("dumpsys cpuinfo", packageName);
    }

    public String getUsage(String command, String packageName) {
        Process process = null;
        BufferedReader bufferedReader = null;
        float cpuUsage = 0;

        try {
            process = Runtime.getRuntime().exec(command);
            bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (command.contains("cpuinfo")) {
                    if (line.contains(packageName) && line.trim().length() > 1) {
                        LogUtils.d(TAG, "The data obtained from the '" + command +
                                "' command is: " + line);
                        float ret = Float.parseFloat(line.split("%", 2)[0]);
                        cpuUsage += ret;
                        LogUtils.d(TAG, "App: " + packageName + ", CPU usage: " + cpuUsage + "%");
                    }
                } else if (command.contains("meminfo")) {
                    if (line.contains("TOTAL RSS")) {
                        return line.split("TOTAL RSS:", 2)[1].split("T", 2)[0];
                    }
                }
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "Appx: getUsage call failed, " + e.getMessage());
        } finally {
            if (process != null) {
                process.destroy();
            }
            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    LogUtils.e(TAG, "bufferedReader close call failed, " + e.getMessage());
                }
            }
        }
        return (command.contains("cpuinfo")) ? String.valueOf(cpuUsage) : "0";
    }

    private void clearAppFirewallList() {
        int numFirewallList = SystemProperties.getInt(PROP_TMS_APP_FIREWALL_NUM, 0);
        for (int i = 1; i <= numFirewallList; ++i) {
            SystemProperties.set(PROP_TMS_APP_FIREWALL_PART + i, "");
        }
        SystemProperties.set(PROP_TMS_APP_FIREWALL_NUM, "0");
    }

    private void setAppFirewallList(int num, String data) {
        SystemProperties.set(PROP_TMS_APP_FIREWALL_NUM, String.valueOf(num));
        SystemProperties.set(PROP_TMS_APP_FIREWALL_PART + num, data);
    }

    private ArrayList<String> parsePkgNamesFromJson(String Json) {
        ArrayList<String> packageNames;
        try {
            Gson gson = new Gson();
            packageNames = gson.fromJson(Json, new TypeToken<List<String>>(){}.getType());
            if (packageNames != null) {
                packageNames.removeIf(TextUtils::isEmpty);
                return packageNames;
            } else {
                LogUtils.e(TAG, "The JSON data is empty");
            }
        } catch (Exception e) {
            LogUtils.e(TAG, "JSON data parsing exception, " + e.getMessage());
        }
        return new ArrayList<>();
    }

    private boolean parseAppFirewallList(int type, String list) {
        ArrayList<String> packageNames = parsePkgNamesFromJson(list);
        if (packageNames.isEmpty()) {
            LogUtils.d(TAG, "The content of packageNames is empty.");
            // 黑名单功能不允许列表为空，为空代表不启用黑名单功能
            // 白名单功能允许列表为空
            return type == TYPE_WHITELIST;
        }

        // 由于单个系统属性Set长度不能超过91，所以需要将包名数组拆分，依次存入不同的系统属性
        int numPropPart = 0;
        if (list.length() > 91) {
            ArrayList<String> firewallList = new ArrayList<>();
            for (String packageName : packageNames) {
                if (TextUtils.isEmpty(packageName)) {
                    LogUtils.d(TAG, "packageName is empty, skip this time");
                    continue;
                }
                if (packageName.length() + firewallList.toString().length() > 85) {
                    numPropPart++;
                    setAppFirewallList(numPropPart, new Gson().toJson(firewallList));
                    firewallList.clear();
                }
                firewallList.add(packageName);
            }
            if (!firewallList.isEmpty()) {
                numPropPart++;
                setAppFirewallList(numPropPart, new Gson().toJson(firewallList));
            }
        } else {
            setAppFirewallList(1, new Gson().toJson(packageNames));
        }
        return true;
    }

    private String getMergedLockList() {
        ArrayList<String> mergedList = new ArrayList<>();

        String blackListStatus = DbManager.getDBParam(NODE_APP_BATCH_BLACKLIST_ENABLE);
        if ("1".equals(blackListStatus)) {
            String blackList = DbManager.getDBParam(NODE_APP_BATCH_BLACKLIST_LIST);
            LogUtils.d(TAG, "getMergedLockList blackList: " + blackList);
            mergedList.addAll(parsePkgNamesFromJson(blackList));
        }

        String lockListStatus = DbManager.getDBParam(NODE_APP_BATCH_LOCKLIST_ENABLE);
        if ("1".equals(lockListStatus)) {
            String lockList = DbManager.getDBParam(NODE_APP_BATCH_LOCKLIST_LIST);
            LogUtils.d(TAG, "getMergedLockList lockList: " + lockList);
            mergedList.addAll(parsePkgNamesFromJson(lockList));
        }

        if (!mergedList.isEmpty()) {
            LogUtils.d(TAG, "getMergedLockList mergedList: " + mergedList);
            return new Gson().toJson(mergedList);
        } else {
            return "";
        }
    }

    private boolean setAppLockListStatus(boolean enable) {
        if (enable) {
            String lockList = getMergedLockList();
            LogUtils.d(TAG, "setAppLockListStatus lockList: " + lockList);
            if (TextUtils.isEmpty(lockList)) {
                return true;
            }
            // 限制应用启动
            Intent service = new Intent(GlobalContext.getContext(), ActivityStartWatcher.class);
            service.putExtra("lockType", TYPE_BLACKLIST);
            service.putExtra("lockList", lockList);
            GlobalContext.getContext().startForegroundService(service);
        } else {
            // 1. 取消限制应用启动
            GlobalContext.getContext().stopService(
                    new Intent(GlobalContext.getContext(), ActivityStartWatcher.class));
            // 2. 若原BlackList功能打开，则继续打开LockList功能
            String blackListStatus = DbManager.getDBParam(NODE_APP_BATCH_BLACKLIST_ENABLE);
            if ("1".equals(blackListStatus)) {
                setAppLockListStatus(true);
            }
        }
        return true;
    }

    private boolean setAppBlackListStatus(boolean enable) {
        if (enable) {
            // 1. 限制应用启动
            String lockList = getMergedLockList();
            LogUtils.d(TAG, "setAppBlackListStatus lockList: " + lockList);
            if (TextUtils.isEmpty(lockList)) {
                return true;
            }
            Intent service = new Intent(GlobalContext.getContext(), ActivityStartWatcher.class);
            service.putExtra("lockType", TYPE_BLACKLIST);
            service.putExtra("lockList", lockList);
            GlobalContext.getContext().startForegroundService(service);

            // 2. 卸载应用
            String blackList = DbManager.getDBParam(NODE_APP_BATCH_BLACKLIST_LIST);
            LogUtils.d(TAG, "setAppBlackListStatus blackList: " + blackList);
            if (TextUtils.isEmpty(blackList)) {
                return true;
            }
            final PackageManager pm = GlobalContext.getContext().getPackageManager();
            List<PackageInfo> packlist = pm.getInstalledPackages(0);
            for (int i = 0; i < packlist.size(); i++) {
                PackageInfo pkgInfo = packlist.get(i);
                final String pkgName = pkgInfo.packageName;
                if (blackList.contains(pkgName)) {
                    final ApplicationInfo info = pkgInfo.applicationInfo;
                    if (!info.isSystemApp()) {
                        ApplicationUtils.uninstall(pkgName);
                        LogUtils.d(TAG, "Uninstallation process completed");
                    } else {
                        LogUtils.i(TAG, "The application is system app and cannot be uninstalled");
                    }
                }
            }
            // 3. 禁止应用安装
            clearAppFirewallList();
            if (parseAppFirewallList(TYPE_BLACKLIST, blackList)) {
                SystemProperties.set(PROP_TMS_APP_FIREWALL_TYPE, String.valueOf(TYPE_BLACKLIST));
            } else {
                SystemProperties.set(PROP_TMS_APP_FIREWALL_TYPE, String.valueOf(TYPE_UNDEFINED));
                return false;
            }
        } else {
            // 1. 取消限制应用启动
            GlobalContext.getContext().stopService(
                    new Intent(GlobalContext.getContext(), ActivityStartWatcher.class));
            // 2. 取消禁止应用安装
            if (SystemProperties.getInt(PROP_TMS_APP_FIREWALL_TYPE, TYPE_UNDEFINED) ==
                    TYPE_BLACKLIST) {
                SystemProperties.set(PROP_TMS_APP_FIREWALL_TYPE, String.valueOf(TYPE_UNDEFINED));
                clearAppFirewallList();
            }
            // 3. 若原LockList功能打开，则继续打开LockList功能
            String lockListStatus = DbManager.getDBParam(NODE_APP_BATCH_LOCKLIST_ENABLE);
            if ("1".equals(lockListStatus)) {
                setAppLockListStatus(true);
            }
        }
        return true;
    }

    private boolean setAppWhiteListStatus(boolean enable) {
        if (enable) {
            String whiteList = DbManager.getDBParam(NODE_APP_BATCH_WHITELIST_LIST);
            LogUtils.d(TAG, "setAppWhiteListStatus whiteList: " + whiteList);
            // 禁止应用安装
            clearAppFirewallList();
            if (TextUtils.isEmpty(whiteList) || parseAppFirewallList(TYPE_WHITELIST, whiteList)) {
                SystemProperties.set(PROP_TMS_APP_FIREWALL_TYPE, String.valueOf(TYPE_WHITELIST));
            } else {
                SystemProperties.set(PROP_TMS_APP_FIREWALL_TYPE, String.valueOf(TYPE_UNDEFINED));
                return false;
            }
        } else {
            // 取消禁止应用安装
            if (SystemProperties.getInt(PROP_TMS_APP_FIREWALL_TYPE, TYPE_UNDEFINED) ==
                    TYPE_WHITELIST) {
                SystemProperties.set(PROP_TMS_APP_FIREWALL_TYPE, String.valueOf(TYPE_UNDEFINED));
                clearAppFirewallList();
            }
        }
        return true;
    }

    @Tr369Set("Device.X_Skyworth.AppBatch.")
    public boolean SK_TR369_SetAppBatchParams(String path, String value) {
        DbManager.setDBParam(path, value);
        if (NODE_APP_BATCH_LOCKLIST_ENABLE.equals(path)) {
            // value: 0->close, 1->open
            return setAppLockListStatus("1".equals(value));
        } else if (NODE_APP_BATCH_BLACKLIST_ENABLE.equals(path)) {
            // value: 0->close, 1->open
            boolean enable = "1".equals(value);
            if (enable) {
                String whiteListStatus = DbManager.getDBParam(NODE_APP_BATCH_WHITELIST_ENABLE);
                if ("1".equals(whiteListStatus)) {
                    SK_TR369_SetAppBatchParams(NODE_APP_BATCH_WHITELIST_ENABLE, "0");
                }
            }
            return setAppBlackListStatus(enable);
        } else if (NODE_APP_BATCH_WHITELIST_ENABLE.equals(path)) {
            // value: 0->close, 1->open
            boolean enable = "1".equals(value);
            if (enable) {
                String blackListStatus = DbManager.getDBParam(NODE_APP_BATCH_BLACKLIST_ENABLE);
                if ("1".equals(blackListStatus)) {
                    SK_TR369_SetAppBatchParams(NODE_APP_BATCH_BLACKLIST_ENABLE, "0");
                }
            }
            return setAppWhiteListStatus(enable);
        }
        return true;
    }

    @Tr369Get("Device.X_Skyworth.AppBatch.")
    public String SK_TR369_GetAppBatchParams(String path) {
        return DbManager.getDBParam(path);
    }

    public static boolean isPkgAllowedToInstall(String packageName) {
        int firewallType = SystemProperties.getInt(PROP_TMS_APP_FIREWALL_TYPE, TYPE_UNDEFINED);

        int numPropPart = SystemProperties.getInt(PROP_TMS_APP_FIREWALL_NUM, 0);
        if (numPropPart <= 0) {
            return firewallType != TYPE_WHITELIST;
        }

        String packageList = "";
        for (int i = 1; i <= numPropPart; ++i) {
            String packageNames = SystemProperties.get(PROP_TMS_APP_FIREWALL_PART + i, "");
            if (!packageNames.isEmpty()) {
                packageList += packageNames;
            }
        }
        if (firewallType == TYPE_WHITELIST && !packageList.contains(packageName)) {
            return false;
        } else if (firewallType == TYPE_BLACKLIST && packageList.contains(packageName)) {
            return false;
        }

        return true;
    }

}
