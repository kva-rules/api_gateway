package com.example.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class ApiGatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)  // Disable CSRF for API Gateway
            .authorizeExchange(exchanges -> exchanges
                // Permit auth endpoints (login/register)
                .pathMatchers("/api/auth/**").permitAll()
                // Permit actuator for monitoring
                .pathMatchers("/actuator/**", "/actuator/health").permitAll()
                // Protect all other routes with JWT
                .anyExchange().authenticated()
            )
            .oauth2ResourceServer(ServerHttpSecurity.OAuth2ResourceServerSpec::jwt);
        
        return http.build();
    }
}
