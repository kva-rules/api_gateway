package com.example.api_gateway.security;

import com.example.api_gateway.util.JwtUtilFixed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthenticationFilterTest {

    @InjectMocks
    private AuthenticationFilter filter;

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

    private final String validToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMSJ9.signature";
    private final String invalidToken = "invalid.token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "jwtTokenValidator", jwtTokenValidator);
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        when(chain.filter(any())).thenReturn(Mono.empty());
    }

    @Test
    void filter_validToken_addsHeadersAndForwards() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + validToken);
        when(request.getHeaders()).thenReturn(headers);
        when(jwtTokenValidator.validateToken(validToken)).thenReturn(mock(Claims.class));
        when(jwtTokenValidator.extractUserId(any())).thenReturn("user1");
        when(jwtTokenValidator.extractRoles(any())).thenReturn(List.of("ADMIN"));

        // When
        Mono<Void> result = filter.apply(AbstractGatewayFilterFactory.Config.class).filter(exchange, chain);

        // Then
        verify(request).mutate();
        verify(chain).filter(any(ServerWebExchange.class));
        verify(response, never()).setStatusCode(any());
    }

    @Test
    void filter_missingToken_returnsUnauthorized() {
        // Given
        when(request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).thenReturn(null);

        // When
        Mono<Void> result = filter.apply(AbstractGatewayFilterFactory.Config.class).filter(exchange, chain);

        // Then
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_invalidToken_returnsUnauthorized() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken);
        when(request.getHeaders()).thenReturn(headers);
        when(jwtTokenValidator.validateToken(invalidToken)).thenThrow(new JwtException("Invalid token"));

        // When
        Mono<Void> result = filter.apply(AbstractGatewayFilterFactory.Config.class).filter(exchange, chain);

        // Then
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void filter_malformedBearer_returnsUnauthorized() {
        // Given
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Malformed " + validToken);
        when(request.getHeaders()).thenReturn(headers);

        // When
        Mono<Void> result = filter.apply(AbstractGatewayFilterFactory.Config.class).filter(exchange, chain);

        // Then
        verify(response).setStatusCode(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }
}
