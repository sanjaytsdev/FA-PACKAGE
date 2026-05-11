package com.spam.financialaccounting.presentation.exception;

import java.time.LocalDateTime;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import com.spam.financialaccounting.presentation.dto.ErrorResponse;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fagroup.FAGroupValidationException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupNotFoundException;
import com.spam.financialaccounting.presentation.exception.fasubgroup.FASubGroupValidationException;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailNotFoundException;
import com.spam.financialaccounting.presentation.exception.journaldetail.JournalDetailValidationException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterAlreadyExistsException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterNotFoundException;
import com.spam.financialaccounting.presentation.exception.journalmaster.JournalMasterValidationException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private ErrorResponse buildResponse(HttpStatus status, String message, HttpServletRequest request) {
                return new ErrorResponse(
                                LocalDateTime.now().toString(),
                                status.value(),
                                status.getReasonPhrase(),
                                message,
                                request.getRequestURI());
        }

        // ---FAGroup Handlers---
        @ExceptionHandler(FAGroupNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleNotFound(
                        FAGroupNotFoundException ex,
                        HttpServletRequest request) {

                return new ResponseEntity<>(
                                buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request),
                                HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(FAGroupValidationException.class)
        public ResponseEntity<ErrorResponse> handleValidation(
                        FAGroupValidationException ex,
                        HttpServletRequest request) {

                return new ResponseEntity<>(
                                buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request),
                                HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(FAGroupAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleConflict(
                        FAGroupAlreadyExistsException ex,
                        HttpServletRequest request) {

                return new ResponseEntity<>(
                                buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request),
                                HttpStatus.CONFLICT);
        }

        // ---FASubGroup Handlers---
        @ExceptionHandler(FASubGroupNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleSubGroupNotFound(
                        FASubGroupNotFoundException ex, HttpServletRequest request) {
                return new ResponseEntity<>(buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request),
                                HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(FASubGroupValidationException.class)
        public ResponseEntity<ErrorResponse> handleSubGroupValidation(
                        FASubGroupValidationException ex,
                        HttpServletRequest request) {
                return new ResponseEntity<>(
                                buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request),
                                HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(FASubGroupAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleSubGroupConflict(
                        FASubGroupAlreadyExistsException ex,
                        HttpServletRequest request) {
                return new ResponseEntity<>(
                                buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request),
                                HttpStatus.CONFLICT);
        }

        // ----JournalDetail Handler---
        @ExceptionHandler(JournalDetailNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleJournalDetailNotFound(
                        JournalDetailNotFoundException ex, HttpServletRequest request) {
                return new ResponseEntity<>(buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request),
                                HttpStatus.NOT_FOUND);
        }

        @ExceptionHandler(JournalDetailAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleJournalAlreadyExist(
                        JournalDetailAlreadyExistsException ex, HttpServletRequest request) {
                return new ResponseEntity<>(buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request),
                                HttpStatus.CONFLICT);
        }

        @ExceptionHandler(JournalDetailValidationException.class)
        public ResponseEntity<ErrorResponse> handleJournalValidation(
                        JournalDetailValidationException ex, HttpServletRequest request) {
                return new ResponseEntity<>(buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request),
                                HttpStatus.BAD_REQUEST);
        }

        // ---JournalMaster Handler ---
        @ExceptionHandler(JournalMasterNotFoundException.class)
        public ResponseEntity<ErrorResponse> handleJournalMasterNotFound(
                        JournalMasterNotFoundException ex, HttpServletRequest request) {
                return new ResponseEntity<>(buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request),
                                HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(JournalMasterValidationException.class)
        public ResponseEntity<ErrorResponse> handleJournalMasterValidation(
                        JournalMasterValidationException ex, HttpServletRequest request) {
                return new ResponseEntity<>(buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request),
                                HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(JournalMasterAlreadyExistsException.class)
        public ResponseEntity<ErrorResponse> handleJournalMasterAlreadyExistsException(
                        JournalMasterAlreadyExistsException ex, HttpServletRequest request) {
                return new ResponseEntity<>(buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request),
                                HttpStatus.BAD_REQUEST);
        }

        // --- Generic Handlers ---
        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex,
                        HttpServletRequest request) {

                return new ResponseEntity<>(
                                buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request),
                                HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(
                        Exception ex,
                        HttpServletRequest request) {

                return new ResponseEntity<>(
                                buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error occurred", request),
                                HttpStatus.INTERNAL_SERVER_ERROR);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidationExceptions(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {

                String errorMessage = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                                .findFirst()
                                .orElse("Validation error");

                return new ResponseEntity<>(buildResponse(HttpStatus.BAD_REQUEST, errorMessage, request),
                                HttpStatus.BAD_REQUEST);
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
                        HttpMessageNotReadableException ex,
                        HttpServletRequest request) {
                return new ResponseEntity<>(
                                buildResponse(HttpStatus.BAD_REQUEST, "Request body is missing or invalid", request),
                                HttpStatus.BAD_REQUEST);
        }
}
