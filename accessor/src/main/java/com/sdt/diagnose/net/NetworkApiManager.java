package com.sdt.diagnose.net;

public class NetworkApiManager {
    private NetworkApi networkApi = null;
    private static NetworkApiManager instance = null;

    private NetworkApiManager() {
        networkApi = new NetworkApi();
    }

    public static NetworkApiManager getInstance() {
        synchronized (NetworkApiManager.class) {
            if (instance == null) {
                instance = new NetworkApiManager();
            }
        }
        return instance;
    }

    public NetworkApi getNetworkApi() {
        return networkApi;
    }
}
