
package com.aerosaga.backend;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MissionSagaWorkflowImpl implements MissionSagaWorkflow {

    private final TakeoffActivity takeoffActivity =
            Workflow.newActivityStub(
                    TakeoffActivity.class,
                    ActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofSeconds(30))
                            .setRetryOptions(
                                    RetryOptions.newBuilder()
                                            .setMaximumAttempts(3)
                                            .build()
                            )
                            .build()
            );

    private final Map<String, String> droneStatuses =
            new LinkedHashMap<>();

    private final Map<String, Map<String, String>> missionSteps =
            new LinkedHashMap<>();

    private static final String PENDING = "PENDING";
    private static final String ACTIVE = "ACTIVE";
    private static final String COMPLETED = "COMPLETED";
    private static final String FAILED = "FAILED";
    private static final String SKIPPED = "SKIPPED";

    @Override
    public List<String> launchMission(List<String> droneIds) {
        if (droneIds == null || droneIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "At least one drone ID is required."
            );
        }

        if (droneIds.stream().anyMatch(
                id -> id == null || id.isBlank()
        )) {
            throw new IllegalArgumentException(
                    "Drone IDs cannot be blank."
            );
        }

        if (droneIds.stream().distinct().count() != droneIds.size()) {
            throw new IllegalArgumentException(
                    "Drone IDs must be unique."
            );
        }

        List<Promise<String>> promises = new ArrayList<>();

        for (String droneId : droneIds) {
            droneStatuses.put(droneId, "MISSION_PENDING");

            Map<String, String> steps = new LinkedHashMap<>();
            steps.put("takeoff", PENDING);
            steps.put("navigate", PENDING);
            steps.put("dropPackage", PENDING);
            steps.put("returnToBase", PENDING);

            missionSteps.put(droneId, steps);
            promises.add(
                    Async.function(this::runDroneMission, droneId)
            );
        }

        List<String> results = new ArrayList<>();

        for (Promise<String> promise : promises) {
            try {
                results.add(promise.get());
            } catch (RuntimeException exception) {
                results.add("MISSION_FAILED: " + exception.getMessage());
            }
        }

        return results;
    }

    private String runDroneMission(String droneId) {
        Map<String, String> steps = missionSteps.get(droneId);

        try {
            droneStatuses.put(droneId, "TAKING_OFF");
            steps.put("takeoff", ACTIVE);
            takeoffActivity.performTakeoff(droneId);
            steps.put("takeoff", COMPLETED);
            droneStatuses.put(droneId, "AIRBORNE");

            // TEMPORARY: delay only the recovery-test drone
if ("DR-RECOVERY-01".equals(droneId)) {
    Workflow.sleep(Duration.ofMinutes(10));
}

        } catch (RuntimeException exception) {
            steps.put("takeoff", FAILED);
            steps.put("navigate", SKIPPED);
            steps.put("dropPackage", SKIPPED);
            steps.put("returnToBase", SKIPPED);
            droneStatuses.put(droneId, "FAILED");
            return "FAILED: Drone " + droneId + " takeoff failed.";
        }

        try {
            droneStatuses.put(droneId, "NAVIGATING");
            steps.put("navigate", ACTIVE);
            takeoffActivity.navigateToDropPoint(droneId);
            steps.put("navigate", COMPLETED);

        } catch (RuntimeException exception) {
            steps.put("navigate", FAILED);
            steps.put("dropPackage", SKIPPED);
            return compensate(droneId, steps, "navigation");
        }

        try {
            droneStatuses.put(droneId, "DROPPING_PACKAGE");
            steps.put("dropPackage", ACTIVE);
            takeoffActivity.dropPackage(droneId);
            steps.put("dropPackage", COMPLETED);

        } catch (RuntimeException exception) {
            steps.put("dropPackage", FAILED);
            return compensate(droneId, steps, "package drop");
        }

        try {
            droneStatuses.put(droneId, "RETURNING_TO_BASE");
            steps.put("returnToBase", ACTIVE);
            takeoffActivity.returnToBase(droneId);
            steps.put("returnToBase", COMPLETED);
            droneStatuses.put(droneId, "MISSION_COMPLETED");

            return "SUCCESS: Drone " + droneId
                    + " completed delivery and returned to base.";

        } catch (RuntimeException exception) {
            steps.put("returnToBase", FAILED);
            droneStatuses.put(droneId, "COMPENSATION_FAILED");

            return "COMPENSATION FAILED: Drone " + droneId
                    + " could not return to base.";
        }
    }

    private String compensate(
            String droneId,
            Map<String, String> steps,
            String failureReason
    ) {
        droneStatuses.put(droneId, "COMPENSATING");
        steps.put("returnToBase", ACTIVE);

        try {
            takeoffActivity.returnToBase(droneId);
            steps.put("returnToBase", COMPLETED);
            droneStatuses.put(droneId, "RETURNED_TO_BASE");

            return "COMPENSATION: Drone " + droneId
                    + " returned to base after " + failureReason + " failure.";

        } catch (RuntimeException exception) {
            steps.put("returnToBase", FAILED);
            droneStatuses.put(droneId, "COMPENSATION_FAILED");

            return "COMPENSATION FAILED: Drone " + droneId
                    + " could not return to base.";
        }
    }

    @Override
    public Map<String, String> getDroneStatuses() {
        return new LinkedHashMap<>(droneStatuses);
    }

    @Override
    public Map<String, Map<String, String>> getMissionSteps() {
        Map<String, Map<String, String>> snapshot = new LinkedHashMap<>();

        for (Map.Entry<String, Map<String, String>> entry
                : missionSteps.entrySet()) {
            snapshot.put(
                    entry.getKey(),
                    new LinkedHashMap<>(entry.getValue())
            );
        }

        return snapshot;
    }
}
