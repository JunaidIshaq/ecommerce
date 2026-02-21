package com.shopfast.adminservice.controller;

import com.shopfast.adminservice.client.UserAdminClient;
import com.shopfast.adminservice.service.AdminUserService;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.HeaderParam;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService service;

    @Autowired
    private UserAdminClient userAdminClient;

    //  @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/pageNumber/{pageNumber}/pageSize/{pageSize}")
    public Object getAllUsers( @RequestHeader("userId") @NotNull(message =  "userId is required") String id,
                               @PathVariable(required = false) Integer pageNumber,
                               @PathVariable(required = false) Integer pageSize,
                               @RequestParam(required = false) String role,
                               Authentication auth) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        if(StringUtils.isEmpty(userId)) {
            throw new RuntimeException("Not an Admin User");
        }

        return userAdminClient.getAllUsers(id, pageNumber, pageSize, role);

    }

    @PutMapping("/{id}/block")
    public void block(@PathVariable Long id, Authentication auth) {
        service.blockUser(id, auth.getName());
    }
}
