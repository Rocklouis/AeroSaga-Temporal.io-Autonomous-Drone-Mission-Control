package com.aerosaga.workflow;

public class DroneActivitiesImpl implements DroneActivities {

    @Override
    public String takeoff(String droneId) {
        validateDroneId(droneId);
        System.out.println("Drone " + droneId + " is taking off!");
        return "Takeoff completed.";
    }

    @Override
    public String navigate(String droneId) {
        System.out.println("Drone " + droneId + " is navigating to destination.");
        return "Navigation completed.";
    }

   @Override
    public String dropPackage(String droneId) {
        System.out.println("Drone " + droneId + " package dropped successfully!");
        return "Package dropped successfully";
}

    @Override
    public String returnToBase(String droneId) {
        System.out.println("Drone " + droneId + " is returning to base.");
        return "Drone returned to base.";
    }

    private void validateDroneId(String droneId) {
        if (droneId == null || droneId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Drone ID cannot be empty or null."
            );
        }
    }
}