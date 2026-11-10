package com.aerosaga.workflow;

import java.time.Duration;

import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.workflow.Workflow;

public class DroneWorkflowImpl implements DroneWorkflow {

    private boolean abortRequested = false;

    private final DroneActivities activities =
            Workflow.newActivityStub(
                    DroneActivities.class,
                    ActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofSeconds(30))
                            .setRetryOptions(
                                    RetryOptions.newBuilder()
                                            .setMaximumAttempts(1)
                                            .build()
                            )
                            .build()
            );

    @Override
    public void emergencyAbort() {
        abortRequested = true;
    }

    @Override
    public String takeoff(String droneId) {

        activities.takeoff(droneId);

        if (abortRequested) {
            activities.returnToBase(droneId);
            return "Emergency abort requested. Drone returned to base.";
        }

        activities.navigate(droneId);

        if (abortRequested) {
            activities.returnToBase(droneId);
            return "Emergency abort requested. Drone returned to base.";
        }

        try {
            activities.dropPackage(droneId);
        } catch (ActivityFailure e) {
            activities.returnToBase(droneId);
            return "Package drop failed. Drone returned to base.";
        }

        if (abortRequested) {
            activities.returnToBase(droneId);
            return "Emergency abort requested. Drone returned to base.";
        }

        activities.returnToBase(droneId);

        return "Mission completed successfully.";
    }
}