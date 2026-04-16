package com.example.api_gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayRoutingTest {

    @LocalServerPort
    private int port;

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private ApplicationContext context;

    @Test
    void testUserServiceRouting() {
        RouteLocator routeLocator = context.getBean(RouteConfig.class).routeLocator(null);
        
        // Test all defined routes map correctly
        webClient.get()
                .uri("/api/users/123")
                .exchange()
                .expectStatus().isOk() // Mock downstream would respond
                .expectHeader().valueEquals("X-Route", "user-service");
    }

    @Test
    void testTicketServiceRouting() {
        webClient.get()
                .uri("/api/tickets/456")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Route", "ticket-service");
    }

    @Test
    void testRewardServiceRouting() {
        webClient.get()
                .uri("/api/rewards/list")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Route", "reward-service");
    }

    @Test
    void testKnowledgeServiceRouting() {
        webClient.get()
                .uri("/api/knowledge/docs")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Route", "knowledge-service");
    }

    @Test
    void testNotificationServiceRouting() {
        webClient.get()
                .uri("/api/notifications/123")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Route", "notification-service");
    }

    @Test
    void testAuthServiceRouting() {
        webClient.get()
                .uri("/api/auth/login")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Route", "auth-service");
    }

    @Test
    void testAllMicroservicesCovered() {
        // Verify comprehensive microservice coverage
        String[] paths = {"/api/users/**", "/api/tickets/**", "/api/rewards/**", 
                         "/api/knowledge/**", "/api/notifications/**", "/api/auth/**"};
        
        for (String path : paths) {
            webClient.get()
                    .uri(path.replace("**", "test"))
                    .exchange()
                    .expectStatus().isOk();
        }
    }
}
