package com.shopfast.adminservice.dto;

import lombok.Data;

@Data
public class UserDto {

    private String id;

    private String email;

    private boolean active;

    private String role;

}
