package com.aerosaga.backend;

import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalWorkerConfig {

    private WorkerFactory workerFactory;

    @Bean
public WorkerFactory temporalWorkerFactory(
        WorkflowClient workflowClient,
        TakeoffActivityImpl takeoffActivity) {

        workerFactory = WorkerFactory.newInstance(workflowClient);

        Worker worker = workerFactory.newWorker(
                DroneWorker.TASK_QUEUE
        );

        worker.registerWorkflowImplementationTypes(
                DroneWorkflowImpl.class,
                MissionSagaWorkflowImpl.class
        );

        worker.registerActivitiesImplementations(
        takeoffActivity
);


        workerFactory.start();

        System.out.println(
                "AeroSaga Temporal Worker started inside Spring Boot."
        );

        System.out.println(
                "Task Queue: " + DroneWorker.TASK_QUEUE
        );

        return workerFactory;
    }

    @PreDestroy
    public void shutdownWorker() {
        if (workerFactory != null) {
            workerFactory.shutdown();
            System.out.println("AeroSaga Temporal Worker stopped.");
        }
    }
}
