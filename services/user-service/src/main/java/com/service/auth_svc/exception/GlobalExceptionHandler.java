package com.service.auth_svc.exception;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * Global exception handler for the authentication service.
 * 
 * <p>Centralizes exception handling across all controllers, providing consistent
 * error responses with standardized structure. All error responses include:</p>
 * <ul>
 *   <li><b>timestamp:</b> ISO-8601 formatted timestamp of when the error occurred</li>
 *   <li><b>status:</b> HTTP status code (e.g., 400, 401, 404, 500)</li>
 *   <li><b>error:</b> Human-readable error message</li>
 *   <li><b>code:</b> Application-specific error code for client-side handling</li>
 * </ul>
 * 
 * <p><b>Error Categories:</b></p>
 * <ul>
 *   <li><b>Authentication Errors (401):</b> Invalid credentials, expired tokens, signature validation</li>
 *   <li><b>Authorization Errors (403):</b> Insufficient permissions</li>
 *   <li><b>Validation Errors (400):</b> Invalid input, type mismatches, missing parameters</li>
 *   <li><b>Resource Errors (404):</b> User not found, endpoint not found</li>
 *   <li><b>Conflict Errors (409):</b> Duplicate resources, constraint violations</li>
 *   <li><b>Server Errors (500):</b> Unexpected runtime exceptions</li>
 * </ul>
 * 
 * @author APIBP Team
 * @version 1.0
 * @since 2025-11-26
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles custom application exceptions with specific HTTP status codes.
     * 
     * @param ex the custom exception thrown
     * @return ResponseEntity with error details and appropriate HTTP status
     */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Map<String, Object>> handleCustomException(CustomException ex) {
        log.warn("CustomException: {} (status={})", ex.getMessage(), ex.getStatus());
        return buildErrorResponse(
                ex.getMessage(),
                ex.getStatus(),
                "CUSTOM_ERROR"
        );
    }

    /**
     * Handles user registration with duplicate email (HTTP 409 Conflict).
     * 
     * @param ex the user already exists exception
     * @return ResponseEntity with error details and HTTP 409 status
     */
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        log.warn("User registration failed: {}", ex.getMessage());
        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.CONFLICT,
                "USER_ALREADY_EXISTS"
        );
    }

    /**
     * Handles attempts to access non-existent users (HTTP 404 Not Found).
     * 
     * @param ex the user not found exception
     * @return ResponseEntity with error details and HTTP 404 status
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFound(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.NOT_FOUND,
                "USER_NOT_FOUND"
        );
    }

    /**
     * Handles login attempts with incorrect email or password (HTTP 401 Unauthorized).
     * 
     * @param ex the invalid credentials exception
     * @return ResponseEntity with error details and HTTP 401 status
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        log.warn("Invalid credentials attempt");
        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS"
        );
    }

    /**
     * Handles registration with invalid role specification (HTTP 400 Bad Request).
     * 
     * @param ex the invalid role exception
     * @return ResponseEntity with error details and HTTP 400 status
     */
    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRole(InvalidRoleException ex) {
        log.warn("Invalid role specified: {}", ex.getMessage());
        return buildErrorResponse(
                ex.getMessage(),
                HttpStatus.BAD_REQUEST,
                "INVALID_ROLE"
        );
    }

    /**
     * Handles Bean Validation (@Valid) failures with field-level error details (HTTP 400 Bad Request).
     * 
     * <p>Returns a map of field names to error messages, helping clients identify
     * exactly which fields failed validation and why.</p>
     * 
     * @param ex the method argument validation exception
     * @return ResponseEntity with field errors and HTTP 400 status
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation failed: {} field errors", ex.getBindingResult().getErrorCount());

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> {
            log.debug("Invalid field '{}': {}", error.getField(), error.getDefaultMessage());
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        });

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("code", "VALIDATION_ERROR");
        response.put("fieldErrors", fieldErrors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles generic Spring Security authentication failures (HTTP 401 Unauthorized).
     * 
     * @param ex the authentication exception from Spring Security
     * @return ResponseEntity with error details and HTTP 401 status
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Map<String, Object>> handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return buildErrorResponse(
                "Authentication failed: " + ex.getMessage(),
                HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_FAILED"
        );
    }

    /**
     * Handles Spring Security bad credentials during authentication (HTTP 401 Unauthorized).
     * 
     * @param ex the bad credentials exception from Spring Security
     * @return ResponseEntity with error details and HTTP 401 status
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials provided");
        return buildErrorResponse(
                "Invalid email or password",
                HttpStatus.UNAUTHORIZED,
                "INVALID_CREDENTIALS"
        );
    }

    /**
     * Handles access denied errors when user lacks required permissions (HTTP 403 Forbidden).
     * 
     * @param ex the access denied exception from Spring Security
     * @return ResponseEntity with error details and HTTP 403 status
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return buildErrorResponse(
                "You don't have permission to access this resource",
                HttpStatus.FORBIDDEN,
                "ACCESS_DENIED"
        );
    }

    /**
     * Handles JWT tokens with invalid signatures (HTTP 401 Unauthorized).
     * 
     * <p>This occurs when a token has been tampered with or signed with a different key.</p>
     * 
     * @param ex the signature exception from JWT library
     * @return ResponseEntity with error details and HTTP 401 status
     */
    @ExceptionHandler(SignatureException.class)
    public ResponseEntity<Map<String, Object>> handleJwtSignatureException(SignatureException ex) {
        log.warn("Invalid JWT signature: {}", ex.getMessage());
        return buildErrorResponse(
                "Invalid token signature",
                HttpStatus.UNAUTHORIZED,
                "INVALID_TOKEN_SIGNATURE"
        );
    }

    /**
     * Handles expired JWT access tokens (HTTP 401 Unauthorized).
     * 
     * <p>Clients should use their refresh token to obtain a new access token.</p>
     * 
     * @param ex the expired JWT exception from JWT library
     * @return ResponseEntity with error details and HTTP 401 status
     */
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<Map<String, Object>> handleExpiredJwt(ExpiredJwtException ex) {
        log.warn("JWT token expired");
        return buildErrorResponse(
                "Token has expired. Please login again",
                HttpStatus.UNAUTHORIZED,
                "TOKEN_EXPIRED"
        );
    }

    /**
     * Handles malformed JWT tokens with invalid structure (HTTP 401 Unauthorized).
     * 
     * <p>This occurs when the token is not properly formatted (missing parts, invalid encoding, etc.).</p>
     * 
     * @param ex the malformed JWT exception from JWT library
     * @return ResponseEntity with error details and HTTP 401 status
     */
    @ExceptionHandler(MalformedJwtException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJwt(MalformedJwtException ex) {
        log.warn("Malformed JWT token: {}", ex.getMessage());
        return buildErrorResponse(
                "Invalid token format",
                HttpStatus.UNAUTHORIZED,
                "MALFORMED_TOKEN"
        );
    }

    /**
     * Handles database constraint violations like unique key violations (HTTP 409 Conflict).
     * 
     * @param ex the data integrity violation exception from JPA/Hibernate
     * @return ResponseEntity with error details and HTTP 409 status
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Data integrity violation: {}", ex.getMessage());

        String message = "Database constraint violation";
        if (ex.getMessage() != null && ex.getMessage().contains("unique")) {
            message = "A record with this value already exists";
        }

        return buildErrorResponse(
                message,
                HttpStatus.CONFLICT,
                "DATA_INTEGRITY_VIOLATION"
        );
    }

    /**
     * Handles missing required query or path parameters (HTTP 400 Bad Request).
     * 
     * @param ex the missing parameter exception from Spring MVC
     * @return ResponseEntity with error details and HTTP 400 status
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParams(MissingServletRequestParameterException ex) {
        log.warn("Missing required parameter: {}", ex.getParameterName());
        return buildErrorResponse(
                "Missing required parameter: " + ex.getParameterName(),
                HttpStatus.BAD_REQUEST,
                "MISSING_PARAMETER"
        );
    }

    /**
     * Handles type mismatches in parameters (e.g., passing string when number expected) (HTTP 400 Bad Request).
     * 
     * @param ex the type mismatch exception from Spring MVC
     * @return ResponseEntity with error details and HTTP 400 status
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("Type mismatch for parameter: {}", ex.getName());
        String message = String.format(
                "Invalid value for parameter '%s'. Expected type: %s",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );
        return buildErrorResponse(message, HttpStatus.BAD_REQUEST, "TYPE_MISMATCH");
    }

    /**
     * Handles malformed or unparseable JSON in request body (HTTP 400 Bad Request).
     * 
     * @param ex the HTTP message not readable exception from Spring MVC
     * @return ResponseEntity with error details and HTTP 400 status
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        return buildErrorResponse(
                "Malformed JSON request body",
                HttpStatus.BAD_REQUEST,
                "MALFORMED_JSON"
        );
    }

    /**
     * Handles unsupported HTTP methods for endpoints (HTTP 405 Method Not Allowed).
     * 
     * @param ex the method not supported exception from Spring MVC
     * @return ResponseEntity with error details and HTTP 405 status
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("HTTP method not supported: {}", ex.getMethod());
        String message = String.format(
                "HTTP method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(),
                ex.getSupportedHttpMethods()
        );
        return buildErrorResponse(message, HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED");
    }

    /**
     * Handles requests to non-existent endpoints (HTTP 404 Not Found).
     * 
     * @param ex the no handler found exception from Spring MVC
     * @return ResponseEntity with error details and HTTP 404 status
     */
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoHandlerFound(NoHandlerFoundException ex) {
        log.warn("No handler found for: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        return buildErrorResponse(
                "Endpoint not found: " + ex.getRequestURL(),
                HttpStatus.NOT_FOUND,
                "ENDPOINT_NOT_FOUND"
        );
    }

    /**
     * Handles Bean Validation constraint violations at method level (HTTP 400 Bad Request).
     * 
     * @param ex the constraint violation exception from Bean Validation
     * @return ResponseEntity with violation details and HTTP 400 status
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        log.warn("Constraint violation: {} violations", ex.getConstraintViolations().size());

        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        violation -> violation.getMessage()
                ));

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Validation Failed");
        response.put("code", "CONSTRAINT_VIOLATION");
        response.put("violations", violations);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles illegal argument exceptions from application logic (HTTP 400 Bad Request).
     * 
     * @param ex the illegal argument exception
     * @return ResponseEntity with error details and HTTP 400 status
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return buildErrorResponse(
                ex.getMessage() != null ? ex.getMessage() : "Invalid argument provided",
                HttpStatus.BAD_REQUEST,
                "ILLEGAL_ARGUMENT"
        );
    }

    /**
     * Handles illegal state exceptions indicating service misconfiguration (HTTP 500 Internal Server Error).
     * 
     * @param ex the illegal state exception
     * @return ResponseEntity with error details and HTTP 500 status
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException ex) {
        log.error("Illegal state: {}", ex.getMessage());
        return buildErrorResponse(
                "Service is in an invalid state: " + ex.getMessage(),
                HttpStatus.INTERNAL_SERVER_ERROR,
                "ILLEGAL_STATE"
        );
    }

    /**
     * Handles unhandled runtime exceptions (HTTP 500 Internal Server Error).
     * 
     * <p>Acts as a catch-all for runtime exceptions not handled by more specific handlers.</p>
     * 
     * @param ex the runtime exception
     * @return ResponseEntity with error details and HTTP 500 status
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException ex) {
        log.error("Runtime exception occurred: ", ex);
        return buildErrorResponse(
                ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "RUNTIME_ERROR"
        );
    }

    /**
     * Handles all unhandled exceptions as final fallback (HTTP 500 Internal Server Error).
     * 
     * <p>This is the last resort handler for any exception not caught by more specific handlers.
     * Prevents exposing internal error details to clients while logging full stack traces.</p>
     * 
     * @param ex the exception
     * @return ResponseEntity with generic error message and HTTP 500 status
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unhandled exception occurred: ", ex);
        return buildErrorResponse(
                "An unexpected error occurred. Please try again later",
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR"
        );
    }

    /**
     * Builds a standardized error response with consistent structure.
     * 
     * <p>All error responses follow this format:</p>
     * <pre>
     * {
     *   "timestamp": "2025-11-26T10:30:45.123",
     *   "status": 400,
     *   "error": "Bad Request",
     *   "message": "Detailed error message",
     *   "code": "ERROR_CODE"
     * }
     * </pre>
     * 
     * @param message human-readable error message
     * @param status HTTP status code
     * @param errorCode application-specific error code for client-side handling
     * @return ResponseEntity with standardized error response
     */
    private ResponseEntity<Map<String, Object>> buildErrorResponse(String message, HttpStatus status,
                                                                   String errorCode) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now().toString());
        response.put("status", status.value());
        response.put("error", status.getReasonPhrase());
        response.put("message", message);
        response.put("code", errorCode);
        return new ResponseEntity<>(response, status);
    }
}
