package com.example.api_gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${auth-service.url:http://auth-service:8081}")
    private String authServiceUrl;

    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/auth/login",
            "/api/auth/register"
    );

    public AuthFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // Skip auth check for open endpoints
            if (isOpenEndpoint(path)) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onUnauthorized(exchange.getResponse(), "Missing or invalid Authorization header");
            }

            // Call auth-service to validate token
            return webClient.get()
                    .uri(authServiceUrl + "/api/auth/validate")
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .exchangeToMono(response -> {
                        if (response.statusCode().is2xxSuccessful()) {
                            return response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        try {
                                            JsonNode json = objectMapper.readTree(body);
                                            String userId = extractField(json, "userId");
                                            String role = extractField(json, "role");

                                            // Add headers for downstream services
                                            ServerHttpRequest mutatedRequest = request.mutate()
                                                    .header("X-User-Id", userId != null ? userId : "")
                                                    .header("X-User-Role", role != null ? role : "")
                                                    .build();

                                            return chain.filter(exchange.mutate().request(mutatedRequest).build());
                                        } catch (Exception e) {
                                            return onUnauthorized(exchange.getResponse(), "Failed to parse auth response");
                                        }
                                    });
                        } else {
                            return onUnauthorized(exchange.getResponse(), "Token validation failed");
                        }
                    })
                    .onErrorResume(e -> onUnauthorized(exchange.getResponse(), "Auth service unavailable"));
        };
    }

    private boolean isOpenEndpoint(String path) {
        return OPEN_ENDPOINTS.stream().anyMatch(path::startsWith);
    }

    private String extractField(JsonNode json, String fieldName) {
        // Try direct field access
        if (json.has(fieldName)) {
            return json.get(fieldName).asText();
        }
        // Try nested in "data" object
        if (json.has("data") && json.get("data").has(fieldName)) {
            return json.get("data").get(fieldName).asText();
        }
        return null;
    }

    private Mono<Void> onUnauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String errorBody = String.format(
                "{\"success\":false,\"message\":\"%s\",\"data\":null}",
                message
        );

        byte[] bytes = errorBody.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}
