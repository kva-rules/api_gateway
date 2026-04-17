package com.example.api_gateway.exception;

import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class GlobalExceptionHandler {

    @org.springframework.core.annotation.Order(-2)
    public Mono<Void> handleError(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();
        
        Map<String, Object> errorResponse = new LinkedHashMap<>();
        errorResponse.put("timestamp", LocalDateTime.now());
        errorResponse.put("traceId", exchange.getRequest().getHeaders().getFirst("X-Trace-Id"));
        errorResponse.put("path", exchange.getRequest().getPath());
        
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "Internal Server Error";
        
        if (ex instanceof ResponseStatusException statusEx) {
            status = HttpStatus.valueOf(statusEx.getStatusCode().value());
            message = statusEx.getReason();
        } else if (ex instanceof NotFoundException) {
            status = HttpStatus.NOT_FOUND;
            message = "Route not found";
        }
        
        errorResponse.put("error", status.getReasonPhrase());
        errorResponse.put("message", message);
        errorResponse.put("status", status.value());
        
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.setStatusCode(status);
        
        try {
            return response.writeWith(Mono.just(response.bufferFactory().wrap(
                new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsBytes(errorResponse)
            )));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            return response.writeWith(Mono.just(response.bufferFactory().wrap(
                "{\"error\":\"Internal Server Error\"}".getBytes()
            )));
        }
    }
}
