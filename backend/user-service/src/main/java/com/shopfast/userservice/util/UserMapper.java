package com.shopfast.userservice.util;

import com.shopfast.userservice.dto.UserDto;
import com.shopfast.userservice.model.User;

public class UserMapper {

    public static UserDto getUserDto(User u) {
        return UserDto.builder()
                .id(u.getId())
                .email(u.getEmail())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .role(u.getRole())
                .status(u.getStatus())
                .createdAt(u.getCreatedAt())
                .updatedAt(u.getUpdatedAt())
                .build();
    }
}
