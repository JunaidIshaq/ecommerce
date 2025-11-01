package com.shopfast.userservice.repository;

import com.shopfast.userservice.model.ProcessedCommand;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProcessedCommandRepository extends JpaRepository<ProcessedCommand, UUID> {

    Optional<ProcessedCommand> findByCommandId(String commandId);

}
