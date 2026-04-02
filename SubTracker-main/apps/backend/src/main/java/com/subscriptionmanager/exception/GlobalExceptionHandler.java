package com.subscriptionmanager.exception;

import com.subscriptionmanager.importing.exception.ImportConsentRequiredException;
import com.subscriptionmanager.importing.exception.MailboxConnectionRequiredException;
import com.subscriptionmanager.importing.exception.MailboxReauthRequiredException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ImportConsentRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleImportConsentRequired(ImportConsentRequiredException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", ImportConsentRequiredException.ERROR_CODE);
        body.put("message", ex.getMessage());
        body.put("provider", ex.getProvider().name());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(MailboxConnectionRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleMailboxConnectionRequired(MailboxConnectionRequiredException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", MailboxConnectionRequiredException.ERROR_CODE);
        body.put("message", ex.getMessage());
        body.put("provider", ex.getProvider().name());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(MailboxReauthRequiredException.class)
    public ResponseEntity<Map<String, Object>> handleMailboxReauthRequired(MailboxReauthRequiredException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", MailboxReauthRequiredException.ERROR_CODE);
        body.put("message", ex.getMessage());
        body.put("provider", ex.getProvider().name());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, String>> handleApiException(ApiException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(SubscriptionNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleSubscriptionNotFound(SubscriptionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(SubscriptionOwnershipException.class)
    public ResponseEntity<Map<String, String>> handleSubscriptionOwnership(SubscriptionOwnershipException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", ex.getMessage()));
    }

    @ExceptionHandler(LoginThrottledException.class)
    public ResponseEntity<Map<String, String>> handleLoginThrottled(LoginThrottledException ex) {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.RETRY_AFTER, String.valueOf(ex.getRetryAfterSeconds()));
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .headers(headers)
                .body(Map.of("message", "Too many login attempts. Please try again later."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        Map<String, Object> body = new HashMap<>();
        body.put("message", "Validation failed");
        body.put("errors", errors);
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, String>> handleArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String parameterName = ex.getName() == null ? "parameter" : ex.getName();
        return ResponseEntity.badRequest()
                .body(Map.of("message", "Invalid value for parameter: " + parameterName));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Malformed request body"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, String>> handleMissingRequestParameter(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", "Missing required parameter: " + ex.getParameterName()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, String>> handleAuthentication(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid credentials"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleUnknown(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Unexpected error"));
    }
}
