package edu.pku.migrationhelper.data.web;

public class AccessLog {
    private final String url;
    private final String ip;

    public AccessLog(String url, String ip) {
        this.url = url;
        this.ip = ip;
    }

    public String getUrl() {
        return url;
    }

    public String getIp() {
        return ip;
    }
}
