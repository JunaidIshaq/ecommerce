package com.shopfast.categoryservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "category")
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler", "subCategoryIds" }, ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@ToString(exclude = "subCategoryIds")   // <— Lombok won’t trigger lazy load
@EqualsAndHashCode(exclude = "subCategoryIds")
public class Category {

    @Id
    private UUID id;

    @Column(name = "name", unique = true, nullable = false)
    private String name;

    @Column(name = "description", columnDefinition = "Text")
    private String description;

    // For hierarchical structure
    @Column(name = "parentId")
    private String parentId;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "category_subcategory_ids", joinColumns = @JoinColumn(name = "category_id"))
    @Column(name = "sub_category_id")
    @JsonIgnore
    private List<String> subCategoryIds;

    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Column(name = "updated_at", updatable = false)
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by")
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;


}
