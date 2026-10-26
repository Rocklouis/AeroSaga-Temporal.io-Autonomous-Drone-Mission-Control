
package com.aerosaga.backend;

public class TakeoffActivityImpl implements TakeoffActivity {

    @Override
    public String performTakeoff(String droneId) {
        System.out.println(
                "Drone " + droneId + " preparing for takeoff..."
        );

        if ("DR-FAIL".equals(droneId)) {
            throw new RuntimeException(
                    "Simulated takeoff failure for " + droneId
            );
        }

        System.out.println(
                "Drone " + droneId + " took off successfully."
        );

        return "SUCCESS: Drone " + droneId + " has taken off.";
    }

    @Override
    public String navigateToDropPoint(String droneId) {
        System.out.println(
                "Drone " + droneId + " navigating to drop point..."
        );

        return "NAVIGATED: Drone " + droneId
                + " reached the drop point.";
    }

    @Override
    public String dropPackage(String droneId) {
        System.out.println(
                "Drone " + droneId + " attempting package drop..."
        );

        if ("DR-DROP-FAIL".equals(droneId)) {
            throw new RuntimeException(
                    "Simulated package-drop failure for " + droneId
            );
        }

        System.out.println(
                "Drone " + droneId + " dropped the package."
        );

        return "PACKAGE_DROPPED: Drone " + droneId;
    }

    @Override
    public String returnToBase(String droneId) {
        System.out.println(
                "Compensation: Drone " + droneId
                        + " returning to base..."
        );

        System.out.println(
                "Drone " + droneId + " returned to base."
        );

        return "COMPENSATION: Drone " + droneId
                + " returned to base.";
    }
}
