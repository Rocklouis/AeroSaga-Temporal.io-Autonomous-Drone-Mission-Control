
package com.aerosaga.backend;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface TakeoffActivity {

    @ActivityMethod
    String performTakeoff(String droneId);

    @ActivityMethod
    String returnToBase(String droneId);
}
