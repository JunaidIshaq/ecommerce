package com.shopfast.categoryservice.repository;

import com.shopfast.categoryservice.model.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends MongoRepository <Category, String> {

    Optional<Category> findByNameIgnoreCase(String name);

    List<Category> findByParentId(String parentId);

}
