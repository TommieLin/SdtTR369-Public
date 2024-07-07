package com.sdt.android.tr369.Bean;

import com.google.gson.annotations.SerializedName;

public class ClientCertBean {
    @SerializedName("privateKey")
    private String privateKey;

    @SerializedName("certContent")
    private String certContent;

    @SerializedName("isGzip")
    private boolean isGzip;

    public String getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(String privateKey) {
        this.privateKey = privateKey;
    }

    public String getCertContent() {
        return certContent;
    }

    public void setCertContent(String certContent) {
        this.certContent = certContent;
    }

    public boolean isGzip() {
        return isGzip;
    }

    public void setGzip(boolean gzip) {
        isGzip = gzip;
    }

    @Override
    public String toString() {
        return "ClientCertBean{" +
                "privateKey='" + privateKey + '\'' +
                ", certContent='" + certContent + '\'' +
                ", isGzip=" + isGzip +
                '}';
    }
}
