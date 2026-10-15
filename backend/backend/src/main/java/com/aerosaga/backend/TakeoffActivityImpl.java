
package com.aerosaga.backend;

public class TakeoffActivityImpl implements TakeoffActivity {

    @Override
    public String performTakeoff(String droneId) {
        System.out.println("Drone " + droneId + " is preparing for takeoff...");

        System.out.println("Drone " + droneId + " has taken off successfully!");

        return "SUCCESS: Drone " + droneId + " has taken off.";
    }
}

