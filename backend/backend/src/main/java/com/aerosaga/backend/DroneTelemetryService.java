
package com.aerosaga.backend;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DroneTelemetryService {

    private final Map<String, Map<String, Object>> telemetry =
            new ConcurrentHashMap<>();

    public DroneTelemetryService() {

        for (DroneAssignment assignment :
                DroneFleetRegistry.getAllDrones()) {

            Map<String, Object> drone = new ConcurrentHashMap<>();

            drone.put("id", assignment.getDroneId());
            drone.put("state", assignment.getStateName());
            drone.put("base", assignment.getBaseName());

            drone.put("lat", assignment.getBaseLatitude());
            drone.put("lon", assignment.getBaseLongitude());

            drone.put("baseLatitude", assignment.getBaseLatitude());
            drone.put("baseLongitude", assignment.getBaseLongitude());

            drone.put("altitude", 0);
            drone.put("status", "AT_BASE");
            drone.put("timestamp", Instant.now().toString());

            telemetry.put(assignment.getDroneId(), drone);
        }

        System.out.println(
                "Drone telemetry initialized: "
                        + telemetry.size() + " drones."
        );
    }

    public void updateDrone(
            String droneId,
            double latitude,
            double longitude,
            double altitude,
            String status) {

        Map<String, Object> drone = telemetry.get(droneId);

        if (drone == null) {
            return;
        }

        drone.put("lat", latitude);
        drone.put("lon", longitude);
        drone.put("altitude", altitude);
        drone.put("status", status);
        drone.put("timestamp", Instant.now().toString());
    }

    public void setStatus(String droneId, String status) {

        Map<String, Object> drone = telemetry.get(droneId);

        if (drone != null) {
            drone.put("status", status);
            drone.put("timestamp", Instant.now().toString());
        }
    }

    public Map<String, Object> getDrone(String droneId) {
        return telemetry.get(droneId);
    }

    public List<Map<String, Object>> getAllTelemetry() {

        List<Map<String, Object>> result =
                new ArrayList<>(telemetry.values());

        result.sort(
                Comparator.comparing(
                        drone -> drone.get("id").toString()
                )
        );

        return result;
    }

    public int getDroneCount() {
        return telemetry.size();
    }
}
