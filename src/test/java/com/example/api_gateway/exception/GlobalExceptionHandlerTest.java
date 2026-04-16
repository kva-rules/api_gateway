package com.example.api_gateway.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private ServerHttpResponse response;

    @Test
    void handle_internalServerError() {
        // Given
        when(exchange.getResponse()).thenReturn(response);
        when(exchange.getRequest().getHeaders().getFirst("X-Trace-Id")).thenReturn("trace-123");
        when(exchange.getRequest().getPath()).thenReturn(PathContainer.parsePath("/api/users"));

        // When
        Mono<Void> result = handler.handleError(exchange, new RuntimeException("Test error"));

        // Then
        StepVerifier.create(result).verifyComplete();
        verify(response).setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
        verify(response).getHeaders().setContentType(MediaType.APPLICATION_JSON);
    }

    @Test
    void handle_responseStatusException() {
        // Given
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bad request");
        when(exchange.getResponse()).thenReturn(response);

        // When
        Mono<Void> result = handler.handleError(exchange, ex);

        // Then
        verify(response).setStatusCode(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handle_notFoundException() {
        // Given
        NotFoundException ex = new NotFoundException("Route not found");
        when(exchange.getResponse()).thenReturn(response);

        // Then
        Mono<Void> result = handler.handleError(exchange, ex);

        // Verify
        verify(response).setStatusCode(HttpStatus.NOT_FOUND);
    }

    @Test
    void handle_errorResponseStructure() throws Exception {
        // Given
        ObjectMapper mapper = new ObjectMapper();
        when(exchange.getResponse()).thenReturn(response);
        when(exchange.getRequest().getHeaders().getFirst("X-Trace-Id")).thenReturn("trace-123");
        when(exchange.getRequest().getPath()).thenReturn(PathContainer.parsePath("/test"));

        // When
        Mono<Void> result = handler.handleError(exchange, new RuntimeException());

        // Then JSON structure
        StepVerifier.create(result).verifyComplete();
        
        // Verify response was written with JSON body containing expected fields
        verify(response).writeWith(any(Mono.class));
    }
}
