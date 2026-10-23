
package com.aerosaga.backend;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class TelemetryWebSocketHandler implements WebSocketHandler {

    private final JsonMapper jsonMapper;

    public TelemetryWebSocketHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public reactor.core.publisher.Mono<Void> handle(WebSocketSession session) {
        Flux<org.springframework.web.reactive.socket.WebSocketMessage> messages =
                Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
                        .map(this::createTelemetry)
                        .map(session::textMessage);

        return session.send(messages);
    }

    private String createTelemetry(long tick) {
        double time = tick / 5.0;

        List<Map<String, Object>> drones = List.of(
                Map.of(
                        "id", "DR-001",
                        "lat", 22.5726 + 0.03 * Math.sin(time),
                        "lon", 88.3639 + 0.03 * Math.cos(time),
                        "altitude", 120,
                        "timestamp", Instant.now().toString()
                ),
                Map.of(
                        "id", "DR-002",
                        "lat", 28.6139 + 0.02 * Math.sin(time + 2),
                        "lon", 77.2090 + 0.02 * Math.cos(time + 2),
                        "altitude", 150,
                        "timestamp", Instant.now().toString()
                ),
                Map.of(
                        "id", "DR-003",
                        "lat", 19.0760 + 0.025 * Math.sin(time + 4),
                        "lon", 72.8777 + 0.025 * Math.cos(time + 4),
                        "altitude", 100,
                        "timestamp", Instant.now().toString()
                )
        );

        return jsonMapper.writeValueAsString(drones);
    }
}
