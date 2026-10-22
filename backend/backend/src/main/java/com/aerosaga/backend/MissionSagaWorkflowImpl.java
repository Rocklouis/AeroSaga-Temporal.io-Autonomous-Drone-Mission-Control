
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
            droneStatuses.put(droneId, "TAKEOFF_REQUESTED");

            Promise<String> promise = Async.function(
                    takeoffActivity::performTakeoff,
                    droneId
            );

            promises.add(promise);
        }

        try {
            Promise.allOf(promises.toArray(new Promise<?>[0])).get();

            List<String> results = new ArrayList<>();

            for (int i = 0; i < droneIds.size(); i++) {
                results.add(promises.get(i).get());
                droneStatuses.put(droneIds.get(i), "AIRBORNE");
            }

            return results;
        } catch (RuntimeException exception) {
            for (String droneId : droneIds) {
                if (!"AIRBORNE".equals(droneStatuses.get(droneId))) {
                    droneStatuses.put(droneId, "FAILED");
                }
            }

            throw exception;
        }
    }

    @Override
    public Map<String, String> getDroneStatuses() {
        return new LinkedHashMap<>(droneStatuses);
    }
}
