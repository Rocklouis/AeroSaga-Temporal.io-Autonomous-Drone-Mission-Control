package com.aerosaga.sim.api;

import com.aerosaga.sim.engine.FleetEngine;
import com.aerosaga.sim.model.DroneState;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sim")
@CrossOrigin(origins = "*")
public class SimController {

    private final FleetEngine engine;

    public SimController(FleetEngine engine) {
        this.engine = engine;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "status", "UP",
                "service", "drone-simulator",
                "stats", engine.stats()
        );
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return engine.stats();
    }

    @GetMapping("/drones")
    public List<DroneState> listDrones() {
        return engine.listAll();
    }

    @GetMapping("/drones/{droneId}")
    public DroneState getDrone(@PathVariable String droneId) {
        return engine.get(droneId);
    }

    @GetMapping("/drones/{droneId}/steps")
    public List<Map<String, String>> steps(@PathVariable String droneId) {
        return missionSteps(engine.get(droneId));
    }

    @PostMapping("/drones/{droneId}/takeoff")
    public DroneState takeoff(@PathVariable String droneId) {
        return engine.takeoff(droneId);
    }

    @PostMapping("/drones/{droneId}/mission")
    public DroneState mission(
            @PathVariable String droneId,
            @Valid @RequestBody MissionRequest req
    ) {
        return engine.assignMission(droneId, req.getDropLat(), req.getDropLng());
    }

    @PostMapping("/drones/{droneId}/rtb")
    public DroneState rtb(@PathVariable String droneId) {
        return engine.rtb(droneId);
    }

    @PostMapping("/drones/{droneId}/fail-drop")
    public DroneState failDrop(@PathVariable String droneId) {
        return engine.failDrop(droneId);
    }

    @PostMapping("/drones/{droneId}/abort")
    public DroneState abort(@PathVariable String droneId) {
        return engine.abort(droneId);
    }

    @PostMapping("/missions/start-all")
    public List<DroneState> startAll() {
        return engine.startAllMissions();
    }

    @PostMapping("/drones/{droneId}/offline")
    public DroneState offline(@PathVariable String droneId) {
        return engine.offline(droneId);
    }

    @PostMapping("/drones/{droneId}/reconnect")
    public DroneState reconnect(@PathVariable String droneId) {
        return engine.reconnect(droneId);
    }

    @PostMapping("/chaos/storm")
    public List<DroneState> storm(@RequestParam(defaultValue = "5") int count) {
        return engine.storm(count);
    }

    private List<Map<String, String>> missionSteps(DroneState s) {
        List<String> order = List.of(
                "TAKEOFF", "NAVIGATE_TO_DROP", "DROP_PAYLOAD", "RETURN_TO_BASE", "LANDED"
        );
        String current = s.getPhase();
        boolean compensating = s.isDropFailed() || s.isAborted();
        int idx = order.indexOf(current);

        List<Map<String, String>> out = new ArrayList<>();
        for (int i = 0; i < order.size(); i++) {
            String name = order.get(i);
            String status;
            if ("IDLE".equals(current)) {
                status = "PENDING";
            } else if ("OFFLINE".equals(current)) {
                status = "BLOCKED";
            } else if (idx < 0) {
                status = "PENDING";
            } else if (i < idx) {
                status = "COMPLETED";
            } else if (i == idx) {
                status = compensating && "RETURN_TO_BASE".equals(name) ? "COMPENSATING" : "ACTIVE";
            } else {
                status = "PENDING";
            }
            if ("DROP_PAYLOAD".equals(name) && s.isDropFailed()) {
                status = "FAILED";
            }
            if (s.isAborted()
                    && !"RETURN_TO_BASE".equals(name)
                    && !"LANDED".equals(name)
                    && ("ACTIVE".equals(status) || "PENDING".equals(status))) {
                status = "ABORTED";
            }
            out.add(Map.of("name", name, "status", status));
        }
        return out;
    }
}