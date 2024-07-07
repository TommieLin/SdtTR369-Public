package com.sdt.diagnose.common.bean;

/**
 * @Author Outis
 * @Date 2023/11/30 10:22
 * @Version 1.0
 */
public class SpeedTestBean {
    public String enable;
    public String url;
    public String transactionId;
    public static SpeedTestBean instance;

    public static SpeedTestBean getInstance() {
        synchronized (SpeedTestBean.class) {
            if (instance == null) {
                instance = new SpeedTestBean();
            }
        }
        return instance;
    }

    public String getEnable() {
        return enable;
    }

    public void setEnable(String enable) {
        this.enable = enable;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }
}
