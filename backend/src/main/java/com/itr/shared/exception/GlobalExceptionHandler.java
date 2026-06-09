package com.itr.shared.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * GlobalExceptionHandler — maps all domain/system exceptions to HTTP responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        logger.warn("Bad credentials: {}", ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        logger.warn("Illegal argument: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_INPUT", ex.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntime(RuntimeException ex) {
        logger.error("Runtime exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", ex.getMessage());
    }

    @ExceptionHandler(TaxValidationException.class)
    public ResponseEntity<Map<String, Object>> handleTaxValidation(TaxValidationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "TAX_VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(CBDTValidationException.class)
    public ResponseEntity<Map<String, Object>> handleCBDTValidation(CBDTValidationException ex) {
        return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, "CBDT_SCHEMA_ERROR", ex.getMessage());
    }

    @ExceptionHandler(FilingException.class)
    public ResponseEntity<Map<String, Object>> handleFiling(FilingException ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "FILING_ERROR", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception ex) {
        logger.error("Unhandled exception: {}", ex.getMessage(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
            ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred. Please contact support.");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String code, String message) {
        return ResponseEntity.status(status).body(Map.of(
            "error", code,
            "message", message != null ? message : "Unknown error",
            "timestamp", LocalDateTime.now().toString()
        ));
    }
}
