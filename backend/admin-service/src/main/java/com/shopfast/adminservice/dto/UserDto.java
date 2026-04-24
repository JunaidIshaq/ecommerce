package com.shopfast.adminservice.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserDto implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private String email;

    private boolean active;

    private String role;

}
