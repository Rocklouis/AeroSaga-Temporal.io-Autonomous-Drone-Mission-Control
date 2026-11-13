package com.aerosaga.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface MultiDroneWorkflow {

    @WorkflowMethod
    String takeoffMultiple(String[] droneIds);

    @QueryMethod
    String getStatus();
}