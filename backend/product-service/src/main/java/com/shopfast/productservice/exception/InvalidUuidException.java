package com.shopfast.productservice.exception;

public class InvalidUuidException extends RuntimeException {
    
    public InvalidUuidException(String message) {
        super(message);
    }
    
    public InvalidUuidException(String field, String value) {
        super(String.format("Invalid UUID format for field '%s': '%s'", field, value));
    }
}
