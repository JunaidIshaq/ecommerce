package com.shopfast.userservice.service;

import com.shopfast.userservice.dto.UpdateProfileRequest;
import com.shopfast.userservice.enums.Role;
import com.shopfast.userservice.enums.UserStatus;
import com.shopfast.userservice.model.User;
import com.shopfast.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProfileService profileService;

    private Jwt jwt(String subject, String email) {
        return Jwt.withTokenValue("x")
                .header("alg", "none")
                .subject(subject)
                .claim("email", email)
                .claim("given_name", "Jane")
                .claim("family_name", "Doe")
                .build();
    }

    @Test
    void getOrCreateThrowsWhenNoSubject() {
        Jwt jwt = Jwt.withTokenValue("x").header("alg", "none").claim("email", "nobody@x.com").build();

        assertThatThrownBy(() -> profileService.getOrCreate(jwt))
                .isInstanceOf(InsufficientAuthenticationException.class);
    }

    @Test
    void getOrCreateAdoptsExistingProfileByEmail() {
        String subject = "sub-1";
        User existing = User.builder().id(UUID.randomUUID()).email("jane@x.com").firstName("Old").build();
        when(userRepository.findByKeycloakId(subject)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase("jane@x.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User result = profileService.getOrCreate(jwt(subject, "jane@x.com"));

        assertThat(result.getKeycloakId()).isEqualTo(subject);
        assertThat(result.getFirstName()).isEqualTo("Old"); // not overwritten when already set
        verify(userRepository, org.mockito.Mockito.atLeastOnce()).save(existing);
    }

    @Test
    void getOrCreateProvisionsNewProfileWhenUnknown() {
        String subject = "sub-new";
        when(userRepository.findByKeycloakId(subject)).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(anyString())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(UUID.randomUUID());
            return u;
        });

        User result = profileService.getOrCreate(jwt(subject, "new@x.com"));

        assertThat(result.getEmail()).isEqualTo("new@x.com");
        assertThat(result.getRole()).isEqualTo(Role.ROLE_USER);
        assertThat(result.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void updateOnlyChangesSuppliedFields() {
        String subject = "sub-1";
        User user = User.builder().id(UUID.randomUUID()).keycloakId(subject)
                .email("jane@x.com").firstName("Jane").lastName("Doe").build();
        when(userRepository.findByKeycloakId(subject)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = profileService.update(jwt(subject, "jane@x.com"),
                new UpdateProfileRequest(null, null, "555-1234", null));

        assertThat(updated.getPhone()).isEqualTo("555-1234");
        assertThat(updated.getFirstName()).isEqualTo("Jane"); // untouched
    }

    @Test
    void updateBlanksEmptyPhoneToNull() {
        String subject = "sub-1";
        User user = User.builder().id(UUID.randomUUID()).keycloakId(subject)
                .email("jane@x.com").phone("old").build();
        when(userRepository.findByKeycloakId(subject)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        User updated = profileService.update(jwt(subject, "jane@x.com"),
                new UpdateProfileRequest(null, null, "   ", null));

        assertThat(updated.getPhone()).isNull();
    }
}
