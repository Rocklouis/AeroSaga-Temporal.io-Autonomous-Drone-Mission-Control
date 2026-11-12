
package com.aerosaga.backend;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/fleet")
@CrossOrigin(origins = "http://localhost:5173")
public class FleetController {

    @GetMapping("/bases")
    public Map<String, StateBase> getAllBases() {
        return StateBaseRegistry.getAllBases();
    }

    @GetMapping("/drones")
    public List<DroneAssignment> getAllDrones() {
        return DroneFleetRegistry.getAllDrones();
    }

    @GetMapping("/summary")
    public Map<String, Integer> getFleetSummary() {
        return Map.of(
            "totalBases", StateBaseRegistry.getBaseCount(),
            "totalDrones", DroneFleetRegistry.getDroneCount(),
            "dronesPerState", DroneFleetRegistry.getDronesPerState()
        );
    }
}
