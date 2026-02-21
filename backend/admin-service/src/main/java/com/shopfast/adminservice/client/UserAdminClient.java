package com.shopfast.adminservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", url="${user.service.url}",  path = "/api/v1/user")
public interface UserAdminClient {

    @GetMapping("/internal/admin/users/{id}")
    Object getAllUsers(@PathVariable String id, @RequestParam Integer pageNumber, @RequestParam Integer pageSize, @RequestParam String status);

    @PutMapping("/{id}/block")
    void blockUser(@PathVariable Long id);

    @PutMapping("/{id}/unblock")
    void unblockUser(@PathVariable Long id);

}
