# Spring Boot API Gateway Dockerfile
# Multi-stage build for optimal image size

# Build stage
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY . .
COPY pom.xml .
# Download dependencies (Maven wrapper)
RUN ./mvnw dependency:go-offline -B
# Build executable jar
RUN ./mvnw clean package -DskipTests -Dspring-boot.repackage.skip=false

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
# Copy jar from build stage
COPY --from=build /app/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

# Healthcheck
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
    CMD curl -f http://localhost:8080/gateway/health || exit 1

# Expose port
EXPOSE 8080

# Run
ENTRYPOINT ["java", "-jar", "app.jar"]
