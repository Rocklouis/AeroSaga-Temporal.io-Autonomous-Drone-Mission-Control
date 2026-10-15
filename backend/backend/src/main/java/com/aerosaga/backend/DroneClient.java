
package com.aerosaga.backend;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

import java.util.UUID;

public class DroneClient {

    public static void main(String[] args) {
        WorkflowServiceStubs service =
                WorkflowServiceStubs.newLocalServiceStubs();

        WorkflowClient client = WorkflowClient.newInstance(service);

        DroneWorkflow workflow = client.newWorkflowStub(
                DroneWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue(DroneWorker.TASK_QUEUE)
                        .setWorkflowId("aerosaga-takeoff-" + UUID.randomUUID())
                        .build()
        );

        String result = workflow.takeoff("DR-001");

        System.out.println("Workflow result: " + result);

        service.shutdown();
    }
}
