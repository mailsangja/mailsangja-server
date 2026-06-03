package com.mailsangja.core.common.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CorrelationIdFilter extends OncePerRequestFilter {

    private final ObservabilitySupport observabilitySupport;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String workId = resolveWorkId(request);
        response.setHeader(ObservabilitySupport.CORRELATION_ID_HEADER, workId);

        try (ObservabilitySupport.Scope ignored = observabilitySupport.openScope(Map.of(
                ObservabilitySupport.WORK_ID, workId
        ))) {
            filterChain.doFilter(request, response);
        }
    }

    private String resolveWorkId(HttpServletRequest request) {
        String correlationId = request.getHeader(ObservabilitySupport.CORRELATION_ID_HEADER);
        if (!isBlank(correlationId)) {
            return correlationId.trim();
        }

        String currentWorkId = MDC.get(ObservabilitySupport.WORK_ID);
        if (!isBlank(currentWorkId)) {
            return currentWorkId;
        }

        return UUID.randomUUID().toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
