
package com.aerosaga.backend;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/missions")
@CrossOrigin(origins = "http://localhost:5173")
public class MissionController {

    private final WorkflowClient workflowClient;

    public MissionController(WorkflowClient workflowClient) {
        this.workflowClient = workflowClient;
    }

    @PostMapping
    public Map<String, Object> startMission(
            @RequestBody List<String> droneIds) {

        if (droneIds == null || droneIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide at least one drone ID."
            );
        }

        String workflowId =
                "aerosaga-mission-" + UUID.randomUUID();

        MissionSagaWorkflow workflow =
                workflowClient.newWorkflowStub(
                        MissionSagaWorkflow.class,
                        WorkflowOptions.newBuilder()
                                .setWorkflowId(workflowId)
                                .setTaskQueue(DroneWorker.TASK_QUEUE)
                                .build()
                );

        WorkflowClient.start(workflow::launchMission, droneIds);

        return Map.of(
                "workflowId", workflowId,
                "message", "Mission started",
                "droneIds", droneIds
        );
    }

    @GetMapping("/{workflowId}/status")
    public Map<String, Object> getMissionStatus(
            @PathVariable String workflowId) {

        try {
            MissionSagaWorkflow workflow =
                    workflowClient.newWorkflowStub(
                            MissionSagaWorkflow.class,
                            workflowId
                    );

            return Map.of(
                    "workflowId", workflowId,
                    "droneStatuses", workflow.getDroneStatuses()
            );

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Mission status could not be retrieved. Check the workflow ID and whether the workflow is still queryable."
            );
        }
    }
}
