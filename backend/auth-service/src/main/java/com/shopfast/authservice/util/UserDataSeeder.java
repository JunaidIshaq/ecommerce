package com.shopfast.authservice.util;

import com.shopfast.authservice.client.UserClient;
import com.shopfast.authservice.dto.RegisterRequestDto;
import com.shopfast.authservice.dto.UserInternalDto;
import com.shopfast.authservice.keycloak.KeycloakAdminClient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class UserDataSeeder {

    private final UserClient userClient;
    private final KeycloakAdminClient keycloakAdminClient;

    public UserDataSeeder(UserClient userClient, KeycloakAdminClient keycloakAdminClient) {
        this.userClient = userClient;
        this.keycloakAdminClient = keycloakAdminClient;
    }

    @Value("${app.seed-users:false}")
    private boolean seedUsers;

    private static final int USER_COUNT = 10;
    private static final String EMAIL_TEMPLATE = "alice{}@yopmail.com";
    private static final String PASSWORD = "Alice@12345";
    private static final String FIRST_NAME = "Alice";

    @PostConstruct
    public void seed() {
        if (!seedUsers) {
            log.info("🟢 User seeding disabled (set app.seed-users=true to enable)");
            return;
        }

        log.info("🚀 Seeding {} test users...", USER_COUNT);

        int created = 0;
        int skipped = 0;

        for (int n = 1; n <= USER_COUNT; n++) {
            String email = EMAIL_TEMPLATE.replace("{}", String.valueOf(n));

            try {
                UserInternalDto existing = userClient.findByEmail(email);
                if (existing != null) {
                    log.info("  [skip]   {} already exists in user database", email);
                    skipped++;
                    continue;
                }

                if (keycloakAdminClient.findByEmail(email).isPresent()) {
                    log.info("  [skip]   {} already exists in Keycloak", email);
                    skipped++;
                    continue;
                }

                String keycloakId = keycloakAdminClient.createUser(
                        email,
                        email,
                        FIRST_NAME,
                        "Test" + n,
                        PASSWORD,
                        List.of(),
                        true
                );

                keycloakAdminClient.assignRealmRole(keycloakId, "ROLE_USER");

                RegisterRequestDto registerRequest = new RegisterRequestDto();
                registerRequest.setEmail(email);
                registerRequest.setPassword(PASSWORD);
                registerRequest.setFirstName(FIRST_NAME);
                registerRequest.setLastName("Test" + n);

                UserInternalDto createdUser = userClient.register(registerRequest);

                keycloakAdminClient.setUserIdAttribute(keycloakId, createdUser.getId().toString());

                log.info("  [create] {} (Keycloak ID: {}, User DB ID: {})", email, keycloakId, createdUser.getId());
                created++;

            } catch (Exception e) {
                log.error("  [error]  Failed to seed user {}: {}", email, e.getMessage(), e);
            }
        }

        log.info("✅ User seeding complete: created={}, skipped={}", created, skipped);
    }
}
