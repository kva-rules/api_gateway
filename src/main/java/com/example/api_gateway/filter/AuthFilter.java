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
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(AuthFilter.class);
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
                                            // Downstream services parse X-User-Id as a UUID — the legacy "userId"
                                            // field is the auto-increment Long pk (always 0 in this build), so we
                                            // prefer the new "authUserId" claim and fall back to "userId" only when
                                            // the auth service hasn't been redeployed yet. Without this preference
                                            // the knowledge / reward / notification services blow up converting
                                            // "0" → UUID and return 500 Internal Server Error.
                                            String authUserId = extractField(json, "authUserId");
                                            String userId = authUserId != null ? authUserId : extractField(json, "userId");
                                            String role = extractField(json, "role");
                                            // Roles may be in "roles" (list) — fall back if "role" is missing
                                            if (role == null && json.has("roles") && json.get("roles").isArray() && json.get("roles").size() > 0) {
                                                role = json.get("roles").get(0).asText();
                                            }

                                            final String userIdVal = userId != null ? userId : "";
                                            final String roleVal = role != null ? role : "";
                                            // Use ServerHttpRequestDecorator to add headers without mutating Netty's read-only headers
                                            ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest()) {
                                                @Override
                                                public HttpHeaders getHeaders() {
                                                    HttpHeaders headers = new HttpHeaders();
                                                    headers.addAll(super.getHeaders());
                                                    headers.set("X-User-Id", userIdVal);
                                                    headers.set("X-User-Role", roleVal);
                                                    return headers;
                                                }
                                            };
                                            return chain.filter(exchange.mutate().request(decoratedRequest).build());
                                        } catch (Exception e) {
                                            log.error("AuthFilter parse error: {}: {}", e.getClass().getName(), e.getMessage(), e);
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
