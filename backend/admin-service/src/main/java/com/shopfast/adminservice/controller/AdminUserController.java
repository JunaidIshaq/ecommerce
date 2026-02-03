package com.shopfast.adminservice.controller;

import com.shopfast.adminservice.dto.UserDto;
import com.shopfast.adminservice.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService service;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public List<UserDto> allUsers() {
        return service.getUsers();
    }

    @PutMapping("/{id}/block")
    public void block(@PathVariable Long id, Authentication auth) {
        service.blockUser(id, auth.getName());
    }
}
