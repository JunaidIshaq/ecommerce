package com.shopfast.authservice.client;

import com.shopfast.authservice.dto.RegisterRequestDto;
import com.shopfast.authservice.dto.UserInternalDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", url = "${user.service.url}")
public interface UserClient {

   @GetMapping("/api/v1/user/internal/email")
   UserInternalDto findByEmail(@RequestParam("email") String email);

   /**
    * Creates the ShopFast profile row that owns orders, addresses and carts.
    * Keycloak holds the credential; this holds the domain data.
    */
   @PostMapping("/api/v1/user")
   UserInternalDto register(@RequestBody RegisterRequestDto request);

}
