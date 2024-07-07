package com.skyworthdigital.speedtest.core.config;

import org.json.JSONException;
import org.json.JSONObject;

public class SpeedTestConfig {
    private int mDlCkSize = 100, mUlCkSize = 20;
    private int mDlParallelStreams = 3, mUlParallelStreams = 3;
    private int mDlStreamDelay = 300, mUlStreamDelay = 300;
    private double mDlGraceTime = 1.5, mUlGraceTime = 1.5;
    private int mDlConnectTimeout = 5000, mDlSoTimeout = 10000, mUlConnectTimeout = 5000,
            mUlSoTimeout = 10000, mPingConnectTimeout = 2000, mPingSoTimeout = 5000;
    private int mDlRecvBuffer = -1, mDlSendBuffer = -1, mUlRecvBuffer = -1, mUlSendBuffer = 16384,
            mPingRecvBuffer = -1, mPingSendBuffer = -1;
    private String errorHandlingMode = ONERROR_ATTEMPT_RESTART;
    public static final String ONERROR_FAIL = "fail", ONERROR_ATTEMPT_RESTART = "attempt-restart",
            ONERROR_MUST_RESTART = "must-restart";
    private int mTimeDlMax = 15, mTimeUlMax = 15;
    private boolean mTimeAuto = true;
    private int mCountPing = 10;
    private String mTelemetryExtra = "";
    private double overheadCompensationFactor = 1.06;
    private boolean mGetIPIsp = true;
    private String mGetIPDistance = DISTANCE_KM;
    public static final String DISTANCE_NO = "no", DISTANCE_MILES = "mi", DISTANCE_KM = "km";
    private boolean useMebibits = false;
    private String mTestOrder = "D_U";

    private void check() {
        if (mDlCkSize < 1) throw new IllegalArgumentException("dl_ckSize must be at least 1");
        if (mUlCkSize < 1) throw new IllegalArgumentException("ul_ckSize must be at least 1");
        if (mDlParallelStreams < 1) {
            throw new IllegalArgumentException(
                    "dl_parallelStreams must be at least 1");
        }
        if (mUlParallelStreams < 1) {
            throw new IllegalArgumentException(
                    "ul_parallelStreams must be at least 1");
        }
        if (mDlStreamDelay < 0) {
            throw new IllegalArgumentException(
                    "dl_streamDelay must be at least 0");
        }
        if (mUlStreamDelay < 0) {
            throw new IllegalArgumentException(
                    "ul_streamDelay must be at least 0");
        }
        if (mDlGraceTime < 0) throw new IllegalArgumentException("dl_graceTime must be at least 0");
        if (mUlGraceTime < 0) throw new IllegalArgumentException("ul_graceTime must be at least 0");
        if (!(errorHandlingMode.equals(ONERROR_FAIL) || errorHandlingMode.equals(
                ONERROR_ATTEMPT_RESTART) || errorHandlingMode.equals(ONERROR_MUST_RESTART))) {
            throw new IllegalArgumentException(
                    "errorHandlingMode must be fail, attempt-restart, or must-restart");
        }
        if (mTimeDlMax < 1) throw new IllegalArgumentException("time_dl_max must be at least 1");
        if (mTimeUlMax < 1) throw new IllegalArgumentException("time_ul_max must be at least 1");
        if (mCountPing < 1) throw new IllegalArgumentException("count_ping must be at least 1");
        if (overheadCompensationFactor < 1) {
            throw new IllegalArgumentException(
                    "overheadCompensationFactor must be at least 1");
        }
        if (!(mGetIPDistance.equals(DISTANCE_NO) || mGetIPDistance.equals(DISTANCE_KM)
                || mGetIPDistance.equals(DISTANCE_MILES))) {
            throw new IllegalArgumentException(
                    "getIP_distance must be no, km or miles");
        }
        for (char c : mTestOrder.toCharArray()) {
            if (!(c == 'I' || c == 'P' || c == 'D' || c == 'U' || c == '_')) {
                throw new IllegalArgumentException(
                        "test_order can only contain characters I, P, D, U, _");
            }
        }
    }

