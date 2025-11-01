package com.shopfast.orderservice.exception;

public class InvalidCategoryException extends RuntimeException {

    public InvalidCategoryException(String categoryId) {
        super("Invalid category ID: " + categoryId + " (Category not found)");
    }

}
