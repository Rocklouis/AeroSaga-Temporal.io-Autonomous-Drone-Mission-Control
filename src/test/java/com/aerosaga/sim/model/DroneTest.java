package com.aerosaga.sim.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DroneTest {

    private Drone heavy() {
        return new Drone(
                "DRONE-01", DroneType.HEAVY_CARGO,
                19.0, 72.8,
                20.0, 0.00008, 2, 120.0, 8.0, 0.0,
                10, 15
        );
    }

    @Test
    void week1_takeoffClimbs() {
        Drone d = heavy();
        d.takeoff();
        assertEquals(MissionPhase.TAKEOFF, d.getPhase());
        for (int i = 0; i < 20; i++) {
            d.tick();
        }
        assertTrue(d.toState().getAlt() > 50.0);
        assertTrue(d.isOnline());
    }

    @Test
    void week2_missionReachesDropAndReturns() {
        Drone d = heavy();
        d.assignMission(19.0 + 0.0005, 72.8 + 0.0005);
        boolean sawNavigate = false;
        boolean sawRtb = false;
        for (int i = 0; i < 500; i++) {
            d.tick();
            MissionPhase p = d.getPhase();
            if (p == MissionPhase.NAVIGATE_TO_DROP) sawNavigate = true;
            if (p == MissionPhase.RETURN_TO_BASE || p == MissionPhase.LANDED) sawRtb = true;
            if (p == MissionPhase.LANDED) break;
        }
        assertTrue(sawNavigate, "should navigate");
        assertTrue(sawRtb, "should return/land");
        assertEquals(0.0, d.toState().getPayloadKg(), 0.01);
    }

    @Test
    void midReview_offlineFreezesPosition() {
        Drone d = heavy();
        d.assignMission(19.05, 72.85);
        for (int i = 0; i < 10; i++) d.tick();
        double lat = d.toState().getLat();
        double lng = d.toState().getLng();
        d.goOffline();
        assertEquals(MissionPhase.OFFLINE, d.getPhase());
        for (int i = 0; i < 20; i++) d.tick();
        assertEquals(lat, d.toState().getLat(), 1e-9);
        assertEquals(lng, d.toState().getLng(), 1e-9);
        assertFalse(d.isOnline());
    }

    @Test
    void midReview_reconnectResumes() {
        Drone d = heavy();
        d.assignMission(19.05, 72.85);
        for (int i = 0; i < 5; i++) d.tick();
        d.goOffline();
        d.reconnect();
        assertTrue(d.isOnline());
        assertNotEquals(MissionPhase.OFFLINE, d.getPhase());
    }

@Test
void compensation_lowBatteryForcesRtb() {
    // Drain 50/tick so battery hits 0 quickly and forces compensation
    Drone d = new Drone(
            "DRONE-02", DroneType.HEAVY_CARGO,
            19.0, 72.8,
            20.0, 0.00008, 50, 120.0, 8.0, 0.0,
            10, 15
    );
    d.assignMission(19.1, 72.9);

    d.tick(); // battery 100 → 50, still TAKEOFF
    d.tick(); // battery 50 → 0 → must enter RETURN_TO_BASE

    assertEquals(
            MissionPhase.RETURN_TO_BASE,
            d.getPhase(),
            "low battery should force return-to-base compensation"
    );
}
}