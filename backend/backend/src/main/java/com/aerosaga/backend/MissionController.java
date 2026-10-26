
package com.aerosaga.backend;
import io.temporal.client.WorkflowQueryException;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
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

        MissionSagaWorkflow workflow =
                workflowClient.newWorkflowStub(
                        MissionSagaWorkflow.class,
                        workflowId
                );

        try {
            Map<String, String> statuses =
                    workflow.getDroneStatuses();

            return Map.of(
                    "workflowId", workflowId,
                    "droneStatuses", statuses
            );

        } catch (io.temporal.client.WorkflowQueryException queryError) {
            try {
                WorkflowStub stub = WorkflowStub.fromTyped(workflow);
                List<?> results = stub.getResult(List.class);

                Map<String, String> statuses =
                        new LinkedHashMap<>();

                for (Object item : results) {
                    String result = String.valueOf(item);

                    if (result.startsWith("SUCCESS: Drone ")
                            && result.endsWith(" has taken off.")) {

                        String droneId = result.substring(
                                "SUCCESS: Drone ".length(),
                                result.indexOf(" has taken off.")
                        );

                        statuses.put(droneId, "AIRBORNE");

                    } else if (result.startsWith("FAILED: Drone ")
                            && result.contains(" takeoff failed.")) {

                        String droneId = result.substring(
                                "FAILED: Drone ".length(),
                                result.indexOf(" takeoff failed.")
                        );

                        statuses.put(droneId, "FAILED");

                    } else if (result.startsWith("COMPENSATION: Drone ")
                            && result.endsWith(" returned to base.")) {

                        String droneId = result.substring(
                                "COMPENSATION: Drone ".length(),
                                result.indexOf(" returned to base.")
                        );

                        statuses.put(droneId, "RETURNED_TO_BASE");

                    } else if (result.startsWith(
                            "COMPENSATION FAILED: Drone ")) {

                        String marker = "COMPENSATION FAILED: Drone ";
                        String ending = " could not return to base.";

                        if (result.endsWith(ending)) {
                            String droneId = result.substring(
                                    marker.length(),
                                    result.length() - ending.length()
                            );

                            statuses.put(
                                    droneId,
                                    "COMPENSATION_FAILED"
                            );
                        }
                    }
                }

                return Map.of(
                        "workflowId", workflowId,
                        "droneStatuses", statuses
                );

            } catch (Exception e) {
                System.err.println(
                        "Failed to retrieve mission result: "
                                + e.getClass().getName()
                                + " - " + e.getMessage()
                );

                throw new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "Could not retrieve mission status or result."
                );
            }
        }
    }
    
    @GetMapping("/{workflowId}/steps")
    public Map<String, Object> getMissionSteps(
            @PathVariable String workflowId) {

        MissionSagaWorkflow workflow =
                workflowClient.newWorkflowStub(
                        MissionSagaWorkflow.class,
                        workflowId
                );

        try {
            Map<String, Map<String, String>> steps =
                    workflow.getMissionSteps();

            return Map.of(
                    "workflowId", workflowId,
                    "missionSteps", steps
            );

        } catch (WorkflowQueryException queryError) {
            return Map.of(
                    "workflowId", workflowId,
                    "missionSteps", Map.of(),
                    "message",
                    "Workflow query unavailable; it may have completed."
            );
        }
    }

}