    public SpeedTestConfig() {
        check();
    }

    public SpeedTestConfig(int mDlCkSize, int mUlCkSize, int mDlParallelStreams,
            int mUlParallelStreams, int mDlStreamDelay, int mUlStreamDelay, double mDlGraceTime,
            double mUlGraceTime, int mDlConnectTimeout, int mDlSoTimeout, int mUlConnectTimeout,
            int mUlSoTimeout, int mPingConnectTimeout, int mPingSoTimeout, int mDlRecvBuffer,
            int mDlSendBuffer, int mUlRecvBuffer, int mUlSendBuffer, int mPingRecvBuffer,
            int mPingSendBuffer, String errorHandlingMode, int mTimeDlMax, int mTimeUlMax,
            boolean mTimeAuto, int mCountPing, String mTelemetryExtra,
            double overheadCompensationFactor, boolean mGetIPIsp, String mGetIPDistance,
            boolean useMebibits, String mTestOrder) {
        this.mDlCkSize = mDlCkSize;
        this.mUlCkSize = mUlCkSize;
        this.mDlParallelStreams = mDlParallelStreams;
        this.mUlParallelStreams = mUlParallelStreams;
        this.mDlStreamDelay = mDlStreamDelay;
        this.mUlStreamDelay = mUlStreamDelay;
        this.mDlGraceTime = mDlGraceTime;
        this.mUlGraceTime = mUlGraceTime;
        this.mDlConnectTimeout = mDlConnectTimeout;
        this.mDlSoTimeout = mDlSoTimeout;
        this.mUlConnectTimeout = mUlConnectTimeout;
        this.mUlSoTimeout = mUlSoTimeout;
        this.mPingConnectTimeout = mPingConnectTimeout;
        this.mPingSoTimeout = mPingSoTimeout;
        this.mDlRecvBuffer = mDlRecvBuffer;
        this.mDlSendBuffer = mDlSendBuffer;
        this.mUlRecvBuffer = mUlRecvBuffer;
        this.mUlSendBuffer = mUlSendBuffer;
        this.mPingRecvBuffer = mPingRecvBuffer;
        this.mPingSendBuffer = mPingSendBuffer;
        this.errorHandlingMode = errorHandlingMode;
        this.mTimeDlMax = mTimeDlMax;
        this.mTimeUlMax = mTimeUlMax;
        this.mTimeAuto = mTimeAuto;
        this.mCountPing = mCountPing;
        this.mTelemetryExtra = mTelemetryExtra;
        this.overheadCompensationFactor = overheadCompensationFactor;
        this.mGetIPIsp = mGetIPIsp;
        this.mGetIPDistance = mGetIPDistance;
        this.useMebibits = useMebibits;
        this.mTestOrder = mTestOrder;
        check();
    }

