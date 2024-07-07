package com.sdt.diagnose.Device.X_Skyworth.Tcpdump;

/**
 * @Author Outis
 * @Date 2023/11/30 10:23
 * @Version 1.0
 */
public class TcpdumpBean {
    String enable;
    String ip;
    String port;
    String netType;
    String duration;
    String url;
    long fileSize;

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getEnable() {
        return enable;
    }

    public void setEnable(String enable) {
        this.enable = enable;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getNetType() {
        return netType;
    }

    public void setNetType(String netType) {
        this.netType = netType;
    }

    @Override
    public String toString() {
        return "TcpdumpBean{" +
                "enable='" + enable + '\'' +
                ", ip='" + ip + '\'' +
                ", port='" + port + '\'' +
                ", netType='" + netType + '\'' +
                ", duration='" + duration + '\'' +
                ", url='" + url + '\'' +
                ", fileSize=" + fileSize +
                '}';
    }
}
