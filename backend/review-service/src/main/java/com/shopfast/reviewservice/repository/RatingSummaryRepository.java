package com.shopfast.reviewservice.repository;

import com.shopfast.reviewservice.model.RatingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RatingSummaryRepository extends JpaRepository<RatingSummary, UUID> {

}
