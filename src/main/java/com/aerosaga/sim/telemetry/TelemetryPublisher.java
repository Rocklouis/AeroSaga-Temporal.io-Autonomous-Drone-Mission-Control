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

/**
 * Pushes authoritative GPS batch to Telemetry Gateway each tick.
 * If gateway/Spring is killed (mid-review), sim KEEPS RUNNING — does not crash.
 */
@Component
public class TelemetryPublisher {

    private static final Logger log = LoggerFactory.getLogger(TelemetryPublisher.class);

    private final RestClient client = RestClient.builder().build();
    private final String gatewayUrl;
    private final boolean enabled;
    private final AtomicLong failStreak = new AtomicLong();

    public TelemetryPublisher(SimProperties props) {
        this.gatewayUrl = props.getGatewayUrl();
        this.enabled = props.isPublishTelemetry();
    }

    public void publish(List<DroneState> batch) {
        if (!enabled) {
            return;
        }
        try {
            client.post()
                    .uri(gatewayUrl)
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
                        n, gatewayUrl, e.getMessage());
            }
        }
    }
}