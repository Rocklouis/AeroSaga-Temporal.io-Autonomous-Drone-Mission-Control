package com.aerosaga.sim.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "aerosaga.sim")
public class SimProperties {

    private int fleetSize = 50;
    private long tickIntervalMs = 1000;
    private String gatewayUrl = "http://localhost:8080/api/telemetry/batch";
    private boolean publishTelemetry = false;
    private double baseLat = 19.0760;
    private double baseLng = 72.8777;
    private double spread = 0.15;
    private int lowBatteryThreshold = 10;
    private int reconnectRtbThreshold = 15;

    public int getFleetSize() { return fleetSize; }
    public void setFleetSize(int fleetSize) { this.fleetSize = fleetSize; }

    public long getTickIntervalMs() { return tickIntervalMs; }
    public void setTickIntervalMs(long tickIntervalMs) { this.tickIntervalMs = tickIntervalMs; }

    public String getGatewayUrl() { return gatewayUrl; }
    public void setGatewayUrl(String gatewayUrl) { this.gatewayUrl = gatewayUrl; }

    public boolean isPublishTelemetry() { return publishTelemetry; }
    public void setPublishTelemetry(boolean publishTelemetry) { this.publishTelemetry = publishTelemetry; }

    public double getBaseLat() { return baseLat; }
    public void setBaseLat(double baseLat) { this.baseLat = baseLat; }

    public double getBaseLng() { return baseLng; }
    public void setBaseLng(double baseLng) { this.baseLng = baseLng; }

    public double getSpread() { return spread; }
    public void setSpread(double spread) { this.spread = spread; }

    public int getLowBatteryThreshold() { return lowBatteryThreshold; }
    public void setLowBatteryThreshold(int lowBatteryThreshold) {
        this.lowBatteryThreshold = lowBatteryThreshold;
    }

    public int getReconnectRtbThreshold() { return reconnectRtbThreshold; }
    public void setReconnectRtbThreshold(int reconnectRtbThreshold) {
        this.reconnectRtbThreshold = reconnectRtbThreshold;
    }
}