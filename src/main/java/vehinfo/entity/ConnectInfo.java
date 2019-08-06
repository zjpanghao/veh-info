package vehinfo.entity;

import java.util.Date;

public class ConnectInfo {
    private String ip;
    private int port;
    private int servPort;
    private Date connTime;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getServPort() {
        return servPort;
    }

    public void setServPort(int servPort) {
        this.servPort = servPort;
    }

    public Date getConnTime() {
        return connTime;
    }

    public void setConnTime(Date connTime) {
        this.connTime = connTime;
    }
}
