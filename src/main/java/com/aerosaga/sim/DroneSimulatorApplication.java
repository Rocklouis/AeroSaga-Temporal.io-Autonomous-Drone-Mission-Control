package com.aerosaga.sim;

import com.aerosaga.sim.config.SimProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(SimProperties.class)
public class DroneSimulatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(DroneSimulatorApplication.class, args);
    }
}