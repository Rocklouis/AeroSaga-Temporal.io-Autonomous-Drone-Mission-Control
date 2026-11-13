
package com.aerosaga.backend;

public class DroneAssignment {

    private final String droneId;
    private final StateBase assignedBase;

    public DroneAssignment(String droneId, StateBase assignedBase) {
        this.droneId = droneId;
        this.assignedBase = assignedBase;
    }

    public String getDroneId() {
        return droneId;
    }

    public StateBase getAssignedBase() {
        return assignedBase;
    }

    public String getStateName() {
        return assignedBase.getStateName();
    }

    public String getBaseName() {
        return assignedBase.getBaseName();
    }

    public double getBaseLatitude() {
        return assignedBase.getLatitude();
    }

    public double getBaseLongitude() {
        return assignedBase.getLongitude();
    }
}
