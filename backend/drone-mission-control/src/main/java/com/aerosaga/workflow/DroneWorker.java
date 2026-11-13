package com.aerosaga.workflow;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;

public class DroneWorker {

public static void main(String[] args) {

    WorkflowServiceStubs service = WorkflowServiceStubs.newLocalServiceStubs();

    WorkflowClient client = WorkflowClient.newInstance(service);

    WorkerFactory factory = WorkerFactory.newInstance(client);

    Worker worker = factory.newWorker("DRONE_TASK_QUEUE");

    worker.registerWorkflowImplementationTypes(
        DroneWorkflowImpl.class,
        MultiDroneWorkflowImpl.class
);

    worker.registerActivitiesImplementations(new DroneActivitiesImpl());

    factory.start();

    System.out.println("AeroSaga Drone Worker started successfully!");
}

}