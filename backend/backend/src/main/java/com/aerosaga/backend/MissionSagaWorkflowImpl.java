
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

    // Tracks each drone's mission DAG step statuses.
    private final Map<String, Map<String, String>> missionSteps =
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

        List<String> results = new ArrayList<>();
        List<String> successfullyLaunched = new ArrayList<>();
        boolean missionHasFailure = false;

        // Initialize mission steps for each drone.
        for (String droneId : droneIds) {

            droneStatuses.put(droneId, "TAKEOFF_REQUESTED");

            Map<String, String> steps = new LinkedHashMap<>();
            steps.put("Takeoff", "Pending");
            steps.put("Navigate", "Pending");
            steps.put("Drop Package", "Pending");
            steps.put("Return to Base", "Pending");

            missionSteps.put(droneId, steps);
        }

        // Start all takeoff activities concurrently.
        List<Promise<String>> takeoffPromises = new ArrayList<>();

        for (String droneId : droneIds) {

            setStepStatus(droneId, "Takeoff", "Active");

            Promise<String> promise = Async.function(
                    takeoffActivity::performTakeoff,
                    droneId
            );

            takeoffPromises.add(promise);
        }

        try {
            Promise.allOf(
                    takeoffPromises.toArray(new Promise<?>[0])
            ).get();
        } catch (RuntimeException ignored) {
            // Individual takeoff results are handled below.
        }

        // Inspect each takeoff result.
        for (int i = 0; i < droneIds.size(); i++) {

            String droneId = droneIds.get(i);

            try {
                String result = takeoffPromises.get(i).get();

                droneStatuses.put(droneId, "AIRBORNE");
                setStepStatus(droneId, "Takeoff", "Completed");

                successfullyLaunched.add(droneId);
                results.add(result);

            } catch (RuntimeException exception) {

                droneStatuses.put(droneId, "FAILED");
                setStepStatus(droneId, "Takeoff", "Failed");

                missionHasFailure = true;

                results.add(
                        "FAILED: Drone " + droneId
                                + " takeoff failed."
                );
            }
        }

        // If any takeoff fails, compensate successful takeoffs.
        if (missionHasFailure) {

            for (String droneId : successfullyLaunched) {
                compensateReturnToBase(droneId, results);
            }

            return results;
        }

        // Execute remaining mission steps concurrently for all drones.
        List<Promise<List<String>>> missionPromises = new ArrayList<>();

        for (String droneId : droneIds) {

            Promise<List<String>> promise = Async.function(
                    () -> executeRemainingMission(droneId)
            );

            missionPromises.add(promise);
        }

        // Wait for all drone missions to finish.
        Promise.allOf(
                missionPromises.toArray(new Promise<?>[0])
        ).get();

        // Collect results in the original drone order.
        for (Promise<List<String>> promise : missionPromises) {
            results.addAll(promise.get());
        }

        return results;
    }

    // Runs the remaining mission steps for one drone.
    // Activities within this drone remain sequential.
    private List<String> executeRemainingMission(String droneId) {

        List<String> droneResults = new ArrayList<>();

        try {

            // Navigate to drop point.
            droneStatuses.put(droneId, "NAVIGATING");
            setStepStatus(droneId, "Navigate", "Active");

            takeoffActivity.navigateToDropPoint(droneId);

            setStepStatus(droneId, "Navigate", "Completed");

            // Drop package.
            droneStatuses.put(droneId, "DROPPING_PACKAGE");
            setStepStatus(droneId, "Drop Package", "Active");

            takeoffActivity.dropPackage(droneId);

            setStepStatus(droneId, "Drop Package", "Completed");

            // Return to base.
            droneStatuses.put(droneId, "RETURNING_TO_BASE");
            setStepStatus(droneId, "Return to Base", "Active");

            String returnResult = returnDroneToHomeBase(droneId);

            setStepStatus(droneId, "Return to Base", "Completed");
            droneStatuses.put(droneId, "MISSION_COMPLETED");

            droneResults.add(
                    "SUCCESS: Drone " + droneId
                            + " completed delivery and returned to base."
            );

            droneResults.add(returnResult);

        } catch (RuntimeException exception) {

            String activeStep = getActiveStep(droneId);

            if (activeStep != null) {
                setStepStatus(droneId, activeStep, "Failed");
            }

            droneResults.add(
                    "FAILED: Drone " + droneId
                            + " mission failed at " + activeStep + "."
            );

            // Compensate failures during navigation or package drop.
            if ("Navigate".equals(activeStep)
                    || "Drop Package".equals(activeStep)) {

                compensateReturnToBase(droneId, droneResults);

            } else {
                droneStatuses.put(droneId, "FAILED");
            }
        }

        return droneResults;
    }

    private void compensateReturnToBase(
            String droneId,
            List<String> results) {

        droneStatuses.put(droneId, "RETURNING_TO_BASE");
        setStepStatus(droneId, "Return to Base", "Active");

        try {

            String compensationResult = returnDroneToHomeBase(droneId);

            droneStatuses.put(droneId, "RETURNED_TO_BASE");
            setStepStatus(droneId, "Return to Base", "Completed");

            results.add("COMPENSATION: " + compensationResult);

        } catch (RuntimeException exception) {

            droneStatuses.put(droneId, "COMPENSATION_FAILED");
            setStepStatus(droneId, "Return to Base", "Failed");

            results.add(
                    "COMPENSATION FAILED: Drone " + droneId
                            + " could not return to base."
            );
        }
    }

    private void setStepStatus(
            String droneId,
            String step,
            String status) {

        Map<String, String> steps = missionSteps.get(droneId);

        if (steps != null) {
            steps.put(step, status);
        }
    }

    private String getActiveStep(String droneId) {

        Map<String, String> steps = missionSteps.get(droneId);

        if (steps == null) {
            return null;
        }

        for (Map.Entry<String, String> entry : steps.entrySet()) {

            if ("Active".equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        return null;
    }

    @Override
    public Map<String, String> getDroneStatuses() {
        return new LinkedHashMap<>(droneStatuses);
    }

    @Override
public Map<String, Map<String, String>> getMissionSteps() {

    Map<String, Map<String, String>> snapshot =
            new LinkedHashMap<>();

    for (Map.Entry<String, Map<String, String>> entry
            : missionSteps.entrySet()) {

        snapshot.put(
                entry.getKey(),
                new LinkedHashMap<>(entry.getValue())
        );
    }

    return snapshot;
}

private String returnDroneToHomeBase(String droneId) {

    DroneAssignment assignment =
            DroneFleetRegistry.getDrone(droneId);

    // Keep compatibility with older test drone IDs.
    if (assignment == null) {
        return takeoffActivity.returnToBase(droneId);
    }

    return takeoffActivity.returnToAssignedBase(
            droneId,
            assignment.getBaseName(),
            assignment.getBaseLatitude(),
            assignment.getBaseLongitude()
    );
}
}

