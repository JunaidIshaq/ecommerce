package com.shopfast.userservice.util;

import com.shopfast.userservice.client.ProductClient;
import com.shopfast.userservice.dto.RegisterRequestDto;
import com.shopfast.userservice.repository.UserRepository;
import com.shopfast.userservice.service.UserService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserDataSeeder {

    private final UserRepository userRepository;

    private final UserService userService;

    private final ProductClient productClient;

    public UserDataSeeder(UserRepository userRepository, UserService userService, ProductClient productClient) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.productClient = productClient;
    }

    @Value("${app.seed-user:false}")
    private boolean seedUser; // toggle via application.yml

    private static final int USER_COUNT = 10;

    @PostConstruct
    public void seed() {
        if (!seedUser) {
            System.out.println("🟢 User seeding disabled (set app.seed-user=true to enable)");
            return;
        }
        
        if (userRepository.count() > 0) {
            System.out.println("🟢 Users already exist, skipping seeding.");
            return;
        }

        log.info("🚀 Generating " + USER_COUNT + " dummy users...");

        // Create alice@example.com first
        try {
            RegisterRequestDto aliceRequest = RegisterRequestDto.builder()
                    .email("alice@example.com")
                    .password("secret123")
                    .firstName("Alice")
                    .lastName("User")
                    .build();
            
            userService.registerNewUser(aliceRequest);
            log.info("✅ Created user: alice@example.com");
        } catch (Exception e) {
            log.error("❌ Failed to create user alice@example.com: {}", e.getMessage());
        }
        
        // Create alice1 to alice9 (9 more users)
        for (int i = 1; i < USER_COUNT; i++) {
            try {
                RegisterRequestDto registerRequestDto = RegisterRequestDto.builder()
                        .email("alice" + i + "@example.com")
                        .password("secret123")
                        .firstName("Alice")
                        .lastName("User" + i)
                        .build();
                
                userService.registerNewUser(registerRequestDto);
                log.info("✅ Created user: alice{}@example.com", i);
            } catch (Exception e) {
                log.error("❌ Failed to create user alice{}@example.com: {}", i, e.getMessage());
            }
        }

        System.out.println("✅ Seeded " + USER_COUNT + " users successfully!");
    }

}
