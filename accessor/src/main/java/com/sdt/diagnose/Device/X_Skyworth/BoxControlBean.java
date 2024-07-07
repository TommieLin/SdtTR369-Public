package com.sdt.diagnose.Device.X_Skyworth;

public class BoxControlBean {
    private boolean isAllow = false;
    private String confirmResultUrl = null;
    private String transactionId = null;

    public static BoxControlBean instance;

    public static BoxControlBean getInstance() {
        synchronized (BoxControlBean.class) {
            if (instance == null) {
                instance = new BoxControlBean();
            }
        }
        return instance;
    }

    public boolean isAllow() {
        return isAllow;
    }

    public void setAllow(boolean allow) {
        isAllow = allow;
    }

    public String getConfirmResultUrl() {
        return confirmResultUrl;
    }

    public void setConfirmResultUrl(String confirmResultUrl) {
        this.confirmResultUrl = confirmResultUrl;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    @Override
    public String toString() {
        return "BoxControlBean{" +
                "isAllow=" + isAllow +
                ", confirmResultUrl='" + confirmResultUrl + '\'' +
                ", transactionId='" + transactionId + '\'' +
                '}';
    }
}
