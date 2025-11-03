package com.shopfast.authservice.client;

import com.shopfast.authservice.dto.UserInternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", url = "${user.service.url}")
public interface UserClient {

   @GetMapping("/internal/email")
   UserInternalDto findByEmail(@RequestParam("email") String email);

}
