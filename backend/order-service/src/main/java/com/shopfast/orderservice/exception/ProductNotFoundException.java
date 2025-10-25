package com.shopfast.orderservice.exception;

public class ProductNotFoundException extends RuntimeException {
    public ProductNotFoundException(String id) {
        super("Product not found with ID: " + id);
    }
}
