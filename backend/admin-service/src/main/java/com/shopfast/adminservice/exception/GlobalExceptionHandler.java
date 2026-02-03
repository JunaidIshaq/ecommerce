package com.shopfast.adminservice.exception;

import feign.FeignException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Validation errors (@Valid failures)
  @ExceptionHandler(FeignException.class)
  public ResponseEntity<?> handleFeignException(FeignException ex) {
      return ResponseEntity.status(ex.status())
              .body("Downstream service error : " + ex.getMessage());
  }

}
