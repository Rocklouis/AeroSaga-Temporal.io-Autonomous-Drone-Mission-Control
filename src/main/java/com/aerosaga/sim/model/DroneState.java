package com.aerosaga.sim.model;

/**
 * Shared wire format for:
 *  - GET /api/sim/drones  (Frontend Week 1 static markers)
 *  - GET /api/sim/drones/{id}  (Temporal queries / activities)
 *  - POST gateway /api/telemetry/batch  (Backend → WebSocket → Cesium)
 */
public class DroneState {

    private String droneId;
    private double lat;
    private double lng;
    private double alt;
    private int battery;
    private String phase;
    private String status;
    private boolean online;
    private double headingDeg;
    private double payloadKg;
    private String droneType;
    private long timestamp;

    public DroneState() {}

    public String getDroneId() { return droneId; }
    public void setDroneId(String droneId) { this.droneId = droneId; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public double getAlt() { return alt; }
    public void setAlt(double alt) { this.alt = alt; }

    public int getBattery() { return battery; }
    public void setBattery(int battery) { this.battery = battery; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }

    public double getHeadingDeg() { return headingDeg; }
    public void setHeadingDeg(double headingDeg) { this.headingDeg = headingDeg; }

    public double getPayloadKg() { return payloadKg; }
    public void setPayloadKg(double payloadKg) { this.payloadKg = payloadKg; }

    public String getDroneType() { return droneType; }
    public void setDroneType(String droneType) { this.droneType = droneType; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}