package com.example.api_gateway.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.DefaultServerRequest;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.test.publisher.TestPublisher;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoggingFilterTest {

    @InjectMocks
    private LoggingFilter filter;

    @Test
    void filter_logsRequestAndResponse() {
        // Given
        try (MockedStatic<LocalDateTime> localDateTimeMock = mockStatic(LocalDateTime.class)) {
            localDateTimeMock.when(LocalDateTime::now).thenReturn(LocalDateTime.of(2024, 1, 1, 12, 0));

            ServerHttpRequest request = mock(ServerHttpRequest.class);
            ServerHttpResponse response = mock(ServerHttpResponse.class);
            ServerWebExchange exchange = mock(ServerWebExchange.class);
            GatewayFilterChain chain = mock(GatewayFilterChain.class);

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-User-Id", "user123");

            PathContainer path = PathContainer.parsePath("/api/users/123");
            when(exchange.getRequest()).thenReturn(request);
            when(exchange.getResponse()).thenReturn(response);
            when(request.getHeaders()).thenReturn(headers);
            when(request.getPath()).thenReturn(path);
            when(request.getMethod()).thenReturn(HttpMethod.GET);
            when(chain.filter(exchange)).thenReturn(Mono.empty());
            when(response.getStatusCode()).thenReturn(HttpStatus.OK);

            // When
            Mono<Void> result = filter.apply(LoggingFilter.Config.class).filter(exchange, chain);

            // Then
            StepVerifier.create(result)
                    .verifyComplete();

            verify(exchange.getRequest().getHeaders(), atLeastOnce()).getFirst("X-User-Id");
            verify(chain).filter(exchange);
        }
    }

    @Test
    void filter_logsErrorStatus() {
        // Given
        try (MockedStatic<LocalDateTime> localDateTimeMock = mockStatic(LocalDateTime.class)) {
            localDateTimeMock.when(LocalDateTime::now).thenReturn(LocalDateTime.of(2024, 1, 1, 12, 0));

            ServerHttpRequest request = mock(ServerHttpRequest.class);
            ServerHttpResponse response = mock(ServerHttpResponse.class);
            ServerWebExchange exchange = mock(ServerWebExchange.class);
            GatewayFilterChain chain = mock(GatewayFilterChain.class);

            when(exchange.getRequest()).thenReturn(request);
            when(exchange.getResponse()).thenReturn(response);
            when(chain.filter(exchange)).thenReturn(Mono.error(new RuntimeException("test")));
            when(response.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);

            // When
            Mono<Void> result = filter.apply(LoggingFilter.Config.class).filter(exchange, chain);

            // Then
            StepVerifier.create(result)
                    .expectError()
                    .verify();

            verify(chain).filter(exchange);
        }
    }

    @Test
    void filter_logsAnonymousUser() {
        // Given anonymous user
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        GatewayFilterChain chain = mock(GatewayFilterChain.class);

        HttpHeaders headers = new HttpHeaders();

        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(request.getHeaders()).thenReturn(headers);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        // When
        Mono<Void> result = filter.apply(LoggingFilter.Config.class).filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
    }
}
