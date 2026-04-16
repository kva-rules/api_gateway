# API Gateway Spring Boot Project Implementation Plan

## Completed: 0/11

### 1. Create TODO.md ✅ (Current)

### 2. Update pom.xml ✅

- Java 17, Spring Boot 3.3.5, Cloud BOM 2023.0.3
- Added gateway, security, oauth2-resource-server, webflux, JWT (jjwt), reactor-test
- Removed JPA/Liquibase/Postgres/webmvc

### 3. Update main application.yaml ✅

- Added gateway routes (user-service, order-service placeholders)
- Security/JWT issuer config
- Server port 8080, cors

### 4. Rename/refactor main class to ApiGatewayApplication

### 4. Rename/refactor main class to ApiGatewayApplication

- Update package to com.example.api-gateway
- Add gateway annotations

### 5. Update tests

### 6. Create SecurityConfig.java ✅ (JWT filter)

### 7. Create JwtUtil.java ✅ (token utilities)

### 8. Create GatewayConfig.java ✅ (custom routes/filters)

### 9. Clean up unused dirs ✅ (db/, templates/, static/)

### 10. Execute mvn clean compile ✅

### 11. Test startup: ./mvnw spring-boot:run ✅ (server starting on port 8080)

**Next step: Update pom.xml**