    public SpeedTestConfig(JSONObject json) {
        try {
            if (json.has("dl_ckSize")) this.mDlCkSize = json.getInt("dl_ckSize");
            if (json.has("ul_ckSize")) this.mUlCkSize = json.getInt("ul_ckSize");
            if (json.has("dl_parallelStreams")) {
                this.mDlParallelStreams = json.getInt("dl_parallelStreams");
            }
            if (json.has("ul_parallelStreams")) {
                this.mUlParallelStreams = json.getInt("ul_parallelStreams");
            }
            if (json.has("dl_streamDelay")) this.mDlStreamDelay = json.getInt("dl_streamDelay");
            if (json.has("ul_streamDelay")) this.mUlStreamDelay = json.getInt("ul_streamDelay");
            if (json.has("dl_graceTime")) this.mDlGraceTime = json.getDouble("dl_graceTime");
            if (json.has("ul_graceTime")) this.mUlGraceTime = json.getDouble("ul_graceTime");
            if (json.has("dl_connectTimeout")) {
                this.mDlConnectTimeout = json.getInt("dl_connectTimeout");
            }
            if (json.has("ul_connectTimeout")) {
                this.mUlConnectTimeout = json.getInt("ul_connectTimeout");
            }
            if (json.has("ping_connectTimeout")) {
                this.mPingConnectTimeout = json.getInt("ping_connectTimeout");
            }
            if (json.has("dl_soTimeout")) this.mDlSoTimeout = json.getInt("dl_soTimeout");
            if (json.has("ul_soTimeout")) this.mUlSoTimeout = json.getInt("ul_soTimeout");
            if (json.has("ping_soTimeout")) this.mPingSoTimeout = json.getInt("ping_soTimeout");
            if (json.has("dl_recvBuffer")) this.mDlRecvBuffer = json.getInt("dl_recvBuffer");
            if (json.has("ul_recvBuffer")) this.mUlRecvBuffer = json.getInt("ul_recvBuffer");
            if (json.has("ping_recvBuffer")) this.mPingRecvBuffer = json.getInt("ping_recvBuffer");
            if (json.has("dl_sendBuffer")) this.mDlSendBuffer = json.getInt("dl_sendBuffer");
            if (json.has("ul_sendBuffer")) this.mUlSendBuffer = json.getInt("ul_sendBuffer");
            if (json.has("ping_sendBuffer")) this.mPingSendBuffer = json.getInt("ping_sendBuffer");
            if (json.has("errorHandlingMode")) {
                this.errorHandlingMode = json.getString("errorHandlingMode");
            }
            if (json.has("time_dl_max")) this.mTimeDlMax = json.getInt("time_dl_max");
            if (json.has("time_ul_max")) this.mTimeUlMax = json.getInt("time_ul_max");
            if (json.has("count_ping")) this.mCountPing = json.getInt("count_ping");
            if (json.has("telemetry_extra")) {
                this.mTelemetryExtra = json.getString("telemetry_extra");
            }
            if (json.has("overheadCompensationFactor")) {
                this.overheadCompensationFactor = json.getDouble("overheadCompensationFactor");
            }
            if (json.has("getIP_isp")) this.mGetIPIsp = json.getBoolean("getIP_isp");
            if (json.has("getIP_distance")) this.mGetIPDistance = json.getString("getIP_distance");
            if (json.has("test_order")) this.mTestOrder = json.getString("test_order");
            if (json.has("useMebibits")) this.useMebibits = json.getBoolean("useMebibits");
            check();
        } catch (JSONException t) {
            throw new IllegalArgumentException("Invalid JSON (" + t.toString() + ")");
        }
    }

    public int getDlCkSize() {
        return mDlCkSize;
    }

    public int getUlCkSize() {
        return mUlCkSize;
    }

    public int getDlParallelStreams() {
        return mDlParallelStreams;
    }

    public int getUlParallelStreams() {
        return mUlParallelStreams;
    }

    public int getDlStreamDelay() {
        return mDlStreamDelay;
    }

    public int getUlStreamDelay() {
        return mUlStreamDelay;
    }

    public double getDlGraceTime() {
        return mDlGraceTime;
    }

    public double getUlGraceTime() {
        return mUlGraceTime;
    }

    public int getDlConnectTimeout() {
        return mDlConnectTimeout;
    }

    public int getDlSoTimeout() {
        return mDlSoTimeout;
    }

    public int getUlConnectTimeout() {
        return mUlConnectTimeout;
    }

    public int getUlSoTimeout() {
        return mUlSoTimeout;
    }

    public int getPingConnectTimeout() {
        return mPingConnectTimeout;
    }

    public int getPingSoTimeout() {
        return mPingSoTimeout;
    }

    public int getDlRecvBuffer() {
        return mDlRecvBuffer;
    }

    public int getDlSendBuffer() {
        return mDlSendBuffer;
    }

    public int getUlRecvBuffer() {
        return mUlRecvBuffer;
    }

    public int getUlSendBuffer() {
        return mUlSendBuffer;
    }

