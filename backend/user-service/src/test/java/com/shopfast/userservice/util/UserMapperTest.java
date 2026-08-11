package com.shopfast.userservice.util;

import com.shopfast.userservice.dto.UserDto;
import com.shopfast.userservice.enums.Role;
import com.shopfast.userservice.enums.UserStatus;
import com.shopfast.userservice.model.User;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    @Test
    void getUserDtoMapsAllFields() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        User user = User.builder()
                .id(id)
                .email("a@b.com")
                .firstName("Jane")
                .lastName("Doe")
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        UserDto dto = UserMapper.getUserDto(user);

        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getEmail()).isEqualTo("a@b.com");
        assertThat(dto.getFirstName()).isEqualTo("Jane");
        assertThat(dto.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(dto.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
}
