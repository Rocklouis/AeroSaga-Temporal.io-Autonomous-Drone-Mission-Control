package com.aerosaga.sim.telemetry;

import com.aerosaga.sim.config.SimProperties;
import com.aerosaga.sim.model.DroneState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class TelemetryPublisher {

    private static final Logger log = LoggerFactory.getLogger(TelemetryPublisher.class);

    private final RestClient client = RestClient.builder().build();
    private final SimProperties props;
    private final AtomicLong failStreak = new AtomicLong();

    public TelemetryPublisher(SimProperties props) {
        this.props = props;
        log.info("Telemetry publish enabled={}", props.isPublishTelemetry());
        log.info("Telemetry gateway url={}", props.getGatewayUrl());
    }

    public void publish(List<DroneState> batch) {
        // Read live from config every tick (yml / defaults)
        if (!props.isPublishTelemetry()) {
            return;
        }

        try {
            client.post()
                    .uri(props.getGatewayUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(batch)
                    .retrieve()
                    .toBodilessEntity();

            long fails = failStreak.getAndSet(0);
            if (fails > 0) {
                log.info("Telemetry restored after {} failed ticks", fails);
            }
        } catch (Exception e) {
            long n = failStreak.incrementAndGet();
            if (n == 1L || n % 30 == 0L) {
                log.warn("Gateway unreachable (fail streak={}). Sim continues. url={} msg={}",
                        n, props.getGatewayUrl(), e.getMessage());
            }
        }
    }
}