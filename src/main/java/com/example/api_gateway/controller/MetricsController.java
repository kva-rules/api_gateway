package com.example.api_gateway.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/gateway")
@Tag(name = "Gateway Metrics", description = "Latency and traffic counters across routed services")
public class MetricsController {

    private static long requestCount = 0;
    private static long errorCount = 0;
    private static long totalLatency = 0;

    @GetMapping(value = "/metrics", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get gateway traffic metrics", description = "Request count, error rate and average latency")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metrics returned")
    })
    public ResponseEntity<Map<String, Object>> metrics() {
        double errorRate = requestCount > 0 ? (double) errorCount / requestCount * 100 : 0;
        double avgLatency = requestCount > 0 ? totalLatency / requestCount : 0;

        Map<String, Object> metrics = Map.of(
            "requestCount", requestCount,
            "errorRate", String.format("%.2f%%", errorRate),
            "avgLatencyMs", avgLatency
        );

        return ResponseEntity.ok(metrics);
    }

    // Static methods for filters to update metrics
    public static synchronized void incrementRequest() {
        requestCount++;
    }

    public static synchronized void incrementError() {
        errorCount++;
    }

    public static synchronized void addLatency(long latencyMs) {
        totalLatency += latencyMs;
    }
}
