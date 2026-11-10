package com.aerosaga.workflow;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Async;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;

public class MultiDroneWorkflowImpl implements MultiDroneWorkflow {

    private String status = "INITIALIZED";

    private final DroneActivities activities =
            Workflow.newActivityStub(
                    DroneActivities.class,
                    ActivityOptions.newBuilder()
                            .setStartToCloseTimeout(Duration.ofSeconds(30))
                            .build()
            );

    @Override
    public String takeoffMultiple(String[] droneIds) {
        status = "IN_PROGRESS";

        List<Promise<String>> promises = new ArrayList<>();

        for (String droneId : droneIds) {
            Promise<String> promise =
                    Async.function(activities::takeoff, droneId);
            promises.add(promise);
        }

        Promise.allOf(promises.toArray(new Promise<?>[0])).get();

        List<String> results = new ArrayList<>();

        for (Promise<String> promise : promises) {
            results.add(promise.get());
        }

        status = "COMPLETED";

        return String.join("\n", results);
    }

    @Override
    public String getStatus() {
        return status;
    }
}