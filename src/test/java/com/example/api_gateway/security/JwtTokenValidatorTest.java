package com.example.api_gateway.security;

import com.example.api_gateway.util.JwtUtilFixed;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtTokenValidatorTest {

    @InjectMocks
    private JwtTokenValidator validator;

    private final String secret = "mySecretKeyForSigningJWTs12345678901234567890";
    private final String validToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMSIsInJvbGVzIjpbIkFETUlOIiwiRU5HSU5FRVIiXSwiZXhwIjoxNzAwMDAwMDAwfQ.fake.signature.here";
    private final String expiredToken = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ1c2VyMSIsImV4cCI6MTQ3OTYxMjE1Mn0.signature";
    private final String invalidToken = "eyJhbGciOiJIUzI1NiJ9.invalid.payload.signature";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(validator, "secret", secret);
    }

    @Test
    void validateToken_validToken_returnsClaims() {
        Claims claims = validator.validateToken(validToken);
        
        assertNotNull(claims);
        assertEquals("user1", claims.getSubject());
        assertNotNull(claims.get("roles"));
    }

    @Test
    void validateToken_expiredToken_throwsJwtException() {
        JwtException exception = assertThrows(JwtException.class, () -> {
            validator.validateToken(expiredToken);
        });
        
        assertTrue(exception.getMessage().contains("Token expired"));
    }

    @Test
    void validateToken_invalidSignature_throwsJwtException() {
        JwtException exception = assertThrows(JwtException.class, () -> {
            validator.validateToken(invalidToken);
        });
        
        assertTrue(exception.getMessage().contains("Invalid JWT token"));
    }

    @Test
    void isValidToken_valid_returnsTrue() {
        assertTrue(validator.isValidToken(validToken));
    }

    @Test
    void isValidToken_invalid_returnsFalse() {
        assertFalse(validator.isValidToken(invalidToken));
    }

    @Test
    void extractUserId_validClaims_returnsSubject() {
        Claims claims = validator.validateToken(validToken);
        assertEquals("user1", validator.extractUserId(claims));
    }

    @Test
    void extractRoles_validClaims_returnsRolesList() {
        Claims claims = validator.validateToken(validToken);
        assertNotNull(validator.extractRoles(claims));
        assertEquals(2, validator.extractRoles(claims).size());
    }
}
