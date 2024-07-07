package com.sdt.diagnose.Device.IP.Diagnostics.TraceRoute;

import com.sdt.annotations.Tr369Get;
import com.sdt.diagnose.common.IProtocolArray;
import com.sdt.diagnose.common.ProtocolPathUtils;
import com.sdt.diagnose.common.log.LogUtils;
import com.sdt.diagnose.traceroute.TraceRouteContainer;
import com.sdt.diagnose.traceroute.TraceRouteManager;

import java.util.List;


public class RouteHopsX implements IProtocolArray<TraceRouteContainer> {
    private static final String TAG = "RouteHopsX";
    private static final String REFIX = "Device.IP.Diagnostics.TraceRoute.RouteHops.";

    @Tr369Get("Device.IP.Diagnostics.TraceRoute.RouteHops.")
    public String SK_TR369_GetRouteHops(String path) {
        return handleAppPath(path);
    }

    private String handleAppPath(String path) {
        LogUtils.d(TAG, "handleAppPath: " + path);
        return ProtocolPathUtils.getInfoFromArray(REFIX, path, this);
    }

    @Override
    public List<TraceRouteContainer> getArray() {
        LogUtils.d(TAG, "getArray");
        return TraceRouteManager.getInstance().getTraces();
    }

    @Override
    public String getValue(TraceRouteContainer container, String[] paramsArr) {
        LogUtils.d(TAG, "getValue: " + container.toString());
        if (paramsArr.length > 1) {
            String param = paramsArr[1];
            switch (param) {
                case "Host":
                    return container.getHostname();
                case "HostAddress":
                    return container.getIp();
                case "RTTimes":
                    return String.valueOf(container.getMs());
                default:
                    break;
            }
        }
        return "";
    }
}
