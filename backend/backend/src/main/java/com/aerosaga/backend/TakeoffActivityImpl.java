
package com.aerosaga.backend;

public class TakeoffActivityImpl implements TakeoffActivity {

    @Override
    public String performTakeoff(String droneId) {
        System.out.println("Drone " + droneId + " is preparing for takeoff...");

        if ("DR-FAIL".equals(droneId)) {
            System.out.println("Simulated takeoff failure for " + droneId);
            throw new RuntimeException("Simulated takeoff failure for " + droneId);
        }

        System.out.println("Drone " + droneId + " has taken off successfully!");

        return "SUCCESS: Drone " + droneId + " has taken off.";
    }
}
