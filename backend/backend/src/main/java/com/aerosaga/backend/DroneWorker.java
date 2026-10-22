
package com.aerosaga.backend;


import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class DroneWorker {

    public static final String TASK_QUEUE = "AEROSAGA_DRONE_TASK_QUEUE";

    public static void main(String[] args) {
        WorkflowServiceStubs service =
        WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder()
                        .setTarget("127.0.0.1:7233")
                        .build()
        );

        WorkflowClient client = WorkflowClient.newInstance(service);

        WorkerFactory factory = WorkerFactory.newInstance(client);

        Worker worker = factory.newWorker(TASK_QUEUE);

        worker.registerWorkflowImplementationTypes(
        DroneWorkflowImpl.class,
        MissionSagaWorkflowImpl.class
);
        worker.registerActivitiesImplementations(new TakeoffActivityImpl());

        factory.start();

        System.out.println("AeroSaga Drone Worker is running.");
        System.out.println("Task Queue: " + TASK_QUEUE);
    }
}
