
package com.aerosaga.backend;

public class StateBase {

    private final String stateName;
    private final String baseName;
    private final double latitude;
    private final double longitude;

    public StateBase(
            String stateName,
            String baseName,
            double latitude,
            double longitude) {

        this.stateName = stateName;
        this.baseName = baseName;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getStateName() {
        return stateName;
    }

    public String getBaseName() {
        return baseName;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }
}
