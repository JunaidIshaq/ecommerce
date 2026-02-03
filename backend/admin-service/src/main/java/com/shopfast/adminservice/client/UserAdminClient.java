package com.shopfast.adminservice.client;

import com.shopfast.adminservice.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import java.util.List;

@FeignClient(name = "user-service", url="${user.service.url}",  path = "/internal/admin/users")
public interface UserAdminClient {

    @GetMapping
    List<UserDto> getAllUsers();

    @PutMapping("/{id}/block")
    void blockUser(@PathVariable Long id);

    @PutMapping("/{id}/unblock")
    void unblockUser(@PathVariable Long id);

}
