package com.example.api_gateway.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebFluxTest(DashboardController.class)
class DashboardControllerTest {

    @Autowired
    private WebTestClient webClient;

    @MockBean
    private WebClient webClientMock;

    @Test
    void dashboard_allServicesRespond() {
        // Mock all downstream services
        when(webClientMock.get().uri(anyString()).retrieve().bodyToMono(Map.class))
                .thenReturn(Mono.just(Map.of(
                    "activeUsers", 150,
                    "status", "healthy"
                )));

        webClient.get()
                .uri("/api/dashboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .value(dashboard -> {
                    assertThat(dashboard).containsKey("userService");
                    assertThat(dashboard).containsKey("ticketService");  
                    assertThat(dashboard).containsKey("rewardService");
                    assertThat(dashboard).containsKey("notificationService");
                    assertThat(dashboard).containsKey("timestamp");
                });
    }

    @Test
    void dashboard_oneServiceDown_returnsPartialWithError() {
        // Mock one service failure
        when(webClientMock.get().uri(contains("ticket")).retrieve().bodyToMono(Map.class))
                .thenReturn(Mono.error(new RuntimeException("Service down")));

        when(webClientMock.get().uri(contains("user")).retrieve().bodyToMono(Map.class))
                .thenReturn(Mono.just(Map.of("activeUsers", 100)));

        when(webClientMock.get().uri(contains("reward")).retrieve().bodyToMono(Map.class))
                .thenReturn(Mono.just(Map.of("totalRewards", 50)));

        when(webClientMock.get().uri(contains("notification")).retrieve().bodyToMono(Map.class))
                .thenReturn(Mono.just(Map.of("pending", 20)));

        webClient.get()
                .uri("/api/dashboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody(Map.class)
                .value(dashboard -> {
                    // Graceful degradation
                    assertThat((Map)dashboard.get("ticketService")).containsEntry("error", "Service unavailable");
                    assertThat(dashboard.get("userService")).isNotNull();
                });
    }

    @Test
    void dashboard_timeout_returnsErrorResponses() {
        // Mock timeout
        when(webClientMock.get().anyExchange()).thenReturn(mockRequestWithTimeout());

        webClient.get()
                .uri("/api/dashboard")
                .exchange()
                .expectStatus().isOk() // Graceful timeout handling
                .expectBody(Map.class)
                .value(dashboard -> {
                    // All services show error
                    assertThat(dashboard.get("userService")).containsEntry("error", "Service unavailable");
                    assertThat(dashboard.get("ticketService")).containsEntry("error", "Service unavailable");
                });
    }

    private WebClient.RequestHeadersUriSpec mockRequestWithTimeout() {
        WebClient.RequestHeadersUriSpec spec = mock(WebClient.RequestHeadersUriSpec.class);
        WebClient.ResponseSpec response = mock(WebClient.ResponseSpec.class);
        when(response.bodyToMono(Map.class)).thenReturn(Mono.delay(Duration.ofSeconds(10)).then(Mono.empty()));
        when(spec.retrieve()).thenReturn(response);
        return spec;
    }
}
