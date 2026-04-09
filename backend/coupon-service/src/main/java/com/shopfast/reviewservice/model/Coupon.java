package com.shopfast.reviewservice.model;

import com.shopfast.reviewservice.enums.CouponType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "coupons", indexes = {
        @Index(name = "idx_coupon_code", columnList = "code", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coupon {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String code; // uppercase

    @Enumerated(EnumType.STRING)
    private CouponType type;

    // For PERCENTAGE: store 10 for 10%. For AMOUNT; store fixed amount.
    private Double value;

    private Double minSubTotal; // minimum order subtotal to apply coupon

    @Column(length = 2000)
    private String applicableProductIds; // null -> all products

    private Integer maxUses; // total uses across all users (null = unlimited)

    private Integer maxUsesPerUser; // per user limit

    private Integer usedCount; // tracking total uses

    private Instant startAt;

    private Instant endAt;

    private Instant createdAt;

}
