package com.example.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RouteConfig {

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/auth/**")
                        .uri("lb://auth-service"))
                .route("user-service", r -> r.path("/api/users/**")
                        .uri("lb://user-service"))
                .route("ticket-service", r -> r.path("/api/tickets/**")
                        .uri("lb://ticket-service"))
                .route("solution-service", r -> r.path("/api/solutions/**")
                        .uri("lb://solution-service"))
                .route("knowledge-service", r -> r.path("/api/knowledge/**")
                        .uri("lb://knowledge-service"))
                .route("reward-service", r -> r.path("/api/rewards/**")
                        .uri("lb://reward-service"))
                .route("notification-service", r -> r.path("/api/notifications/**")
                        .uri("lb://notification-service"))
                .build();
    }
}
