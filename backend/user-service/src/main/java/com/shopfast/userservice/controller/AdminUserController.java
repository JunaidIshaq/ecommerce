package com.shopfast.userservice.controller;

import com.shopfast.userservice.dto.PagedResponse;
import com.shopfast.userservice.dto.UserDto;
import com.shopfast.userservice.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Admin Users", description = "Admin User APIs")
@RestController
@RequestMapping("/api/v1/user")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Operation(summary = "Get order status by ID for admin")
    @GetMapping("/internal/admin/users/{id}")
    public ResponseEntity<PagedResponse<UserDto>> getOrderStatus(
            @PathVariable("id") String id,
            @RequestParam(name = "pageNumber", required = false, defaultValue = "1") Integer pageNumber,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(name = "status", required = false) String status) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserDto> orderPage = userRepository.findAll(pageable)
                .map(UserDto::from);

        PagedResponse<UserDto> response = new PagedResponse<>(
                orderPage.getContent(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages(),
                orderPage.getNumber(),
                orderPage.getSize()
        );

        return ResponseEntity.ok(response);
    }
}
