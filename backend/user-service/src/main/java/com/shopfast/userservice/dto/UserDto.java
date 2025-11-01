package com.shopfast.userservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.shopfast.userservice.enums.Role;
import com.shopfast.userservice.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class UserDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @JsonProperty("id")
    private UUID id;

    @JsonProperty("email")
    private String email;

    @JsonProperty("firstName")
    private String firstName;

    @JsonProperty("lastName")
    private String lastName;

    @JsonProperty("role")
    private Role role;
    
    @JsonProperty("status")
    private UserStatus status;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("updated_at")
    private Instant updatedAt;

}