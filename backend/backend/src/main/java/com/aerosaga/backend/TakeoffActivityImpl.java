
package com.aerosaga.backend;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TakeoffActivityImpl implements TakeoffActivity {

    private final DroneTelemetryService telemetryService;

    @Autowired
    public TakeoffActivityImpl(DroneTelemetryService telemetryService) {
        this.telemetryService = telemetryService;
    }

    // Compatibility with the old standalone worker.
    public TakeoffActivityImpl() {
        this(new DroneTelemetryService());
    }

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

    private void moveDrone(
            String droneId,
            double targetLat,
            double targetLon,
            double targetAltitude,
            String status) {

        Map<String, Object> drone =
                telemetryService.getDrone(droneId);

        if (drone == null) {
            simulateDelay(droneId, status);
            return;
        }

        double startLat = ((Number) drone.get("lat")).doubleValue();
        double startLon = ((Number) drone.get("lon")).doubleValue();
        double startAltitude =
                ((Number) drone.get("altitude")).doubleValue();

        try {
            for (int step = 1; step <= 15; step++) {

                double progress = step / 15.0;

                double latitude =
                        startLat + (targetLat - startLat) * progress;

                double longitude =
                        startLon + (targetLon - startLon) * progress;

                double altitude =
                        startAltitude
                                + (targetAltitude - startAltitude) * progress;

                telemetryService.updateDrone(
                        droneId,
                        latitude,
                        longitude,
                        altitude,
                        status
                );

                Thread.sleep(1000);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(
                    "Movement interrupted for " + droneId, e
            );
        }
    }

    @Override
    public String performTakeoff(String droneId) {

        telemetryService.setStatus(droneId, "TAKING_OFF");

        moveDrone(
                droneId,
                getCurrentLatitude(droneId),
                getCurrentLongitude(droneId),
                120,
                "TAKING_OFF"
        );

        if ("DR-FAIL".equals(droneId)) {
            telemetryService.setStatus(droneId, "FAILED");
            throw new RuntimeException(
                    "Simulated takeoff failure for " + droneId
            );
        }

        System.out.println("Drone " + droneId + " took off successfully.");

        telemetryService.setStatus(droneId, "IN_FLIGHT");

        return "SUCCESS: Drone " + droneId + " has taken off.";
    }

    @Override
    public String navigateToDropPoint(String droneId) {

        DroneAssignment assignment =
                DroneFleetRegistry.getDrone(droneId);

        if (assignment == null) {
            simulateDelay(droneId, "Navigation");
            return "NAVIGATED: Drone " + droneId
                    + " reached the drop point.";
        }

        int offset = Math.floorMod(droneId.hashCode(), 10);

        double destinationLat =
                assignment.getBaseLatitude() + 0.035 + offset * 0.002;

        double destinationLon =
                assignment.getBaseLongitude() + 0.035 + offset * 0.002;

        moveDrone(
                droneId,
                destinationLat,
                destinationLon,
                120,
                "MOVING_TO_DROP_POINT"
        );

        telemetryService.setStatus(droneId, "AT_DROP_POINT");

        return "NAVIGATED: Drone " + droneId
                + " reached the drop point.";
    }

    @Override
    public String dropPackage(String droneId) {

        telemetryService.setStatus(droneId, "DROPPING_PACKAGE");

        simulateDelay(droneId, "Package Drop");

        if ("DR-DROP-FAIL".equals(droneId)) {
            telemetryService.setStatus(droneId, "DROP_FAILED");

            throw new RuntimeException(
                    "Simulated package-drop failure for " + droneId
            );
        }

        telemetryService.setStatus(droneId, "PACKAGE_DELIVERED");

        return "PACKAGE_DROPPED: Drone " + droneId;
    }

    @Override
    public String returnToBase(String droneId) {

        simulateDelay(droneId, "Return to Base");

        telemetryService.setStatus(droneId, "RETURNED_TO_BASE");

        System.out.println("Drone " + droneId + " returned to base.");

        return "RETURNED_TO_BASE: Drone " + droneId
                + " returned to base.";
    }

    @Override
    public String returnToAssignedBase(
            String droneId,
            String baseName,
            double baseLatitude,
            double baseLongitude) {

        System.out.println(
                "Drone " + droneId
                        + " returning to assigned base: " + baseName
        );

        moveDrone(
                droneId,
                baseLatitude,
                baseLongitude,
                0,
                "RETURNING_TO_BASE"
        );

        telemetryService.setStatus(droneId, "AT_BASE");

        System.out.println(
                "Drone " + droneId
                        + " reached its assigned base: " + baseName
        );

        return "RETURNED_TO_ASSIGNED_BASE: Drone " + droneId
                + " returned to " + baseName;
    }

    private double getCurrentLatitude(String droneId) {
        Map<String, Object> drone =
                telemetryService.getDrone(droneId);

        return drone == null
                ? 0
                : ((Number) drone.get("lat")).doubleValue();
    }

    private double getCurrentLongitude(String droneId) {
        Map<String, Object> drone =
                telemetryService.getDrone(droneId);

        return drone == null
                ? 0
                : ((Number) drone.get("lon")).doubleValue();
    }
}
