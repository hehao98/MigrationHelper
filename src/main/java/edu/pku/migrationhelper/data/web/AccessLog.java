package edu.pku.migrationhelper.data.web;

import java.util.Date;

public class AccessLog {
    private final String url;
    private final String ip;
    private final Date date;

    public AccessLog(String url, String ip, Date date) {
        this.url = url;
        this.ip = ip;
        this.date = date;
    }

    public String getUrl() {
        return url;
    }

    public String getIp() {
        return ip;
    }

    public Date getDate() {
        return date;
    }
}