    public int getPingRecvBuffer() {
        return mPingRecvBuffer;
    }

    public int getPingSendBuffer() {
        return mPingSendBuffer;
    }

    public String getErrorHandlingMode() {
        return errorHandlingMode;
    }

    public int getTimeDlMax() {
        return mTimeDlMax;
    }

    public int getTimeUlMax() {
        return mTimeUlMax;
    }

    public boolean getTimeAuto() {
        return mTimeAuto;
    }

    public int getCountPing() {
        return mCountPing;
    }

    public String getTelemetryExtra() {
        return mTelemetryExtra;
    }

    public double getOverheadCompensationFactor() {
        return overheadCompensationFactor;
    }

    public boolean getGetIPIsp() {
        return mGetIPIsp;
    }

    public String getGetIPDistance() {
        return mGetIPDistance;
    }

    public boolean getUseMebibits() {
        return useMebibits;
    }

    public String getTestOrder() {
        return mTestOrder;
    }

    public void setDlCkSize(int dlCkSize) {
        if (dlCkSize < 1) throw new IllegalArgumentException("dl_ckSize must be at least 1");
        this.mDlCkSize = dlCkSize;
    }

    public void setUlCkSize(int ulCkSize) {
        if (ulCkSize < 1) throw new IllegalArgumentException("ul_ckSize must be at least 1");
        this.mUlCkSize = ulCkSize;
    }

    public void setDlParallelStreams(int dlParallelStreams) {
        if (dlParallelStreams < 1) {
            throw new IllegalArgumentException(
                    "dl_parallelStreams must be at least 1");
        }
        this.mDlParallelStreams = dlParallelStreams;
    }

    public void setUlParallelStreams(int ulParallelStreams) {
        if (ulParallelStreams < 1) {
            throw new IllegalArgumentException(
                    "ul_parallelStreams must be at least 1");
        }
        this.mUlParallelStreams = ulParallelStreams;
    }

    public void setDlStreamDelay(int dlStreamDelay) {
        if (dlStreamDelay < 0) {
            throw new IllegalArgumentException(
                    "dl_streamDelay must be at least 0");
        }
        this.mDlStreamDelay = dlStreamDelay;
    }

    public void setUlStreamDelay(int ulStreamDelay) {
        if (ulStreamDelay < 0) {
            throw new IllegalArgumentException(
                    "ul_streamDelay must be at least 0");
        }
        this.mUlStreamDelay = ulStreamDelay;
    }

    public void setDlGraceTime(double dlGraceTime) {
        if (dlGraceTime < 0) throw new IllegalArgumentException("dl_graceTime must be at least 0");
        this.mDlGraceTime = dlGraceTime;
    }

    public void setUlGraceTime(double ulGraceTime) {
        if (ulGraceTime < 0) throw new IllegalArgumentException("ul_graceTime must be at least 0");
        this.mUlGraceTime = ulGraceTime;
    }

    public void setDlConnectTimeout(int dlConnectTimeout) {

        this.mDlConnectTimeout = dlConnectTimeout;
    }

    public void setDlSoTimeout(int dlSoTimeout) {

        this.mDlSoTimeout = dlSoTimeout;
    }

    public void setUlConnectTimeout(int ulConnectTimeout) {

        this.mUlConnectTimeout = ulConnectTimeout;
    }

    public void setUlSoTimeout(int ulSoTimeout) {

        this.mUlSoTimeout = ulSoTimeout;
    }

    public void setPingConnectTimeout(int pingConnectTimeout) {

        this.mPingConnectTimeout = pingConnectTimeout;
    }

    public void setPingSoTimeout(int pingSoTimeout) {

        this.mPingSoTimeout = pingSoTimeout;
    }

    public void setDlRecvBuffer(int dlRecvBuffer) {

        this.mDlRecvBuffer = dlRecvBuffer;
    }

    public void setDlSendBuffer(int dlSendBuffer) {

        this.mDlSendBuffer = dlSendBuffer;
    }

