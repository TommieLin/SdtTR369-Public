package com.sdt.diagnose.common.bean;

public class NotificationBean {
    private String url;
    private String TransactionId;

    private final String notificationUrlKey = "sdt.notification.url";

    public static NotificationBean instance;

    public static NotificationBean getInstance() {
        synchronized (NotificationBean.class) {
            if (instance == null) {
                instance = new NotificationBean();
            }
        }
        return instance;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTransactionId() {
        return TransactionId;
    }

    public void setTransactionId(String transactionId) {
        TransactionId = transactionId;
    }
}
