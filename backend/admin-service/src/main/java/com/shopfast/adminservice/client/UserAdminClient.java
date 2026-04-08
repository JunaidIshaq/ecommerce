package com.shopfast.adminservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", url="${user.service.url}",  path = "/api/v1/user")
public interface UserAdminClient {

    @GetMapping("/internal/admin/users/pageNumber/{pageNumber}/pageSize/{pageSize}")
    Object getAllUsers(@RequestHeader("userId") String userId, @PathVariable("pageNumber") Integer pageNumber, @PathVariable("pageSize") Integer pageSize, @RequestParam("role") String role);

    @PutMapping("/{id}/block")
    void blockUser(@PathVariable("id") Long id);

    @PutMapping("/{id}/unblock")
    void unblockUser(@PathVariable("id") Long id);

}
