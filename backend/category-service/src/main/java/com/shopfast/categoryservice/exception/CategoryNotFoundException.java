package com.shopfast.categoryservice.exception;

public class CategoryNotFoundException extends RuntimeException {

    public CategoryNotFoundException(String id) {
        super("Category not found with ID: " + id);
    }

}
