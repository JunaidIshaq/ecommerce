package com.shopfast.userservice.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.shopfast.userservice.enums.OrderStatus;
import com.shopfast.userservice.enums.Role;
import com.shopfast.userservice.enums.UserStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
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
@Table(name = "users", indexes = {
        @Index(name = "idx_user_email", columnList = "email", unique = true),
        @Index(name = "idx_user_keycloak_id", columnList = "keycloakId", unique = true)
})
@JsonIgnoreProperties(ignoreUnknown = true)  // Prevent unknown fields from breaking serialization
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    /**
     * The Keycloak {@code sub} claim - the identity this profile belongs to.
     *
     * <p>Email is not usable as the link: a user can change it in Keycloak, and the
     * profile would then silently detach from its account. {@code sub} never changes.
     *
     * <p>Nullable because rows created before the Keycloak migration have no
     * counterpart yet; they are adopted on first sign-in by matching on email.
     */
    @Column(unique = true)
    private String keycloakId;

    /**
     * Only set for accounts that predate Keycloak. Keycloak owns credentials now, so
     * new rows leave this null - hence the column can no longer be NOT NULL.
     */
    @Column
    private String password;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    /** Profile details Keycloak does not hold; the shop owns these. */
    @Column
    private String phone;

    @Column
    private String country;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private UserStatus status;

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
