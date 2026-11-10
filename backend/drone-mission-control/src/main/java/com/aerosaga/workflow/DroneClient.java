package com.aerosaga.workflow;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class DroneClient {

    public static void main(String[] args) {

        WorkflowServiceStubs service =
                WorkflowServiceStubs.newLocalServiceStubs();

        WorkflowClient client =
                WorkflowClient.newInstance(service);

        DroneWorkflow workflow = client.newWorkflowStub(
                DroneWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue("DRONE_TASK_QUEUE")
                        .setWorkflowId("drone-normal-mission-002")
                        .build()
        );

        WorkflowClient.start(workflow::takeoff, "DRONE-001");

        

        String result =
                WorkflowStub.fromTyped(workflow).getResult(String.class);

        System.out.println("Workflow Result: " + result);

        service.shutdown();
    }
}