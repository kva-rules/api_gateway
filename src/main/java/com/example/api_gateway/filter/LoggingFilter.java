package com.example.api_gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
public class LoggingFilter extends AbstractGatewayFilterFactory<LoggingFilter.Config> {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);

    public LoggingFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String requestId = UUID.randomUUID().toString();
            LocalDateTime startTime = LocalDateTime.now();
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
            String endpoint = exchange.getRequest().getPath().toString();
            String method = exchange.getRequest().getMethod().name();

            logger.info("Request [{}] {} {} - User: {} - Start: {}", 
                requestId, method, endpoint, userId != null ? userId : "anonymous", startTime);

            return chain.filter(exchange).then(Mono.fromRunnable(() -> {
                LocalDateTime endTime = LocalDateTime.now();
                int responseStatus = exchange.getResponse().getStatusCode() != null 
                    ? exchange.getResponse().getStatusCode().value() : 500;
                long responseTime = java.time.Duration.between(startTime, endTime).toMillis();

                logger.info("Response [{}] Status: {} ResponseTime: {}ms - End: {}", 
                    requestId, responseStatus, responseTime, endTime);
            }));
        };
    }

    public static class Config {
        // Empty config class
    }
}
