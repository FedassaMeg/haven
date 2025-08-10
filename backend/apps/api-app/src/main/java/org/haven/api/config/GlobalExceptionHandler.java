package org.haven.api.config;

import org.haven.clientprofile.domain.ClientDomainService.ClientDuplicationException;
import org.haven.casemgmt.domain.CaseDomainService.CaseDuplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.validation.FieldError;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        var error = new ErrorResponse(
            "INVALID_REQUEST",
            ex.getMessage(),
            HttpStatus.BAD_REQUEST.value(),
            Instant.now()
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        var error = new ErrorResponse(
            "INVALID_STATE",
            ex.getMessage(),
            HttpStatus.CONFLICT.value(),
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler({ClientDuplicationException.class, CaseDuplicationException.class})
    public ResponseEntity<ErrorResponse> handleDuplication(RuntimeException ex) {
        var error = new ErrorResponse(
            "DUPLICATE_RESOURCE",
            ex.getMessage(),
            HttpStatus.CONFLICT.value(),
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        var errorResponse = new ValidationErrorResponse(
            "VALIDATION_FAILED",
            "Request validation failed",
            HttpStatus.BAD_REQUEST.value(),
            Instant.now(),
            errors
        );
        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex) {
        var error = new ErrorResponse(
            "INTERNAL_ERROR",
            "An internal error occurred",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            Instant.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    public record ErrorResponse(
        String code,
        String message,
        int status,
        Instant timestamp
    ) {}

    public record ValidationErrorResponse(
        String code,
        String message,
        int status,
        Instant timestamp,
        Map<String, String> fieldErrors
    ) {}
}