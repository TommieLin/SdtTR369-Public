package com.sdt.diagnose.traceroute;

import android.content.Context;

import com.sdt.diagnose.common.GlobalContext;

import java.util.List;


public class TraceRouteManager {

    private static final String TAG = "TraceRouteManager";
    private static TraceRouteManager instance = null;

    public TraceRoute getTraceRoute() {
        return mTraceRoute;
    }

    private TraceRoute mTraceRoute = null;
    private final Context mContext = GlobalContext.getContext();

    protected TraceRouteManager() {
        mTraceRoute = new TraceRoute(mContext);
    }

    public static TraceRouteManager getInstance() {
        synchronized (TraceRouteManager.class) {
            if (instance == null) {
                instance = new TraceRouteManager();
            }
        }
        return instance;
    }

    public TraceRouteContainer getLatestTrace() {
        return mTraceRoute.getLatestTrace();
    }

    public List<TraceRouteContainer> getTraces() {
        return mTraceRoute.getTraces();
    }

    public void addTrace(TraceRouteContainer trace) {
        getTraces().add(trace);
    }
}
