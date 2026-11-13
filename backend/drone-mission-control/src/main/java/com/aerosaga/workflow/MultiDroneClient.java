package com.aerosaga.workflow;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;

public class MultiDroneClient {

    public static void main(String[] args) {

        WorkflowServiceStubs service =
                WorkflowServiceStubs.newLocalServiceStubs();

        WorkflowClient client = WorkflowClient.newInstance(service);

        MultiDroneWorkflow workflow = client.newWorkflowStub(
                MultiDroneWorkflow.class,
                WorkflowOptions.newBuilder()
                        .setTaskQueue("DRONE_TASK_QUEUE")
                        .setWorkflowId("multi-drone-takeoff-001")
                        .build()
        );

        String result = workflow.takeoffMultiple(
                new String[]{"DRONE-001", "DRONE-002", "DRONE-003"}
        );

        System.out.println("Workflow Results:");
        System.out.println(result);

        System.out.println("Final status: " + workflow.getStatus());

        service.shutdown();
    }
}