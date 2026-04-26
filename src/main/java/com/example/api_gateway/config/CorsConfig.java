package com.example.api_gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.pattern.PathPatternParser;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Allowed origins — cover local dev (Vite :3000 / :5173) and k8s ingress.
        // Use allowedOriginPatterns so wildcards can co-exist with allowCredentials=true.
        corsConfig.setAllowedOriginPatterns(List.of(
                "http://localhost:3000",
                "http://localhost:5173",
                "http://127.0.0.1:3000",
                "http://127.0.0.1:5173",
                "http://ticketing.local",
                "http://ticketing.local:*"
        ));

        // Allow every standard HTTP method.
        corsConfig.addAllowedMethod(CorsConfiguration.ALL);

        // Allow every header — Authorization, Content-Type, custom X-* headers, etc.
        corsConfig.addAllowedHeader(CorsConfiguration.ALL);

        // Expose Authorization so the frontend can read tokens from responses if needed.
        corsConfig.setExposedHeaders(List.of("Authorization", "Content-Disposition"));

        // Credentials (cookies, Authorization header).
        corsConfig.setAllowCredentials(true);

        // Cache preflight for 1 hour.
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource(
                new PathPatternParser());
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
