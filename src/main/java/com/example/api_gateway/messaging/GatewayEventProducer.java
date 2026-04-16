package com.example.api_gateway.messaging;

import com.example.api_gateway.util.TraceIdGenerator;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class GatewayEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.api-request:api.request.logged}")
    private String apiRequestTopic;

    @Value("${kafka.topics.security-violation:security.violation}")
    private String securityViolationTopic;

    public GatewayEventProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void sendApiRequestLogged(String path, String method, long latencyMs, String traceId) {
        ApiRequestEvent event = new ApiRequestEvent(path, method, latencyMs, traceId);
        kafkaTemplate.send(apiRequestTopic, event.getTraceId(), event);
        log.debug("Sent API request event: {}", event);
    }

    public void sendSecurityViolation(String path, String ip, String reason, String traceId) {
        SecurityViolationEvent event = new SecurityViolationEvent(path, ip, reason, traceId);
        kafkaTemplate.send(securityViolationTopic, event.getTraceId(), event);
        log.warn("Sent security violation event: {}", event);
    }

    @Data
    public static class ApiRequestEvent {
        private final String eventType = "api.request.logged";
        private final String path;
        private final String method;
        private final long latencyMs;
        private final String traceId;
        private final long timestamp = System.currentTimeMillis();
    }

    @Data
    public static class SecurityViolationEvent {
        private final String eventType = "security.violation";
        private final String path;
        private final String ip;
        private final String reason;
        private final String traceId;
        private final long timestamp = System.currentTimeMillis();
    }
}
