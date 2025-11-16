package com.shopfast.couponservice.repository;

import com.shopfast.couponservice.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Optional<Review> findByProductIdAndUserId(UUID productId, UUID userId);

    List<Review> findByProductIdOrderByCreatedAtDesc(UUID productId);

}
