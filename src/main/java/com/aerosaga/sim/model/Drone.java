package com.aerosaga.sim.model;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single drone world object.
 * Thread-safety: all mutating ops are synchronized (tick loop + REST).
 */
public class Drone {

    private final String droneId;
    private final DroneType droneType;
    private final double maxPayloadKg;
    private final double cruiseStep;
    private final int batteryDrainPerTick;
    private final double cruiseAltitudeM;
    private final double climbRateM;
    private final int lowBatteryThreshold;
    private final int reconnectRtbThreshold;

    private double lat;
    private double lng;
    private double alt;
    private int battery = 100;
    private double headingRad;
    private boolean payloadAttached = true;

    private final double homeLat;
    private final double homeLng;
    private double targetLat;
    private double targetLng;

    private MissionPhase phase = MissionPhase.IDLE;
    private MissionPhase phaseBeforeOffline = MissionPhase.IDLE;
    private final AtomicBoolean online = new AtomicBoolean(true);

    public Drone(
            String droneId,
            DroneType droneType,
            double lat,
            double lng,
            double maxPayloadKg,
            double cruiseStep,
            int batteryDrainPerTick,
            double cruiseAltitudeM,
            double climbRateM,
            double headingRad,
            int lowBatteryThreshold,
            int reconnectRtbThreshold
    ) {
        this.droneId = droneId;
        this.droneType = droneType;
        this.lat = lat;
        this.lng = lng;
        this.homeLat = lat;
        this.homeLng = lng;
        this.targetLat = lat;
        this.targetLng = lng;
        this.maxPayloadKg = maxPayloadKg;
        this.cruiseStep = cruiseStep;
        this.batteryDrainPerTick = batteryDrainPerTick;
        this.cruiseAltitudeM = cruiseAltitudeM;
        this.climbRateM = climbRateM;
        this.headingRad = headingRad;
        this.lowBatteryThreshold = lowBatteryThreshold;
        this.reconnectRtbThreshold = reconnectRtbThreshold;
        this.alt = 0;
    }

    // ── Commands (Temporal activities / demo curls call these via REST) ──

    /** Week 1: takeoff command. */
    public synchronized void takeoff() {
        if (!online.get()) {
            return;
        }
        if (phase == MissionPhase.IDLE || phase == MissionPhase.LANDED) {
            this.targetLat = homeLat;
            this.targetLng = homeLng;
            this.payloadAttached = true;
            this.phase = MissionPhase.TAKEOFF;
        }
    }

    /** Week 2: full delivery mission. */
    public synchronized void assignMission(double dropLat, double dropLng) {
        if (!online.get()) {
            return;
        }
        this.targetLat = dropLat;
        this.targetLng = dropLng;
        this.payloadAttached = true;
        if (phase == MissionPhase.LANDED || phase == MissionPhase.IDLE) {
            this.alt = 0;
        }
        this.phase = MissionPhase.TAKEOFF;
    }

    public synchronized void forceReturnToBase() {
        if (!online.get()) {
            return;
        }
        if (phase != MissionPhase.LANDED && phase != MissionPhase.IDLE) {
            this.phase = MissionPhase.RETURN_TO_BASE;
        }
    }

    /** Mid-review: storm / link loss — freeze world state. */
    public synchronized void goOffline() {
        if (!online.get()) {
            return;
        }
        phaseBeforeOffline = phase;
        phase = MissionPhase.OFFLINE;
        online.set(false);
    }

    /** Mid-review: reconnect — resume saga-compatible phase. */
    public synchronized void reconnect() {
        if (online.get()) {
            return;
        }
        online.set(true);

        if (battery <= reconnectRtbThreshold) {
            phase = MissionPhase.RETURN_TO_BASE;
            return;
        }

        MissionPhase resume = phaseBeforeOffline;
        if (resume == MissionPhase.OFFLINE || resume == null) {
            resume = MissionPhase.RETURN_TO_BASE;
        }
        // DROP is instantaneous in sim; don't stick there after reconnect
        if (resume == MissionPhase.DROP_PAYLOAD) {
            resume = payloadAttached ? MissionPhase.NAVIGATE_TO_DROP : MissionPhase.RETURN_TO_BASE;
        }
        phase = resume;
    }

    // ── Simulation tick ──

