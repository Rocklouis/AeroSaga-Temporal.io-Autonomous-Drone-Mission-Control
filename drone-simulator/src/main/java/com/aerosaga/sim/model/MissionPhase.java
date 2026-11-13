package com.aerosaga.sim.model;

/**
 * Maps 1:1 to Temporal saga steps (Workflow person uses the same names in queries).
 */
public enum MissionPhase {
    IDLE,
    TAKEOFF,
    NAVIGATE_TO_DROP,
    DROP_PAYLOAD,
    RETURN_TO_BASE,
    LANDED,
    OFFLINE
}