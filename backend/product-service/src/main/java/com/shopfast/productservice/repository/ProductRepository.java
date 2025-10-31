package com.shopfast.productservice.repository;

import com.shopfast.productservice.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {

    Optional<Product> findBySlug(String slug);

    List<Product> findByCategoryId(String category);

    List<Product> findByNameContainingIgnoreCase(String name);

}