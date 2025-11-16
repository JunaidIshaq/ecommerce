package com.shopfast.couponservice.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "rating_summary")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RatingSummary {

    @Id
    private UUID productId;

    private double averageRating;

    private long totalReviews;

}
