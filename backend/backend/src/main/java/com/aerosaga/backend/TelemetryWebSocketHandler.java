
package com.aerosaga.backend;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

@Component
public class TelemetryWebSocketHandler implements WebSocketHandler {

    private final JsonMapper jsonMapper;
    private final DroneTelemetryService telemetryService;

    public TelemetryWebSocketHandler(
            JsonMapper jsonMapper,
            DroneTelemetryService telemetryService) {

        this.jsonMapper = jsonMapper;
        this.telemetryService = telemetryService;
    }

    @Override
    public reactor.core.publisher.Mono<Void> handle(
            WebSocketSession session) {

        Flux<WebSocketMessage> messages =
                Flux.interval(Duration.ZERO, Duration.ofSeconds(1))
                        .map(tick -> createTelemetry())
                        .map(json -> session.textMessage(json));

        return session.send(messages);
    }

    private String createTelemetry() {

        return jsonMapper.writeValueAsString(
                telemetryService.getAllTelemetry()
        );
    }
}
