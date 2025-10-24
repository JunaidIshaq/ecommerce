package com.shopfast.categoryservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "category")
@JsonIgnoreProperties(ignoreUnknown = true)  // Prevent unknown fields from breaking serialization
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Category {

    @Id
    private String id;

    private String name;

    private String description;

    // For hierarchical structure
    private String parentId;

    // Optional: store child IDs for faster lookup
    private List<String> subCategoryIds;

    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @LastModifiedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;


}
