package com.ecom.foundation.common.logging;

import java.io.IOException;
import java.util.Map;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class HttpTraceContextFilter extends OncePerRequestFilter {
    private static final String TRACE_TYPE_HTTP = "HTTP";

    private static final String HEADER_REQUEST_ID = "X-Request-Id";
    private static final String HEADER_CORRELATION_ID = "X-Correlation-Id";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response, 
        FilterChain filter
    ) throws ServletException, IOException {
        String requestId = TraceContext.valueOrGenerate(request.getHeader(HEADER_REQUEST_ID));

        String correlationId = TraceContext.valueOrDefault(request.getHeader(HEADER_CORRELATION_ID), requestId);
        try {
            TraceContext.putAll(Map.of(
                TraceContextKeys.TRACE_TYPE, TRACE_TYPE_HTTP,
                TraceContextKeys.HTTP_METHOD, request.getMethod(),
                TraceContextKeys.REQUEST_URI, request.getRequestURI(),
                TraceContextKeys.REQUEST_ID, requestId,
                TraceContextKeys.CORRELATION_ID, correlationId
            ));
            
            response.setHeader(HEADER_REQUEST_ID, requestId);
            response.setHeader(HEADER_CORRELATION_ID, correlationId);

              filter.doFilter(request, response);
        } finally {
            TraceContext.clear();
        }
    }
}
