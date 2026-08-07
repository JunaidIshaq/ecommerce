package com.shopfast.orderservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.shopfast.orderservice.enums.OrderStatus;
import com.shopfast.orderservice.enums.PaymentMethod;
import jakarta.persistence.Index;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
// "my orders" is the hottest read in the service and always filters by userId,
// newest first; without these it degrades to a full scan as the table grows.
@Table(name = "orders", indexes = {
        @Index(name = "idx_orders_user_created", columnList = "userId, createdAt"),
        @Index(name = "idx_orders_status", columnList = "status")
})
@JsonIgnoreProperties(ignoreUnknown = true)  // Prevent unknown fields from breaking serialization
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Order {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    /**
     * The Keycloak subject for a signed-in shopper, or the anonymous browser id
     * for a guest. {@link #guest} says which, so a guest id can never be mistaken
     * for a real account.
     */
    @Column(nullable = false)
    private String userId;

    /**
     * True when {@link #userId} is an anonymous browser id rather than an account.
     *
     * <p>The DDL carries a default so adding this column to a table that already
     * has rows succeeds - a bare NOT NULL would fail the migration outright, and
     * every pre-existing order was placed by a signed-in shopper.
     */
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean guest;

    /**
     * Capability token handed back once at checkout so a guest - who has no JWT -
     * can open their own order later. Random 256 bits: the order id alone is not an
     * access control, and guests have no other credential to prove ownership with.
     * Never returned when listing orders.
     */
    @Column(length = 64)
    private String accessToken;

    @Column(nullable = false, unique = true)
    private String orderNumber;

    private String couponCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus paymentStatus;

    @Column(nullable = false)
    private BigDecimal subTotal;

    private BigDecimal discount;

    @Column(nullable = false)
    private BigDecimal totalAmount;

    // EAGER is kept deliberately: order responses always render their line items and
    // the DTO mapping happens outside the transaction. The cost of EAGER on a paged
    // query is one extra SELECT per order; @BatchSize collapses those into one IN()
    // query per 50 orders, which removes the N+1 without risking lazy-init errors.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @BatchSize(size = 50)
    private List<OrderItem> items;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "fullName", column = @Column(name = "shipping_full_name")),
            @AttributeOverride(name = "street", column = @Column(name = "shipping_street")),
            @AttributeOverride(name = "city", column = @Column(name = "shipping_city")),
            @AttributeOverride(name = "state", column = @Column(name = "shipping_state")),
            @AttributeOverride(name = "zip", column = @Column(name = "shipping_zip")),
            @AttributeOverride(name = "country", column = @Column(name = "shipping_country")),
            @AttributeOverride(name = "phone", column = @Column(name = "shipping_phone"))
    })
    private ShippingAddress shippingAddress;


    @CreationTimestamp
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant createdAt;

    @UpdateTimestamp
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant updatedAt;

    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String updatedBy;

}
