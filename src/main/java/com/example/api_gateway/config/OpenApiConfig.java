package com.example.api_gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Set;

/**
 * OpenAPI 3.0 configuration for the API Gateway.
 *
 * <p>The gateway aggregates Swagger UI for all downstream services so a single URL
 * — <code>http://localhost:8080/swagger-ui.html</code> — exposes every service's API
 * via a dropdown. Each entry hits <code>/v3/api-docs/&lt;service&gt;</code>, which is
 * proxied to the corresponding downstream service's <code>/v3/api-docs</code> endpoint
 * (see {@link SwaggerRouteConfig}).</p>
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI gatewayOpenAPI() {
        final String schemeName = "bearerAuth";
        return new OpenAPI()
                .info(new Info()
                        .title("API Gateway")
                        .version("1.0.0")
                        .description("""
                                Spring Cloud Gateway routing + JWT validation for all microservices.
                                This aggregator UI lists every downstream service's OpenAPI spec —
                                pick one from the dropdown at the top-right.
                                """)
                        .contact(new Contact().name("Platform Team").email("platform@example.com"))
                        .license(new License().name("Internal")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local gateway"),
                        new Server().url("http://ticketing.local").description("K8s ingress")))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components()
                        .addSecuritySchemes(schemeName, new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Paste the `accessToken` returned by POST /api/auth/login")));
    }

    /**
     * Aggregates the downstream services' Swagger specs into a single UI dropdown.
     * The URLs here are served by the gateway itself (gateway routes in
     * application-local.yaml proxy /v3/api-docs/&lt;service&gt; → downstream /v3/api-docs).
     *
     * <p>NOTE: We MUTATE the springdoc-autoconfigured {@link SwaggerUiConfigProperties}
     * bean rather than declaring our own — declaring a second bean of the same type
     * makes injection fail with "expected single matching bean but found 2" in
     * springdoc's webflux SwaggerConfig. {@code @Autowired} + {@code @PostConstruct}
     * lets us configure the existing one cleanly.</p>
     */
    @Autowired
    private SwaggerUiConfigProperties swaggerUiConfigProperties;

    @PostConstruct
    public void configureSwaggerAggregator() {
        swaggerUiConfigProperties.setUrls(Set.of(
                url("auth-service",         "/v3/api-docs/auth-service"),
                url("user-service",         "/v3/api-docs/user-service"),
                url("ticket-service",       "/v3/api-docs/ticket-service"),
                url("solution-service",     "/v3/api-docs/solution-service"),
                url("knowledge-service",    "/v3/api-docs/knowledge-service"),
                url("reward-service",       "/v3/api-docs/reward-service"),
                url("notification-service", "/v3/api-docs/notification-service")
        ));
        swaggerUiConfigProperties.setOperationsSorter("method");
        swaggerUiConfigProperties.setTagsSorter("alpha");
        swaggerUiConfigProperties.setTryItOutEnabled(true);
    }

    private static AbstractSwaggerUiConfigProperties.SwaggerUrl url(String name, String path) {
        AbstractSwaggerUiConfigProperties.SwaggerUrl u = new AbstractSwaggerUiConfigProperties.SwaggerUrl();
        u.setName(name);
        u.setUrl(path);
        u.setDisplayName(name);
        return u;
    }
}