    public synchronized void tick() {
        if (!online.get()) {
            return; // durable pause from the world's point of view
        }

        if (battery <= lowBatteryThreshold
                && phase != MissionPhase.LANDED
                && phase != MissionPhase.IDLE
                && phase != MissionPhase.RETURN_TO_BASE) {
            phase = MissionPhase.RETURN_TO_BASE;
        }

        switch (phase) {
            case TAKEOFF -> doTakeoff();
            case NAVIGATE_TO_DROP -> doNavigateToDrop();
            case DROP_PAYLOAD -> doDrop();
            case RETURN_TO_BASE -> doReturnToBase();
            default -> { /* IDLE / LANDED */ }
        }

        if (phase != MissionPhase.IDLE && phase != MissionPhase.LANDED) {
            battery = Math.max(0, battery - batteryDrainPerTick);
            if (battery == 0 && phase != MissionPhase.LANDED) {
                phase = MissionPhase.RETURN_TO_BASE;
            }
        }
    }

    private void doTakeoff() {
        alt = Math.min(cruiseAltitudeM, alt + climbRateM);
        if (alt >= cruiseAltitudeM) {
            // If target == home (takeoff-only), hover by going to navigate then immediately RTB-ready
            if (near(targetLat, targetLng, cruiseStep * 2) && near(homeLat, homeLng, cruiseStep * 2)) {
                // Week-1 takeoff with no drop: hold cruise at home
                phase = MissionPhase.NAVIGATE_TO_DROP;
            } else {
                phase = MissionPhase.NAVIGATE_TO_DROP;
            }
        }
    }

    private void doNavigateToDrop() {
        // Takeoff-only with target at home: stay in cruise (frontend still sees motion of others)
        if (near(targetLat, targetLng, cruiseStep * 1.5) && near(homeLat, homeLng, cruiseStep * 1.5)
                && payloadAttached && Math.hypot(targetLat - homeLat, targetLng - homeLng) < 1e-8) {
            return;
        }
        stepToward(targetLat, targetLng, cruiseStep);
        if (near(targetLat, targetLng, cruiseStep * 1.5)) {
            lat = targetLat;
            lng = targetLng;
            phase = MissionPhase.DROP_PAYLOAD;
        }
    }

    private void doDrop() {
        payloadAttached = false;
        phase = MissionPhase.RETURN_TO_BASE;
    }

    private void doReturnToBase() {
        stepToward(homeLat, homeLng, cruiseStep * 0.95);
        if (near(homeLat, homeLng, cruiseStep * 1.5)) {
            lat = homeLat;
            lng = homeLng;
            alt = Math.max(0, alt - climbRateM);
            if (alt <= 0) {
                alt = 0;
                phase = MissionPhase.LANDED;
            }
        }
    }

    private void stepToward(double destLat, double destLng, double step) {
        double dLat = destLat - lat;
        double dLng = destLng - lng;
        double dist = Math.hypot(dLat, dLng);
        if (dist < 1e-12) {
            return;
        }
        double use = Math.min(step, dist);
        lat += use * (dLat / dist);
        lng += use * (dLng / dist);
        headingRad = Math.atan2(dLng, dLat);
    }

    private boolean near(double aLat, double aLng, double eps) {
        return Math.hypot(lat - aLat, lng - aLng) <= eps;
    }

    public synchronized DroneState toState() {
        DroneState s = new DroneState();
        s.setDroneId(droneId);
        s.setLat(round(lat, 6));
        s.setLng(round(lng, 6));
        s.setAlt(round(alt, 1));
        s.setBattery(battery);
        s.setPhase(phase.name());
        s.setStatus(phase.name());
        s.setOnline(online.get());
        s.setHeadingDeg(round(Math.toDegrees(headingRad), 1));
        s.setPayloadKg(payloadAttached ? maxPayloadKg : 0.0);
        s.setDroneType(droneType.name());
        s.setTimestamp(System.currentTimeMillis());
        return s;
    }

    private static double round(double v, int places) {
        double p = Math.pow(10, places);
        return Math.round(v * p) / p;
    }

    public String getDroneId() { return droneId; }
    public MissionPhase getPhase() { return phase; }
    public boolean isOnline() { return online.get(); }
    public DroneType getDroneType() { return droneType; }
}