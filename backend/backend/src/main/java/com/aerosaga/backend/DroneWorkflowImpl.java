
package com.aerosaga.backend;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;

import java.time.Duration;

public class DroneWorkflowImpl implements DroneWorkflow {

    private final TakeoffActivity takeoffActivity =
            Workflow.newActivityStub(
                    TakeoffActivity.class,
                    ActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofSeconds(30))
                            .build()
            );

    @Override
    public String takeoff(String droneId) {
        return takeoffActivity.performTakeoff(droneId);
    }
}
