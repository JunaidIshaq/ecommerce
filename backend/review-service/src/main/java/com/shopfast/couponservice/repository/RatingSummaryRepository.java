package com.shopfast.couponservice.repository;

import com.shopfast.couponservice.model.RatingSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RatingSummaryRepository extends JpaRepository<RatingSummary, UUID> {

}
