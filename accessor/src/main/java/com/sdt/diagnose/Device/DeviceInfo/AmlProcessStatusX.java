package com.sdt.diagnose.Device.DeviceInfo;

import com.droidlogic.app.SystemControlManager;
import com.sdt.diagnose.common.log.LogUtils;

public class AmlProcessStatusX {
    private static final String TAG = "AmlProcessStatusX";
    private static AmlProcessStatusX mAmlProcessStatusX;

    AmlProcessStatusX() {
    }

    public static AmlProcessStatusX getInstance() {
        if (null == mAmlProcessStatusX) {
            mAmlProcessStatusX = new AmlProcessStatusX();
        }
        return mAmlProcessStatusX;
    }

    /**
     * 需要权限 allow system_control proc_stat:file { open read }
     * <p>
     * (user、nice、system、idle、iowait、irq、softirq、stealstolen、guest)的9元组
     * <p>
     * cpu  143024 41996 347947 4830201 15644 0 1658 0 0 0
     * cpu0 38179 10260 87076 1167751 3533 0 1572 0 0 0
     * cpu1 36448 10557 88600 1227830 4656 0 25 0 0 0
     * cpu2 34714 10712 82776 1228479 3793 0 31 0 0 0
     * cpu3 33682 10466 89494 1206139 3661 0 28 0 0 0
     * intr 33170088 0 0 0 21370496 0 0 139 76010 61 36007 0 3 0 0 155771 0 0 0 0 0 1 0 9912 0 0
     * 0 1197 1 0 0 0 1339654 0 0 0 94 202643 12 60 0 0 0 857307 0 0 0 0 0 0 857306 0 236129
     * 845542 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0
     * ctxt 51806301
     * btime 1631934873
     * processes 28912
     * procs_running 1
     * procs_blocked 0
     * softirq 12856022 0 5054837 2712 159134 0 0 41560 3901377 0 3696402
     *
     * @return
     */
    public double getCpuUsageByAml() {
        String[] usages = new String[2];
        for (int i = 0; i < usages.length; i++) {
            try {
                String ret = SystemControlManager.getInstance().readSysFs("/proc/stat");
                if (ret.isEmpty()) return 0;

                String[] cpuSplit = ret.trim().split("cpu");
                if (cpuSplit.length > 1) {
                    usages[i] = cpuSplit[1].trim();
                } else {
                    return 0;
                }

                Thread.sleep(100);
            } catch (InterruptedException e) {
                LogUtils.e(TAG, "getCpuUsageByAml: thread sleep error, " + e.getMessage());
            } catch (NoClassDefFoundError e) {
                LogUtils.e(TAG, "getCpuUsageByAml: SystemControlManager call failed, " + e.getMessage());
                return 0;
            }
        }

        String[] cpuInfo1 = usages[0].trim().split("\\s+");
        String[] cpuInfo2 = usages[1].trim().split("\\s+");
        int count = Math.min(cpuInfo1.length, 9);
        int sum = 0;
        int[] datas = new int[count];
        for (int i = 0; i < count; i++) {
            int data = Integer.parseInt(cpuInfo2[i]) - Integer.parseInt(cpuInfo1[i]);
            sum += data;
            if (i + 1 < count) {
                datas[i + 1] = data;
            }
        }
        datas[0] = sum;
        String summary = String.format(
                "%d%%cpu %d%%user %d%%nice %d%%sys %d%%idle %d%%iow %d%%irq %d%%sirq %d%%host",
                datas[0], datas[1], datas[2], datas[3], datas[4], datas[5], datas[6], datas[7], datas[8]);
        LogUtils.d(TAG, "getCpuUsageByAml: Get usage: " + summary);

        double cpuUsage = 100.0 * (datas[0] - datas[4]) / datas[0];
        if (cpuUsage < 1.0) {
            cpuUsage = 1.0;
        } else if (cpuUsage >= 98.0) {
            cpuUsage = 98.0;
        }
        String result = String.format("Total=%d, Idle=%d, Used=%d, Percent=%f%%", datas[0], datas[4],
                datas[0] - datas[4], cpuUsage);
        LogUtils.d(TAG, "getCpuUsageByAml: result: " + result);
        return cpuUsage;
    }

}
