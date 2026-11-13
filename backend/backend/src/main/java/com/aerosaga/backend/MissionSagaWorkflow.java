package com.aerosaga.backend;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.List;
import java.util.Map;

@WorkflowInterface
public interface MissionSagaWorkflow {

    @QueryMethod
    Map<String, Map<String, String>> getMissionSteps();

    @WorkflowMethod
    List<String> launchMission(List<String> droneIds);

    @QueryMethod
    Map<String, String> getDroneStatuses();
}