    public void setUlRecvBuffer(int ulRecvBuffer) {

        this.mUlRecvBuffer = ulRecvBuffer;
    }

    public void setUlSendBuffer(int ulSendBuffer) {

        this.mUlSendBuffer = ulSendBuffer;
    }

    public void setPingRecvBuffer(int pingRecvBuffer) {

        this.mPingRecvBuffer = pingRecvBuffer;
    }

    public void setPingSendBuffer(int pingSendBuffer) {

        this.mPingSendBuffer = pingSendBuffer;
    }

    public void setErrorHandlingMode(String errorHandlingMode) {
        if (!(errorHandlingMode.equals(ONERROR_FAIL) || errorHandlingMode.equals(
                ONERROR_ATTEMPT_RESTART) || errorHandlingMode.equals(ONERROR_MUST_RESTART))) {
            throw new IllegalArgumentException(
                    "errorHandlingMode must be fail, attempt-restart, or must-restart");
        }
        this.errorHandlingMode = errorHandlingMode;
    }

    public void setTimeDlMax(int timeDlMax) {
        if (timeDlMax < 1) throw new IllegalArgumentException("time_dl_max must be at least 1");
        this.mTimeDlMax = timeDlMax;
    }

    public void setTimeUlMax(int timeUlMax) {
        if (timeUlMax < 1) throw new IllegalArgumentException("time_ul_max must be at least 1");
        this.mTimeUlMax = timeUlMax;
    }

    public void setTimeAuto(boolean timeAuto) {

        this.mTimeAuto = timeAuto;
    }

    public void setCountPing(int countPing) {
        if (countPing < 1) throw new IllegalArgumentException("count_ping must be at least 1");
        this.mCountPing = countPing;
    }

    public void setTelemetryExtra(String telemetryExtra) {

        this.mTelemetryExtra = telemetryExtra;
    }

    public void setOverheadCompensationFactor(double overheadCompensationFactor) {
        if (overheadCompensationFactor < 1) {
            throw new IllegalArgumentException(
                    "overheadCompensationFactor must be at least 1");
        }
        this.overheadCompensationFactor = overheadCompensationFactor;
    }

    public void setGetIPIsp(boolean getIPIsp) {

        this.mGetIPIsp = getIPIsp;
    }

    public void setGetIPDistance(String getIPDistance) {
        if (!(getIPDistance.equals(DISTANCE_NO) || getIPDistance.equals(DISTANCE_KM)
                || getIPDistance.equals(DISTANCE_MILES))) {
            throw new IllegalArgumentException(
                    "getIP_distance must be no, km or miles");
        }
        this.mGetIPDistance = getIPDistance;
    }

    public void setUseMebibits(boolean useMebibits) {

        this.useMebibits = useMebibits;
    }

    public void setTestOrder(String testOrder) {
        for (char c : testOrder.toCharArray()) {
            if (!(c == 'I' || c == 'P' || c == 'D' || c == 'U' || c == '_')) {
                throw new IllegalArgumentException(
                        "test_order can only contain characters I, P, D, U, _");
            }
        }
        this.mTestOrder = testOrder;
    }

    public SpeedTestConfig clone() {
        return new SpeedTestConfig(mDlCkSize, mUlCkSize, mDlParallelStreams, mUlParallelStreams,
                mDlStreamDelay, mUlStreamDelay, mDlGraceTime, mUlGraceTime, mDlConnectTimeout,
                mDlSoTimeout, mUlConnectTimeout, mUlSoTimeout, mPingConnectTimeout, mPingSoTimeout,
                mDlRecvBuffer, mDlSendBuffer, mUlRecvBuffer, mUlSendBuffer, mPingRecvBuffer,
                mPingSendBuffer, errorHandlingMode, mTimeDlMax, mTimeUlMax, mTimeAuto, mCountPing,
                mTelemetryExtra, overheadCompensationFactor, mGetIPIsp, mGetIPDistance, useMebibits,
                mTestOrder);
    }
}
