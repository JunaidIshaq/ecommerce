package com.shopfast.userservice.controller;

import com.shopfast.userservice.dto.RegisterRequestDto;
import com.shopfast.userservice.dto.UserDto;
import com.shopfast.userservice.enums.UserStatus;
import com.shopfast.userservice.model.User;
import com.shopfast.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Users", description = "User APIs")
@RestController
@RequestMapping("/api/v1/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Register new user")
    @PostMapping
    public ResponseEntity<UserDto> register(@Valid @RequestBody RegisterRequestDto dto) {
        User u = userService.registerNewUser(dto);
        return ResponseEntity.ok(getUserDto(u));
    }


    @Operation(summary = "Get current user")
    @GetMapping("/me")
    public ResponseEntity<UserDto> me(Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();
        String email = auth.getName();
        User u = userService.findByEmail(email).orElseThrow();
        return ResponseEntity.ok(getUserDto(u));
    }

    @Operation(summary = "Get user by id (admin)")
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getById(@PathVariable UUID id) {
        User u = userService.getById(id);
        return ResponseEntity.ok(getUserDto(u));
    }


    @Operation(summary = "Update status (admin)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<UserDto> updateStatus(@PathVariable UUID id, @RequestParam UserStatus status) {
        User u = userService.updateStatus(id, status);
        return ResponseEntity.ok(getUserDto(u));
    }

    // Internal endpoint used by Auth Service
    @Operation(summary = "Internal: find user by email")
    @GetMapping("/internal/user/email")
    public ResponseEntity<UserDto> findByEmail(@RequestParam String email) {
        return userService.findByEmail(email)
                .map(this::getUserDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private UserDto getUserDto(User u) {
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
