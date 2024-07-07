/*
This file is part of the project TraceroutePing, which is an Android library
implementing Traceroute with ping under GPL license v3.
Copyright (C) 2013  Olivier Goutay

TraceroutePing is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

TraceroutePing is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with TraceroutePing.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.sdt.diagnose.traceroute;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;

import com.sdt.accessor.R;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.database.DbManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This class contain everything needed to launch a traceroute using the ping command
 *
 * @author Olivier Goutay
 */
public class TraceRoute {
    private static final String TAG = "TraceRoute";
    private static final String PING = "PING";
    private static final String FROM_PING = "From";
    private static final String SMALL_FROM_PING = "from";
    private static final String PARENTHESE_OPEN_PING = "(";
    private static final String PARENTHESE_CLOSE_PING = ")";
    private static final String TIME_PING = "time=";
    private static final String EXCEED_PING = "exceed";
    private static final String UNREACHABLE_PING = "100%";

    public List<TraceRouteContainer> traces;
    private TraceRouteContainer latestTrace;
    ExecutePingAsyncTask pingAsyncTask = null;
    private ExecutorService mExecutorService = null;
    private static final int MSG_TIME_OUT = 3310;
    private CountDownLatch lock;

    private boolean await(long timeout) {
        try {
            return lock.await(timeout, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LogUtils.e(TAG, "await error, " + e.getMessage());
        }
        return false;
    }

    public List<TraceRouteContainer> getTraces() {
        return traces;
    }

    public TraceRouteContainer getLatestTrace() {
        return latestTrace;
    }

    private int ttl;
    private int finishedTasks;
    private String hostToPing;
    private String ipToPing;
    private float elapsedTime;
    private final Context mContext;

    // timeout handling
    private static final int DEFAULT_TIMEOUT = 30000;
    private static final int DEFAULT_MAXHOPCOUNT = 10;
    private static final int DEFAULT_DATABLOCKSIZE = 64;
    private Handler handlerTimeout;
    private static Runnable runnableTimeout;

    private String getHost() {
        return DbManager.getDBParam("Device.IP.Diagnostics.TraceRoute.Host");
    }

    private int getTimeout() {
        String time = DbManager.getDBParam("Device.IP.Diagnostics.TraceRoute.Timeout");
        if (TextUtils.isEmpty(time)) return DEFAULT_TIMEOUT;
        return Integer.parseInt(time);
    }

    private int getMaxHopCount() {
        String maxHopCount = DbManager.getDBParam("Device.IP.Diagnostics.TraceRoute.MaxHopCount");
        if (TextUtils.isEmpty(maxHopCount)) return DEFAULT_MAXHOPCOUNT;
        return Integer.parseInt(maxHopCount);
    }

    private int getDataBlockSize() {
        String dataBlockSize = DbManager.getDBParam("Device.IP.Diagnostics.TraceRoute.DataBlockSize");
        if (TextUtils.isEmpty(dataBlockSize)) return DEFAULT_DATABLOCKSIZE;
        return Integer.parseInt(dataBlockSize);
    }

    protected TraceRoute(Context context) {
        this.mContext = context;
        this.traces = new ArrayList<TraceRouteContainer>();
        mExecutorService = Executors.newFixedThreadPool(1);
    }

    public void executeTraceroute() {
        this.ttl = 1;
        this.finishedTasks = 0;
        hostToPing = getHost();
        lock = new CountDownLatch(1);
        pingAsyncTask = new ExecutePingAsyncTask();
        LogUtils.d(TAG, "isTerminated: " + mExecutorService.isTerminated());
        if (mExecutorService == null || mExecutorService.isTerminated()) {
            mExecutorService = null;
            mExecutorService = Executors.newFixedThreadPool(1);
        }
        mExecutorService.execute(pingAsyncTask);
        LogUtils.d(TAG, "before lock.await.");
        long start = System.currentTimeMillis();
        this.await(15);
        long end = System.currentTimeMillis();
        LogUtils.d(TAG, "after lock.await() traces.size: " + traces.size());
        DbManager.setDBParam("Device.IP.Diagnostics.TraceRoute.ResponseTime", String.valueOf((end - start) / traces.size()));
    }

    private void handleTimeout(Runnable timeoutRunnable) {
        if (this.handlerTimeout == null) {
            this.handlerTimeout = new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    super.handleMessage(msg);
                    switch (msg.what) {
                        case MSG_TIME_OUT:
                            break;
                    }
                }
            };
        }
        // stop old timeout
        if (this.runnableTimeout != null) {
            handlerTimeout.removeCallbacks(this.runnableTimeout);
        }
        this.runnableTimeout = timeoutRunnable;
        // launch timeout after a delay
        this.handlerTimeout.postDelayed(this.runnableTimeout, getTimeout());
    }

    /**
     * Allows to timeout the ping if TIMEOUT exceeds. (-w and -W are not always supported on Android)
     */
    public class TimeOutAsyncTask {
        private final ExecutePingAsyncTask mPingAsyncTask;
        private final int ttlTask;
        private final ExecutorService timeoutExecutorService;

        public TimeOutAsyncTask(ExecutePingAsyncTask mPingAsyncTask, int ttlTask) {
            this.mPingAsyncTask = mPingAsyncTask;
            this.ttlTask = ttlTask;
            timeoutExecutorService = Executors.newFixedThreadPool(1);
        }

        public void runInBackground() {
            timeoutExecutorService.submit(new Runnable() {
                @Override
                public void run() {
                    // TODO 耗时操作
                    Runnable timeoutRunnable = new Runnable() {
                        @Override
                        public void run() {
                            if (mPingAsyncTask != null) {
                                LogUtils.e(TAG, "Task: " + ttlTask + ", task.isFinished(): " + finishedTasks + ", flag: " + (ttlTask == finishedTasks));
                                if (ttlTask == finishedTasks) {
                                    LogUtils.d(TAG, "timeoutRunnable: timeout");
                                    lock.countDown();
                                    LogUtils.d(TAG, "timeoutRunnable sendEmptyMessage lock.countDown()");
                                    mPingAsyncTask.setCancelled(true);
                                    timeoutExecutorService.shutdown();
                                }
                            }
                        }
                    };
                    handleTimeout(timeoutRunnable);
                }
            });
        }
    }

    /**
     * The task that ping an ip, with increasing time to live (ttl) value
     */
    public class ExecutePingAsyncTask implements Runnable {

        private boolean isCancelled;
        private final int maxTtl;
        private TimeOutAsyncTask mTimeOutAsyncTask = null;

        public ExecutePingAsyncTask() {
            this.maxTtl = getMaxHopCount();
        }

        @Override
        public void run() {
            // TODO Launches the ping and handle the result
            doInBackground();
        }

        /**
         * Launches the ping, launches InetAddress to retrieve url if there is one, store trace
         */
        protected void doInBackground() {
            LogUtils.d(TAG, "ExecutePingAsyncTask doInBackground");
            if (hasConnectivity()) {
                try {
                    String pingResult = launchPing(hostToPing);
                    LogUtils.d(TAG, "ExecutePingAsyncTask pingResult: " + pingResult.replace("\n", " "));
                    TraceRouteContainer trace;
                    String ip = parseIpFromPing(pingResult);
                    LogUtils.d(TAG, "ExecutePingAsyncTask pingResult From: " + ip);
                    if (pingResult.contains(UNREACHABLE_PING) && !pingResult.contains(
                            EXCEED_PING)) {
                        // Create the TracerouteContainer object when ping
                        // failed
                        trace = new TraceRouteContainer("", ip, elapsedTime, false);
                    } else {
                        // Create the TracerouteContainer object when succeed
                        if (ttl == maxTtl) {
                            String time = parseTimeFromPing(pingResult);
                            if (!TextUtils.isEmpty(time)) {
                                elapsedTime = Float.parseFloat(time);
                            }
                        }
                        trace = new TraceRouteContainer("", ip, elapsedTime, true);
                    }

                    // Get the host name from ip (unix ping do not support
                    // hostname resolving)
                    if (!trace.getIp().contains("***")) {
                        try {
                            InetAddress inetAddr = InetAddress.getByName(trace.getIp());
                            String hostname = inetAddr.getHostName();
                            String canonicalHostname = inetAddr.getCanonicalHostName();
                            trace.setHostname(hostname);
                        } catch (Exception e) {
                            trace.setHostname(trace.getIp());
                        }
                    } else {
                        trace.setHostname("***");
                    }
                    latestTrace = trace;

                    // Not refresh list if this ip is the final ip but the ttl is not maxTtl
                    // this row will be inserted later
                    if (ttl <= maxTtl) {
                        trace.setPosition(traces.size());
                        traces.add(trace);
                        DbManager.addMultiObject("Device.IP.Diagnostics.TraceRoute.RouteHops", 1);
                        LogUtils.d(TAG, "addMultiObject traces indexOf: " + traces.indexOf(trace));
                        if (ip.equals(ipToPing)) {
                            DbManager.setDBParam("Device.IP.Diagnostics.TraceRoute.Status", "Complete");
                            mExecutorService.shutdownNow();
                            lock.countDown();
                            LogUtils.e(TAG, "ExecutePingAsyncTask doInBackground lock.countDown()");
                            return;
                        }
                    }

                    LogUtils.d(TAG, "ExecutePingAsyncTask ttl: " + ttl + ", maxTtl: " + maxTtl);
                    LogUtils.d(TAG, "latestTrace.getIp(): " + latestTrace.getIp()
                            + ", ipToPing: " + ipToPing);

                    continueExecTraceroute();
                } catch (final Exception e) {
                    LogUtils.e(TAG, "ExecutePingAsyncTask doInBackground error, " + e.getMessage());
                }
            } else {
                DbManager.setDBParam("Device.IP.Diagnostics.TraceRoute.Status", "Error_Internal");
            }
        }

        /**
         * Launches ping command
         * http://aiezu.com/article/87.html ping指令参数说明
         *
         * @param url The url to ping
         * @return The ping string
         */
        @SuppressLint("NewApi")
        private String launchPing(String url) throws Exception {
            // Build ping command with parameters
            Process p;

            String format = "ping -c 1 -t %d %s";
            String command = String.format(format, ttl, url);

            LogUtils.d(TAG, "Will launch: " + command);

            long startTime = System.nanoTime();
            elapsedTime = 0;
            // timeout task
            mTimeOutAsyncTask = new TimeOutAsyncTask(this, ttl);
            mTimeOutAsyncTask.runInBackground();
            // Launch command
            p = Runtime.getRuntime().exec(command);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));

            // Construct the response from ping
            String line;
            StringBuilder ret = new StringBuilder();
            while ((line = stdInput.readLine()) != null) {
                ret.append(line).append("\n");
                if (line.contains(FROM_PING) || line.contains(SMALL_FROM_PING)) {
                    // We store the elapsedTime when the line from ping comes
                    elapsedTime = (System.nanoTime() - startTime) / 1000000.0f;
                }
            }

            p.destroy();

            if (ret.toString().equals("")) {
                throw new IllegalArgumentException();
            }

            // Store the wanted ip adress to compare with ping result
            if (ttl == 1) {
                ipToPing = parseIpToPingFromPing(ret.toString());
            }

            return ret.toString();
        }

        /**
         * Treat the previous ping (launches a ttl+1 if it is not the final ip, refresh the list on view etc...)
         */
        protected void continueExecTraceroute() {
            if (!isCancelled) {
                if (ttl < maxTtl) {
                    if (latestTrace != null && latestTrace.getIp().equals(ipToPing)) {
                        ttl = maxTtl;
                    } else {
                        ttl++;
                    }
                    mExecutorService.execute(pingAsyncTask);
                } else {
                    DbManager.setDBParam("Device.IP.Diagnostics.TraceRoute.Status", "Complete");
                    mExecutorService.shutdownNow();
                    lock.countDown();
                    LogUtils.e(TAG, "ExecutePingAsyncTask continueExecTraceroute lock.countDown()");
                }
                finishedTasks++;
            }

        }

        /**
         * Handles exception on ping
         *
         * @param e The exception thrown
         */
        private void onException(Exception e) {
            LogUtils.e(TAG, "onException: " + e.toString());
            if (e instanceof IllegalArgumentException) {
                LogUtils.e(TAG, "onException toast: " + mContext.getString(R.string.no_ping));
                DbManager.setDBParam("Device.IP.Diagnostics.TraceRoute.Status", "Error_CannotResolveHostName");
            } else {
                LogUtils.e(TAG, "onException: " + mContext.getString(R.string.error));
                DbManager.setDBParam("Device.IP.Diagnostics.TraceRoute.Status", "Error");
            }
            finishedTasks++;
        }

        public void setCancelled(boolean isCancelled) {
            this.isCancelled = isCancelled;
        }

    }

    /**
     * Gets the ip from the string returned by a ping
     *
     * @param ping The string returned by a ping command
     * @return The ip contained in the ping
     */
    private String parseIpFromPing(String ping) {
        String ip = "";
        if (ping.contains(FROM_PING)) {
            // Get ip when ttl exceeded
            int index = ping.indexOf(FROM_PING);

            ip = ping.substring(index + 5);
            if (ip.contains(PARENTHESE_OPEN_PING)) {
                // Get ip when in parenthese
                int indexOpen = ip.indexOf(PARENTHESE_OPEN_PING);
                int indexClose = ip.indexOf(PARENTHESE_CLOSE_PING);

                ip = ip.substring(indexOpen + 1, indexClose);
            } else {
                // Get ip when after from
                ip = ip.substring(0, ip.indexOf("\n"));
                if (ip.contains(":")) {
                    index = ip.indexOf(":");
                } else {
                    index = ip.indexOf(" ");
                }
                ip = ip.substring(0, index);
            }
        } else {
            // Get ip when ping succeeded
            ip = "***";
        }

        return ip;
    }

    /**
     * Gets the final ip we want to ping (example: if user fullfilled google.fr, final ip could be 8.8.8.8)
     *
     * @param ping The string returned by a ping command
     * @return The ip contained in the ping
     */
    private String parseIpToPingFromPing(String ping) {
        String ip = "";
        if (ping.contains(PING)) {
            // Get ip when ping succeeded
            int indexOpen = ping.indexOf(PARENTHESE_OPEN_PING);
            int indexClose = ping.indexOf(PARENTHESE_CLOSE_PING);
            ip = ping.substring(indexOpen + 1, indexClose);
        }
        return ip;
    }

    /**
     * Gets the time from ping command (if there is)
     *
     * @param ping The string returned by a ping command
     * @return The time contained in the ping
     */
    private String parseTimeFromPing(String ping) {
        String time = "";
        if (ping.contains(TIME_PING)) {
            int index = ping.indexOf(TIME_PING);
            time = ping.substring(index + 5);
            index = time.indexOf(" ");
            time = time.substring(0, index);
        }
        LogUtils.d(TAG, "parseTimeFromPing time: " + time);
        return time;
    }

    /**
     * Check for connectivity (wifi and mobile)
     *
     * @return true if there is a connectivity, false otherwise
     */
    public boolean hasConnectivity() {
        ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
