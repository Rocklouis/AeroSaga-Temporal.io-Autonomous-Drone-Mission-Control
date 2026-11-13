
package com.aerosaga.backend;

public class FleetRegistryTest {

    public static void main(String[] args) {

        System.out.println("========== AEROSAGA FLEET ==========");

        System.out.println(
                "Total state bases: "
                + StateBaseRegistry.getBaseCount()
        );

        System.out.println(
                "Total drones: "
                + DroneFleetRegistry.getDroneCount()
        );

        System.out.println(
                "Drones per state: "
                + DroneFleetRegistry.getDronesPerState()
        );

        System.out.println("\n========== STATE ASSIGNMENTS ==========");

        for (StateBase base : StateBaseRegistry.getAllBases().values()) {

            int count = DroneFleetRegistry
                    .getDronesByState(base.getStateName())
                    .size();

            System.out.println(
                    base.getStateName()
                    + " | Base: " + base.getBaseName()
                    + " | Drones: " + count
            );
        }

        System.out.println("\n========== SAMPLE DRONE ==========");

        DroneAssignment drone =
                DroneFleetRegistry.getDrone("DR-WE-001");

        System.out.println("Drone ID: " + drone.getDroneId());
        System.out.println("State: " + drone.getStateName());
        System.out.println("Base: " + drone.getBaseName());
        System.out.println("Latitude: " + drone.getBaseLatitude());
        System.out.println("Longitude: " + drone.getBaseLongitude());
    }
}
