
package com.aerosaga.backend;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface DroneWorkflow {

    @WorkflowMethod
    String takeoff(String droneId);
}
