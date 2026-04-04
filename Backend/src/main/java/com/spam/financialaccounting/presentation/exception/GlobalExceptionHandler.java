package com.spam.financialaccounting.presentation.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.spam.financialaccounting.presentation.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private ErrorResponse buildResponse(HttpStatus status, String message, HttpServletRequest request) {
        return new ErrorResponse(
                LocalDateTime.now().toString(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI()
        );
    }

    @ExceptionHandler(FAGroupNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            FAGroupNotFoundException ex,
            HttpServletRequest request) {

        return new ResponseEntity<>(
                buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request),
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(FAGroupValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            FAGroupValidationException ex,
            HttpServletRequest request) {

        return new ResponseEntity<>(
                buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(FAGroupAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleConflict(
            FAGroupAlreadyExistsException ex,
            HttpServletRequest request) {

        return new ResponseEntity<>(
                buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex,
            HttpServletRequest request) {

        return new ResponseEntity<>(
                buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request),
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(
            Exception ex,
            HttpServletRequest request) {

        return new ResponseEntity<>(
                buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred", request),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }    

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationExceptions(
                MethodArgumentNotValidException ex,
                HttpServletRequest request) {

                        String errorMessage = ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(err -> err.getField() + ": " +err.getDefaultMessage())
                        .findFirst()
                        .orElse("Validation error");
                
                return new ResponseEntity<>(buildResponse(HttpStatus.BAD_REQUEST, errorMessage, request), HttpStatus.BAD_REQUEST);
        }
}
