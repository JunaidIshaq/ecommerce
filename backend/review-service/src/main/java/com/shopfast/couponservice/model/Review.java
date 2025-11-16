package com.shopfast.couponservice.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "review",
        uniqueConstraints = @UniqueConstraint(name="u_user_product", columnNames = {"productId", "userId"}),
        indexes = @Index(name = "idx_productId", columnList = "productId"))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Review {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Integer rating; // 1 - 5

    @Column(length = 200)
    private String title;

    @Column(length = 2000)
    private String comment;

    private Instant createdAt;

    private Instant updatedAt;

}
