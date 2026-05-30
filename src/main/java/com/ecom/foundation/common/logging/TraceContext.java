package com.ecom.foundation.common.logging;

import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class TraceContext {

    private TraceContext() {
    }

    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    public static void put(String key, String value) {
        if (isNotBlank(key) && isNotBlank(value)) {
            MDC.put(key, value);
        }
    }

    public static void putAll(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return;
        }

        values.forEach(TraceContext::put);
    }

    public static String valueOrGenerate(String value) {
        return isNotBlank(value) ? value : generateId();
    }

    public static String valueOrDefault(String value, String defaultValue) {
        return isNotBlank(value) ? value : defaultValue;
    }

    public static Map<String, String> copy() {
        Map<String, String> context = MDC.getCopyOfContextMap();
        return context == null ? new HashMap<>() : new HashMap<>(context);
    }

    public static void restore(Map<String, String> contextMap) {
        clear();

        if (contextMap == null || contextMap.isEmpty()) {
            return;
        }

        MDC.setContextMap(contextMap);
    }

    public static void clear() {
        MDC.clear();
    }

    private static boolean isNotBlank(String value) {
        return Objects.nonNull(value) && !value.isBlank();
    }
}