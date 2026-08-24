package com.ecom.foundation.common.error;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public final class GlobalExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicationException(
            ApplicationException exception,
            HttpServletRequest request) {

        ErrorCode errorCode = exception.getErrorCode();

        logApplicationException(
                exception,
                errorCode,
                request
        );

        ApiErrorResponse response = createResponse(
                errorCode,
                exception.getResponseMessage(),
                request,
                Map.of()
        );

        return ResponseEntity
                .status(errorCode.status())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleBodyValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request) {

        Map<String, String> fieldErrors =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .collect(Collectors.toMap(
                                fieldError -> fieldError.getField(),
                                fieldError -> Objects.toString(
                                        fieldError.getDefaultMessage(),
                                        "Invalid value"
                                ),
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        ));

        log.info(
                "Request validation failed method={} route={} fields={}",
                request.getMethod(),
                resolveRoute(request),
                fieldErrors.keySet()
        );

        ApiErrorResponse response = createResponse(
                ErrorCode.VALIDATION_FAILED,
                ErrorCode.VALIDATION_FAILED.message(),
                request,
                fieldErrors
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception,
            HttpServletRequest request) {

        Map<String, String> fieldErrors =
                exception.getConstraintViolations()
                        .stream()
                        .collect(Collectors.toMap(
                                violation ->
                                        violation.getPropertyPath().toString(),
                                violation ->
                                        violation.getMessage(),
                                (first, ignored) -> first,
                                LinkedHashMap::new
                        ));

        log.info(
                "Request constraint validation failed method={} route={} fields={}",
                request.getMethod(),
                resolveRoute(request),
                fieldErrors.keySet()
        );

        ApiErrorResponse response = createResponse(
                ErrorCode.VALIDATION_FAILED,
                ErrorCode.VALIDATION_FAILED.message(),
                request,
                fieldErrors
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request) {

        String parameterName = Objects.toString(
                exception.getName(),
                "request"
        );

        log.info(
                "Request type mismatch method={} route={} parameter={}",
                request.getMethod(),
                resolveRoute(request),
                parameterName
        );

        ApiErrorResponse response = createResponse(
                ErrorCode.INVALID_REQUEST,
                ErrorCode.INVALID_REQUEST.message(),
                request,
                Map.of(
                        parameterName,
                        "Invalid value"
                )
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request) {

        log.info(
                "Malformed request body method={} route={}",
                request.getMethod(),
                resolveRoute(request)
        );

        ApiErrorResponse response = createResponse(
                ErrorCode.MALFORMED_REQUEST,
                ErrorCode.MALFORMED_REQUEST.message(),
                request,
                Map.of()
        );

        return ResponseEntity
                .badRequest()
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(
            Exception exception,
            HttpServletRequest request) {

        log.error(
                "Unhandled application exception method={} route={}",
                request.getMethod(),
                resolveRoute(request),
                exception
        );

        ApiErrorResponse response = createResponse(
                ErrorCode.INTERNAL_ERROR,
                ErrorCode.INTERNAL_ERROR.message(),
                request,
                Map.of()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private void logApplicationException(
            ApplicationException exception,
            ErrorCode errorCode,
            HttpServletRequest request) {

        HttpStatus status = errorCode.status();

        if (status.is5xxServerError()) {
            log.error(
                    "Application failure code={} status={} method={} route={}",
                    errorCode.name(),
                    status.value(),
                    request.getMethod(),
                    resolveRoute(request),
                    exception
            );

            return;
        }

        if (status == HttpStatus.UNAUTHORIZED
                || status == HttpStatus.FORBIDDEN
                || status == HttpStatus.TOO_MANY_REQUESTS) {

            log.warn(
                    "Security request rejected code={} status={} method={} route={}",
                    errorCode.name(),
                    status.value(),
                    request.getMethod(),
                    resolveRoute(request)
            );

            return;
        }

        log.info(
                "Application request rejected code={} status={} method={} route={}",
                errorCode.name(),
                status.value(),
                request.getMethod(),
                resolveRoute(request)
        );
    }

    private ApiErrorResponse createResponse(
            ErrorCode errorCode,
            String message,
            HttpServletRequest request,
            Map<String, String> fieldErrors) {

        return new ApiErrorResponse(
                Instant.now(),
                errorCode.status().value(),
                errorCode.name(),
                message,
                request.getRequestURI(),
                MDC.get("requestId"),
                MDC.get("correlationId"),
                fieldErrors
        );
    }

    private String resolveRoute(HttpServletRequest request) {

        Object routePattern = request.getAttribute(
                HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE
        );

        if (routePattern != null) {
            return routePattern.toString();
        }

        return request.getRequestURI();
    }
}