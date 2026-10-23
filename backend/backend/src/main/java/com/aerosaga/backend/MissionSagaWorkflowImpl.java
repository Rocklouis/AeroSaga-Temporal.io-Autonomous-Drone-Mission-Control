
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

        
        // Wait for all drone activities to finish.
        try {
            Promise.allOf(promises.toArray(new Promise<?>[0])).get();
        } catch (RuntimeException ignored) {
            // Handle each drone's result individually below.
        }

        List<String> results = new ArrayList<>();

        for (int i = 0; i < droneIds.size(); i++) {
            String droneId = droneIds.get(i);

            try {
                String result = promises.get(i).get();
                droneStatuses.put(droneId, "AIRBORNE");
                results.add(result);
            } catch (RuntimeException exception) {
                droneStatuses.put(droneId, "FAILED");
                results.add(
                    "FAILED: Drone " + droneId
                    + " takeoff failed: " + exception.getMessage()
                );
            }
        }

        return results;
    }


    @Override
    public Map<String, String> getDroneStatuses() {
        return new LinkedHashMap<>(droneStatuses);
    }
}
