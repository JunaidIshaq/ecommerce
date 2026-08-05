package com.shopfast.userservice.repository;

import com.shopfast.userservice.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /** Primary lookup for a signed-in user: the Keycloak {@code sub} claim. */
    Optional<User> findByKeycloakId(String keycloakId);

}