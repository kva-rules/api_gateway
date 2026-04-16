package com.example.api_gateway.util;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Component
public class TraceIdGenerator {

    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    /**
     * Generate new trace ID or reuse existing one from header
     */
    public String generateOrReuseTraceId(String existingTraceId) {
        if (StringUtils.hasText(existingTraceId)) {
            return existingTraceId;
        }
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * Extract trace ID from headers
     */
    public String extractTraceIdFromHeaders(org.springframework.http.server.reactive.ServerHttpRequest request) {
        java.util.List<String> traceIds = request.getHeaders().get(TRACE_ID_HEADER);
        return traceIds != null && !traceIds.isEmpty() ? traceIds.get(0) : null;
    }

    /**
     * Create new unique trace ID
     */
    public String generateNewTraceId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

    /**
     * Validate trace ID format
     */
    public boolean isValidTraceId(String traceId) {
        return traceId != null && traceId.matches("^[a-f0-9]{32}$");
    }
}
