
package com.aerosaga.backend;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowStub;
import io.temporal.client.WorkflowQueryException;

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

    // Start a new mission
    @PostMapping
    public Map<String, Object> startMission(
            @RequestBody List<String> droneIds) {

        if (droneIds == null || droneIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Provide at least one drone ID."
            );
        }

        if (droneIds.stream().anyMatch(
                id -> id == null || id.isBlank())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Drone IDs cannot be blank."
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

    // Get current drone statuses
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

        } catch (WorkflowQueryException queryError) {
            try {
                WorkflowStub stub = WorkflowStub.fromTyped(workflow);
                List<?> results = stub.getResult(List.class);

                Map<String, String> statuses =
                        new LinkedHashMap<>();

                for (Object item : results) {
                    String result = String.valueOf(item);

                    if (result.startsWith("SUCCESS: Drone ")
                            && result.contains("completed delivery")) {

                        String marker = "SUCCESS: Drone ";
                        String ending =
                                " completed delivery and returned to base.";

                        if (result.endsWith(ending)) {
                            String droneId = result.substring(
                                    marker.length(),
                                    result.length() - ending.length()
                            );
                            statuses.put(droneId, "MISSION_COMPLETED");
                        }

                    } else if (result.startsWith("FAILED: Drone ")
                            && result.contains("takeoff failed.")) {

                        String marker = "FAILED: Drone ";
                        String ending = " takeoff failed.";

                        int end = result.indexOf(ending);
                        if (end > marker.length()) {
                            String droneId = result.substring(
                                    marker.length(), end
                            );
                            statuses.put(droneId, "FAILED");
                        }

                    } else if (result.startsWith("COMPENSATION FAILED: Drone ")) {

                        String marker = "COMPENSATION FAILED: Drone ";
                        String ending = " could not return to base.";

                        if (result.endsWith(ending)) {
                            String droneId = result.substring(
                                    marker.length(),
                                    result.length() - ending.length()
                            );
                            statuses.put(droneId, "COMPENSATION_FAILED");
                        }

                    } else if (result.startsWith("COMPENSATION: Drone ")) {

                        String marker = "COMPENSATION: Drone ";
                        int end = result.indexOf(" returned to base");

                        if (end > marker.length()) {
                            String droneId = result.substring(
                                    marker.length(), end
                            );
                            statuses.put(droneId, "RETURNED_TO_BASE");
                        }

                    } else if (result.startsWith("SUCCESS: Drone ")
                            && result.endsWith(" has taken off.")) {

                        String marker = "SUCCESS: Drone ";
                        String ending = " has taken off.";

                        String droneId = result.substring(
                                marker.length(),
                                result.length() - ending.length()
                        );
                        statuses.put(droneId, "AIRBORNE");
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

    // Get mission DAG steps
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
