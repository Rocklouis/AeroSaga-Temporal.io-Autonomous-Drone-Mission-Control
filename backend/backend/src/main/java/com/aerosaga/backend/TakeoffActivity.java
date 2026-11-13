
package com.aerosaga.backend;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface TakeoffActivity {

    @ActivityMethod
    String performTakeoff(String droneId);

    @ActivityMethod
    String navigateToDropPoint(String droneId);

    @ActivityMethod
    String dropPackage(String droneId);

    @ActivityMethod
    String returnToBase(String droneId);

    // New activity: return to the drone's assigned state-capital base.
    @ActivityMethod
    String returnToAssignedBase(
            String droneId,
            String baseName,
            double baseLatitude,
            double baseLongitude
    );
}
