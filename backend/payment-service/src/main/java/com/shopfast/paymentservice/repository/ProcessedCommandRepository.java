package com.shopfast.paymentservice.repository;

import com.shopfast.paymentservice.model.ProcessedCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedCommandRepository extends JpaRepository<ProcessedCommand, UUID> {

    Optional<ProcessedCommand> findByCommandId(String commandId);

}
