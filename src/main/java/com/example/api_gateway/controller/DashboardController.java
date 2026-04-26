package com.example.api_gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple4;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Tag(name = "Gateway Dashboard", description = "Aggregate cross-service metrics for the admin UI")
public class DashboardController {

    private final WebClient webClient;

    @Value("${services.user-service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Value("${services.ticket-service.url:http://localhost:8082}")
    private String ticketServiceUrl;

    @Value("${services.reward-service.url:http://localhost:8083}")
    private String rewardServiceUrl;

    @Value("${services.notification-service.url:http://localhost:8084}")
    private String notificationServiceUrl;

    public DashboardController(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @GetMapping(value = "/dashboard", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Aggregate dashboard stats", description = "Fan-out to downstream services and merge stats")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Aggregated dashboard returned"),
            @ApiResponse(responseCode = "504", description = "Upstream service timeout")
    })
    public Mono<Map<String, Object>> dashboard() {
        Mono<Map> userStats = webClient.get()
                .uri(userServiceUrl + "/api/internal/stats")
                .retrieve()
                .bodyToMono(Map.class);

        Mono<Map> ticketStats = webClient.get()
                .uri(ticketServiceUrl + "/api/internal/stats")
                .retrieve()
                .bodyToMono(Map.class);

        Mono<Map> rewardStats = webClient.get()
                .uri(rewardServiceUrl + "/api/internal/stats")
                .retrieve()
                .bodyToMono(Map.class);

        Mono<Map> notificationStats = webClient.get()
                .uri(notificationServiceUrl + "/api/internal/stats")
                .retrieve()
                .bodyToMono(Map.class);

        return Mono.zip(userStats, ticketStats, rewardStats, notificationStats)
                .map(tuple -> {
                    Map<String, Object> dashboard = new HashMap<>();
                    dashboard.put("userService", tuple.getT1());
                    dashboard.put("ticketService", tuple.getT2());
                    dashboard.put("rewardService", tuple.getT3());
                    dashboard.put("notificationService", tuple.getT4());
                    dashboard.put("timestamp", System.currentTimeMillis());
                    return dashboard;
                })
                .timeout(Duration.ofSeconds(5))
                .onErrorReturn(Map.of(
                    "userService", Map.of("error", "Service unavailable"),
                    "ticketService", Map.of("error", "Service unavailable"),
                    "rewardService", Map.of("error", "Service unavailable"),
                    "notificationService", Map.of("error", "Service unavailable"),
                    "timestamp", System.currentTimeMillis()
                ));
    }
}
