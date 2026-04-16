package com.example.api_gateway.config;

import com.example.api_gateway.filter.LoggingFilter;
import com.example.api_gateway.filter.RateLimiterFilter;
import com.example.api_gateway.security.AuthenticationFilter;
import com.example.api_gateway.security.RoleBasedAccessFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Autowired
    private AuthenticationFilter authenticationFilter;

    @Autowired
    private RoleBasedAccessFilter roleBasedAccessFilter;

    @Autowired
    private RateLimiterFilter rateLimiterFilter;

    @Autowired
    private LoggingFilter loggingFilter;

    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route("auth-service", r -> r.path("/api/auth/**")
                        .filters(f -> f.filter(authenticationFilter.apply(new AuthenticationFilter.Config())))
                        .uri("lb://auth-service"))
                .route("user-service", r -> r.path("/api/users/**")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(roleBasedAccessFilter.apply(new RoleBasedAccessFilter.Config()))
                                .filter(rateLimiterFilter.apply(new RateLimiterFilter.Config()))
                                .filter(loggingFilter.apply(new LoggingFilter.Config())))
                        .uri("lb://user-service"))
                .route("ticket-service", r -> r.path("/api/tickets/**")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(roleBasedAccessFilter.apply(new RoleBasedAccessFilter.Config()))
                                .filter(rateLimiterFilter.apply(new RateLimiterFilter.Config()))
                                .filter(loggingFilter.apply(new LoggingFilter.Config())))
                        .uri("lb://ticket-service"))
                .route("solution-service", r -> r.path("/api/solutions/**")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(rateLimiterFilter.apply(new RateLimiterFilter.Config()))
                                .filter(loggingFilter.apply(new LoggingFilter.Config())))
                        .uri("lb://solution-service"))
                .route("knowledge-service", r -> r.path("/api/knowledge/**")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(roleBasedAccessFilter.apply(new RoleBasedAccessFilter.Config()))
                                .filter(rateLimiterFilter.apply(new RateLimiterFilter.Config()))
                                .filter(loggingFilter.apply(new LoggingFilter.Config())))
                        .uri("lb://knowledge-service"))
                .route("reward-service", r -> r.path("/api/rewards/**")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(roleBasedAccessFilter.apply(new RoleBasedAccessFilter.Config()))
                                .filter(rateLimiterFilter.apply(new RateLimiterFilter.Config()))
                                .filter(loggingFilter.apply(new LoggingFilter.Config())))
                        .uri("lb://reward-service"))
                .route("notification-service", r -> r.path("/api/notifications/**")
                        .filters(f -> f
                                .filter(authenticationFilter.apply(new AuthenticationFilter.Config()))
                                .filter(rateLimiterFilter.apply(new RateLimiterFilter.Config()))
                                .filter(loggingFilter.apply(new LoggingFilter.Config())))
                        .uri("lb://notification-service"))
                .build();
    }
}
