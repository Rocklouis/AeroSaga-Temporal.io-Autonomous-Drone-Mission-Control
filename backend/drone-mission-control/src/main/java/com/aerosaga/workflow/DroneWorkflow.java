package com.aerosaga.workflow;

import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface DroneWorkflow {

    @WorkflowMethod
    String takeoff(String droneId);
    @SignalMethod
    void emergencyAbort();
}