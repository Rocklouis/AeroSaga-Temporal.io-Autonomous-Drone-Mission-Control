
package com.aerosaga.backend;

public class TakeoffActivityImpl implements TakeoffActivity {

    private void simulateDelay(String droneId, String activity) {
        try {
            System.out.println(
                    "Drone " + droneId + " executing " + activity + "..."
            );
            Thread.sleep(15000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Activity interrupted for " + droneId, e
            );
        }
    }

    @Override
    public String performTakeoff(String droneId) {
        simulateDelay(droneId, "Takeoff");

        if ("DR-FAIL".equals(droneId)) {
            throw new RuntimeException(
                    "Simulated takeoff failure for " + droneId
            );
        }

        System.out.println("Drone " + droneId + " took off successfully.");
        return "SUCCESS: Drone " + droneId + " has taken off.";
    }

    @Override
    public String navigateToDropPoint(String droneId) {
        simulateDelay(droneId, "Navigation");
        return "NAVIGATED: Drone " + droneId
                + " reached the drop point.";
    }

    @Override
    public String dropPackage(String droneId) {
        simulateDelay(droneId, "Package Drop");

        if ("DR-DROP-FAIL".equals(droneId)) {
            throw new RuntimeException(
                    "Simulated package-drop failure for " + droneId
            );
        }

        return "PACKAGE_DROPPED: Drone " + droneId;
    }

    @Override
    public String returnToBase(String droneId) {
        simulateDelay(droneId, "Return to Base");

        System.out.println("Drone " + droneId + " returned to base.");
        return "COMPENSATION: Drone " + droneId
                + " returned to base.";
    }
}
