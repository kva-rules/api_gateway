package com.example.api_gateway.filter;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimiterFilter extends AbstractGatewayFilterFactory<RateLimiterFilter.Config> {

private final Cache<String, AtomicLong> requestCounts = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(1))
            .build();

    public RateLimiterFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            String clientIp = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            
            String key = userId != null ? userId : "anonymous-" + clientIp;
            
            AtomicLong count = requestCounts.get(key, k -> new AtomicLong(0));
            long requests = count.incrementAndGet();
            
            if (requests > 100) {
                // Rate limit exceeded
                exchange.getResponse().getHeaders().add("X-RateLimit-Exceeded", "true");
                exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                return exchange.getResponse().setComplete();
            }
            
            // Proceed
            return chain.filter(exchange);
        };
    }

    public static class Config {
        // Configuration properties
    }
}
