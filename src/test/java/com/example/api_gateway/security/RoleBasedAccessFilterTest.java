package com.example.api_gateway.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleBasedAccessFilterTest {

    @InjectMocks
    private RoleBasedAccessFilter filter;

    @Mock
    private JwtTokenValidator jwtTokenValidator;

    @Mock
    private ServerWebExchange exchange;

    @Mock
    private GatewayFilterChain chain;

    @Mock
    private ServerHttpRequest request;

    @Mock
    private ServerHttpResponse response;

    private final String validToken = "valid.token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "jwtTokenValidator", jwtTokenValidator);
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void filter_adminUser_usersPath_authorized() {
        // Given admin user with valid token
        setupRequest("/api/users/admin", List.of("ADMIN"));

        // When
        Mono<Void> result = filter.apply(RoleBasedAccessFilter.Config.class).filter(exchange, chain);

        // Then
        verify(chain).filter(any());
        verify(response, never()).setStatusCode(any());
    }

    @Test
    void filter_engineerUser_knowledgePath_authorized() {
        // Given engineer user
        setupRequest("/api/knowledge/docs", List.of("ENGINEER"));

        // When
        Mono<Void> result = filter.apply(RoleBasedAccessFilter.Config.class).filter(exchange, chain);

        // Then
        verify(chain).filter(any());
    }

    @Test
    void filter_engineerUser_usersPath_forbidden() {
        // Given engineer (no admin) trying users path
        setupRequest("/api/users/", List.of("ENGINEER"));

        // When
        Mono<Void> result = filter.apply(RoleBasedAccessFilter.Config.class).filter(exchange, chain);

        // Then
        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_userUser_ticketsPath_forbidden() {
        // Given regular user trying tickets
        setupRequest("/api/tickets/open", List.of("USER"));

        // When
        Mono<Void> result = filter.apply(RoleBasedAccessFilter.Config.class).filter(exchange, chain);

        // Then
        verify(response).setStatusCode(HttpStatus.FORBIDDEN);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_managerUser_rewardsPath_authorized() {
        // Given manager has multiple roles
        setupRequest("/api/rewards/list", List.of("ADMIN", "MANAGER"));

        // When
        Mono<Void> result = filter.apply(RoleBasedAccessFilter.Config.class).filter(exchange, chain);

        // Then
        verify(chain).filter(any());
    }

    @Test
    void filter_noAuthHeader_unauthorized() {
        // Given no auth header
        when(request.getHeaders().getFirst("Authorization")).thenReturn(null);

        // When
        Mono<Void> result = filter.apply(RoleBasedAccessFilter.Config.class).filter(exchange, chain);

        // Then
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
    }

    private void setupRequest(String path, List<String> roles) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", "Bearer " + validToken);
        headers.add("X-User-Roles", String.join(",", roles));
        when(request.getHeaders()).thenReturn(headers);
        when(request.getURI().getPath()).thenReturn(path);
        when(jwtTokenValidator.validateToken(validToken)).thenReturn(mock(io.jsonwebtoken.Claims.class));
        when(jwtTokenValidator.extractRoles(any())).thenReturn(roles);
    }
}
