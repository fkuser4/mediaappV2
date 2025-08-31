package com.tvz.mediaapp.backend.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;
import java.util.NoSuchElementException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String ERROR_KEY = "error";

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.warn("Conflict error: {}", ex.getMessage());
        return new ResponseEntity<>(Map.of(ERROR_KEY, ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    public ResponseEntity<Map<String, String>> handleAuthenticationException(Exception ex) {
        log.warn("Authentication error: {}", ex.getMessage());
        return new ResponseEntity<>(Map.of(ERROR_KEY, "Invalid credentials"), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, String>> handleNoSuchElementException(NoSuchElementException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(Map.of(ERROR_KEY, ex.getMessage()), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Bad request error: {}", ex.getMessage());
        return new ResponseEntity<>(Map.of(ERROR_KEY, ex.getMessage()), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGenericException(Exception ex) {
        log.error("An unexpected error occurred", ex);
        return new ResponseEntity<>(Map.of(ERROR_KEY, "An internal server error occurred."), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}