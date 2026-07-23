package com.shopfast.orderservice.exception;

import com.shopfast.common.dto.GenericApiResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Validation errors (@Valid failures)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericApiResponseDto<Void>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String details = ex.getBindingResult().getAllErrors().stream()
                .map(error -> ((FieldError) error).getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        return ResponseEntity
                .badRequest()
                .body(GenericApiResponseDto.error("Validation failed: " + details, 400));
    }

    // Custom "Not Found" exceptions
    @ExceptionHandler(ProductNotFoundException.class)
    public ResponseEntity<GenericApiResponseDto<Void>> handleProductNotFound(ProductNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(GenericApiResponseDto.error(ex.getMessage(), 404));
    }

    @ExceptionHandler(InvalidCategoryException.class)
    public ResponseEntity<GenericApiResponseDto<Void>> handleInvalidCategory(InvalidCategoryException ex) {
        return ResponseEntity
                .badRequest()
                .body(GenericApiResponseDto.error(ex.getMessage(), 400));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericApiResponseDto<Void>> handleJsonParseError(HttpMessageNotReadableException ex) {
        return ResponseEntity
                .badRequest()
                .body(GenericApiResponseDto.error(
                        "JSON parsing error: " + ex.getMostSpecificCause().getMessage(), 400));
    }

    // Generic fallback for all other exceptions
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericApiResponseDto<Void>> handleGenericException(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(GenericApiResponseDto.error("Internal server error: " + ex.getMessage(), 500));
    }
}
