package com.example.api_gateway.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Component
public class RoleBasedAccessFilter extends AbstractGatewayFilterFactory<RoleBasedAccessFilter.Config> {

    @Autowired
    private JwtTokenValidator jwtTokenValidator;

    public RoleBasedAccessFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            String xUserRoles = exchange.getRequest().getHeaders().getFirst("X-User-Roles");
            
            if (authHeader == null || !authHeader.startsWith("Bearer ") || xUserRoles == null) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String token = authHeader.substring(7);
            try {
                var claims = jwtTokenValidator.validateToken(token);
                List<String> userRoles = jwtTokenValidator.extractRoles(claims);
                
                String path = exchange.getRequest().getURI().getPath();
                
                // Role-based access rules
                if (path.startsWith("/api/users/") && !hasRole(userRoles, "ADMIN")) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
                
                if (path.startsWith("/api/rewards/") && !hasAnyRole(userRoles, "ADMIN", "MANAGER")) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
                
                if (path.startsWith("/api/tickets/") && !hasAnyRole(userRoles, "ENGINEER", "MANAGER")) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
                
                if (path.startsWith("/api/knowledge/") && !hasRole(userRoles, "ENGINEER")) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }

                return chain.filter(exchange);
                
            } catch (Exception e) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }
        };
    }

    private boolean hasRole(List<String> userRoles, String requiredRole) {
        return userRoles.stream().anyMatch(role -> role.equals(requiredRole));
    }

    private boolean hasAnyRole(List<String> userRoles, String... requiredRoles) {
        return Arrays.stream(requiredRoles).anyMatch(role -> hasRole(userRoles, role));
    }

    public static class Config {
        // Empty config class
    }
}
