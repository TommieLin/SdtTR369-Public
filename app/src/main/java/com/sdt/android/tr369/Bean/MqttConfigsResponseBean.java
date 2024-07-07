package com.sdt.android.tr369.Bean;

import com.google.gson.annotations.SerializedName;

public class MqttConfigsResponseBean {
    @SerializedName("message")
    private String message;

    @SerializedName("mqttServer")
    private String mqttServer;

    @SerializedName("clientId")
    private String clientId;

    @SerializedName("username")
    private String username;

    @SerializedName("password")
    private String password;

    @SerializedName("caCert")
    private CaCertBean caCert;

    @SerializedName("clientCert")
    private ClientCertBean clientCert;

    @SerializedName("enable")
    private boolean enable;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMqttServer() {
        return mqttServer;
    }

    public void setMqttServer(String mqttServer) {
        this.mqttServer = mqttServer;
    }

    public CaCertBean getCaCert() {
        return caCert;
    }

    public void setCaCert(CaCertBean caCert) {
        this.caCert = caCert;
    }

    public ClientCertBean getClientCert() {
        return clientCert;
    }

    public void setClientCert(ClientCertBean clientCert) {
        this.clientCert = clientCert;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "MqttConfigsResponseBean{" +
                "message='" + message + '\'' +
                ", mqttServer='" + mqttServer + '\'' +
                ", clientId='" + clientId + '\'' +
                ", username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", caCert=" + caCert +
                ", clientCert=" + clientCert +
                ", enable=" + enable +
                '}';
    }
}
