
package com.aerosaga.backend;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DroneFleetRegistry {

    private static final int DRONES_PER_STATE = 10;

    private static final Map<String, DroneAssignment> DRONES =
            new LinkedHashMap<>();

    static {
        for (StateBase base : StateBaseRegistry.getAllBases().values()) {

            String stateCode = getStateCode(base.getStateName());

            for (int i = 1; i <= DRONES_PER_STATE; i++) {

                String droneId = String.format(
                        "DR-%s-%03d",
                        stateCode,
                        i
                );

                if (DRONES.containsKey(droneId)) {
                    throw new IllegalStateException(
                            "Duplicate drone ID: " + droneId
                    );
                }

                DRONES.put(
                        droneId,
                        new DroneAssignment(droneId, base)
                );
            }
        }
    }

    private static String getStateCode(String stateName) {

        return switch (stateName) {
            case "Madhya Pradesh" -> "MP";
            case "Maharashtra" -> "MH";
            case "Manipur" -> "MN";
            case "Uttar Pradesh" -> "UP";
            case "Uttarakhand" -> "UK";
            default -> stateName
                    .replaceAll("[^A-Za-z]", "")
                    .substring(0, 2)
                    .toUpperCase();
        };
    }

    public static DroneAssignment getDrone(String droneId) {
        return DRONES.get(droneId);
    }

    public static List<DroneAssignment> getAllDrones() {
        return Collections.unmodifiableList(
                new ArrayList<>(DRONES.values())
        );
    }

    public static List<DroneAssignment> getDronesByState(
            String stateName) {

        List<DroneAssignment> result = new ArrayList<>();

        for (DroneAssignment drone : DRONES.values()) {
            if (drone.getStateName().equalsIgnoreCase(stateName)) {
                result.add(drone);
            }
        }

        return Collections.unmodifiableList(result);
    }

    public static int getDroneCount() {
        return DRONES.size();
    }

    public static int getDronesPerState() {
        return DRONES_PER_STATE;
    }
}
