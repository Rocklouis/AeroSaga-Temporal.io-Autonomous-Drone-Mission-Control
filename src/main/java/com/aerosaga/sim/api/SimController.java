package com.aerosaga.sim.api;

import com.aerosaga.sim.engine.FleetEngine;
import com.aerosaga.sim.model.DroneState;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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

    // ── Health / stats ──

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

    // ── Week 1: fleet snapshot for static Cesium markers ──

    @GetMapping("/drones")
    public List<DroneState> listDrones() {
        return engine.listAll();
    }

    @GetMapping("/drones/{droneId}")
    public DroneState getDrone(@PathVariable String droneId) {
        return engine.get(droneId);
    }

    // ── Week 1: Takeoff activity target ──

    @PostMapping("/drones/{droneId}/takeoff")
    public DroneState takeoff(@PathVariable String droneId) {
        return engine.takeoff(droneId);
    }

    // ── Week 2: full mission saga + multi-drone ──

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

    @PostMapping("/missions/start-all")
    public List<DroneState> startAll() {
        return engine.startAllMissions();
    }

    // ── Mid-review: resiliency / storm ──

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
}