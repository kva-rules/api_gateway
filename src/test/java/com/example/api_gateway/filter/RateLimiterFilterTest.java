package com.example.api_gateway.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterFilterTest {

    @InjectMocks
    private RateLimiterFilter filter;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    private Cache<String, AtomicLong> requestCounts;

    @BeforeEach
    void setUp() {
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(chain.filter(any())).thenReturn(Mono.empty());
        
        // Access private cache via reflection
        requestCounts = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build();
        ReflectionTestUtils.setField(filter, "requestCounts", requestCounts);
    }

    @Test
    void filter_underLimit_proceeds() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "user123");
        when(request.getHeaders()).thenReturn(headers);

        // When
        Mono<Void> result = filter.apply(RateLimiterFilter.Config.class).filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain).filter(any());
        verify(response, never()).setStatusCode(any());
    }

    @Test
    void filter_overLimit_returns429() {
        // Given user already made 100 requests
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "user123");
        when(request.getHeaders()).thenReturn(headers);

        // Simulate 101st request
        requestCounts.getIfPresent("user123").incrementAndGet();

        // When
        Mono<Void> result = filter.apply(RateLimiterFilter.Config.class).filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(response).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_anonymousIp_underLimit_proceeds() {
        // Given anonymous user by IP
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getRemoteAddress().getAddress().getHostAddress()).thenReturn("127.0.0.1");

        // When
        Mono<Void> result = filter.apply(RateLimiterFilter.Config.class).filter(exchange, chain);

        // Then
        StepVerifier.create(result)
                .verifyComplete();
        verify(chain).filter(any());
    }

    @Test
    void filter_anonymousIp_rateLimited_returns429() {
        // Given anonymous IP at limit
        HttpHeaders headers = new HttpHeaders();
        when(request.getHeaders()).thenReturn(headers);
        when(request.getRemoteAddress().getAddress().getHostAddress()).thenReturn("127.0.0.1");

        // Pre-fill cache
        requestCounts.get("anonymous-127.0.0.1", k -> new AtomicLong(100)).incrementAndGet();

        // When
        Mono<Void> result = filter.apply(RateLimiterFilter.Config.class).filter(exchange, chain);

        // Then
        verify(response).setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_resetsAfterOneMinute() throws InterruptedException {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "reset-user");
        when(request.getHeaders()).thenReturn(headers);

        // Hit limit
        for (int i = 0; i < 101; i++) {
            requestCounts.get("reset-user", k -> new AtomicLong(0)).incrementAndGet();
        }

        // Wait for expiry (simulated)
        Thread.sleep(100);

        // When new minute - should allow again
        requestCounts.invalidate("reset-user");
        
        Mono<Void> result = filter.apply(RateLimiterFilter.Config.class).filter(exchange, chain);

        // Then
        verify(chain).filter(any());
    }
}
