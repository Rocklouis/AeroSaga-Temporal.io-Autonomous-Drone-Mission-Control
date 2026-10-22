
package com.aerosaga.backend;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DroneClient {

    public static void main(String[] args) {
        WorkflowServiceStubs service =
                WorkflowServiceStubs.newServiceStubs(
                        WorkflowServiceStubsOptions.newBuilder()
                                .setTarget("127.0.0.1:7233")
                                .build()
                );

        try {
            WorkflowClient client = WorkflowClient.newInstance(service);

            MissionSagaWorkflow workflow = client.newWorkflowStub(
                    MissionSagaWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(DroneWorker.TASK_QUEUE)
                            .setWorkflowId("aerosaga-mission-" + UUID.randomUUID())
                            .build()
            );

            List<String> droneIds =
                    Arrays.asList("DR-001", "DR-002", "DR-003");

            List<String> results = workflow.launchMission(droneIds);

            System.out.println("Mission completed!");
            results.forEach(System.out::println);

            System.out.println("Final drone statuses:");
            System.out.println(workflow.getDroneStatuses());

        } finally {
            service.shutdown();
        }
    }
}
