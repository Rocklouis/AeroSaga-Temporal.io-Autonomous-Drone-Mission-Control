package com.aerosaga.sim.engine;

import com.aerosaga.sim.config.SimProperties;
import com.aerosaga.sim.model.DroneState;
import com.aerosaga.sim.telemetry.TelemetryPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FleetEngineTest {

    private FleetEngine engine;

    @BeforeEach
    void setUp() {
        SimProperties props = new SimProperties();
        props.setFleetSize(50);
        props.setPublishTelemetry(false); // unit test: no gateway
        props.setTickIntervalMs(60_000);  // don't thrash while testing
        TelemetryPublisher publisher = new TelemetryPublisher(props);
        engine = new FleetEngine(props, publisher);
        engine.start();
    }

    @AfterEach
    void tearDown() {
        engine.stop();
    }

    @Test
    void fleetHas50StableIds() {
        List<DroneState> all = engine.listAll();
        assertEquals(50, all.size());
        assertEquals("DRONE-01", all.get(0).getDroneId());
        assertEquals("DRONE-50", all.get(49).getDroneId());
    }

    @Test
    void takeoffAndMissionApis() {
        DroneState t = engine.takeoff("DRONE-01");
        assertEquals("TAKEOFF", t.getPhase());

        DroneState m = engine.assignMission("DRONE-03", 19.12, 72.85);
        assertEquals("TAKEOFF", m.getPhase());
    }

    @Test
    void stormAndReconnect() {
        engine.offline("DRONE-01");
        assertFalse(engine.get("DRONE-01").isOnline());
        engine.reconnect("DRONE-01");
        assertTrue(engine.get("DRONE-01").isOnline());
    }
}