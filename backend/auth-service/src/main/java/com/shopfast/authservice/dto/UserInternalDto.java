package com.shopfast.authservice.dto;

import com.shopfast.authservice.enums.Role;
import com.shopfast.authservice.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInternalDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private UUID id;

    private String email;

    private String password;

    private String firstName;

    private String lastName;

    private Role role;

    private UserStatus status;

    private Instant createdAt;

    private Instant updatedAt;

}
