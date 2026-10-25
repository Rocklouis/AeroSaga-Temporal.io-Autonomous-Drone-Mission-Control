package com.aerosaga.sim.engine;

import com.aerosaga.sim.config.SimProperties;
import com.aerosaga.sim.model.Drone;
import com.aerosaga.sim.model.DroneState;
import com.aerosaga.sim.telemetry.TelemetryPublisher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class FleetEngine {

    private static final Logger log = LoggerFactory.getLogger(FleetEngine.class);

    private final SimProperties props;
    private final TelemetryPublisher publisher;
    private final Map<String, Drone> drones = new ConcurrentHashMap<>();
    private final AtomicLong tickCount = new AtomicLong();
    private ScheduledExecutorService scheduler;

    public FleetEngine(SimProperties props, TelemetryPublisher publisher) {
        this.props = props;
        this.publisher = publisher;
    }

    @PostConstruct
    public void start() {
        for (Drone d : FleetFactory.build(props)) {
            drones.put(d.getDroneId(), d);
        }
        log.info("AeroSaga fleet ready: {} drones (DRONE-01 … DRONE-{})",
                drones.size(), String.format("%02d", props.getFleetSize()));

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "fleet-tick");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(
                this::safeTick,
                props.getTickIntervalMs(),
                props.getTickIntervalMs(),
                TimeUnit.MILLISECONDS
        );
    }

    @PreDestroy
    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private void safeTick() {
        try {
            List<DroneState> batch = new ArrayList<>(drones.size());
            for (Drone d : drones.values()) {
                d.tick();
                batch.add(d.toState());
            }
            long n = tickCount.incrementAndGet();
            publisher.publish(batch);
            if (n % 60 == 0) {
                long online = batch.stream().filter(DroneState::isOnline).count();
                log.info("Tick #{} — online {}/{}", n, online, batch.size());
            }
        } catch (Exception e) {
            log.warn("Tick failed: {}", e.getMessage());
        }
    }

    public List<DroneState> listAll() {
        List<DroneState> out = new ArrayList<>(drones.size());
        for (Drone d : drones.values()) {
            out.add(d.toState());
        }
        out.sort(Comparator.comparing(DroneState::getDroneId));
        return out;
    }

    public DroneState get(String droneId) {
        return require(droneId).toState();
    }

    public DroneState takeoff(String droneId) {
        Drone d = require(droneId);
        d.takeoff();
        log.info("{} TAKEOFF", droneId);
        return d.toState();
    }

    public DroneState assignMission(String droneId, double dropLat, double dropLng) {
        Drone d = require(droneId);
        d.assignMission(dropLat, dropLng);
        log.info("{} MISSION drop=({}, {})", droneId, dropLat, dropLng);
        return d.toState();
    }

    public DroneState rtb(String droneId) {
        Drone d = require(droneId);
        d.forceReturnToBase();
        log.info("{} RTB forced", droneId);
        return d.toState();
    }

    public DroneState offline(String droneId) {
        Drone d = require(droneId);
        d.goOffline();
        log.warn("{} OFFLINE (storm/link loss)", droneId);
        return d.toState();
    }

    public DroneState reconnect(String droneId) {
        Drone d = require(droneId);
        d.reconnect();
        log.info("{} RECONNECTED phase={}", droneId, d.getPhase());
        return d.toState();
    }

    /** Week 2: start missions on all drones for Promise.allOf demos. */
    public List<DroneState> startAllMissions() {
        java.util.Random rng = new java.util.Random(99);
        List<DroneState> out = new ArrayList<>();
        for (Drone d : drones.values()) {
            DroneState cur = d.toState();
            double dropLat = cur.getLat() + (rng.nextDouble() - 0.5) * 0.08;
            double dropLng = cur.getLng() + (rng.nextDouble() - 0.5) * 0.08;
            d.assignMission(dropLat, dropLng);
            out.add(d.toState());
        }
        out.sort(Comparator.comparing(DroneState::getDroneId));
        log.info("start-all: missions assigned to {} drones", out.size());
        return out;
    }

    /** Mid-review chaos: take N drones offline. */
    public List<DroneState> storm(int count) {
        List<Drone> list = new ArrayList<>(drones.values());
        list.sort(Comparator.comparing(Drone::getDroneId));
        int n = Math.min(count, list.size());
        List<DroneState> hit = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            list.get(i).goOffline();
            hit.add(list.get(i).toState());
        }
        log.warn("STORM: {} drones offline", n);
        return hit;
    }

    public Map<String, Object> stats() {
        long online = drones.values().stream().filter(Drone::isOnline).count();
        return Map.of(
                "fleetSize", drones.size(),
                "online", online,
                "offline", drones.size() - online,
                "tickCount", tickCount.get()
        );
    }

    private Drone require(String droneId) {
        Drone d = drones.get(droneId);
        if (d == null) {
            throw new NoSuchElementException("Unknown droneId: " + droneId + " (expected DRONE-01…DRONE-50)");
        }
        return d;
    }
}