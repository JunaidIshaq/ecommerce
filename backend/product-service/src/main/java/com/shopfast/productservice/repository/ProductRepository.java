package com.shopfast.productservice.repository;

import com.shopfast.productservice.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    Optional<Product> findBySlug(String slug);

    List<Product> findByCategory(String category);

    List<Product> findByNameContainingIgnoreCase(String name);

}