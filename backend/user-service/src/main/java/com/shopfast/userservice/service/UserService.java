package com.shopfast.userservice.service;

import com.shopfast.userservice.dto.RegisterRequestDto;
import com.shopfast.userservice.enums.Role;
import com.shopfast.userservice.enums.UserStatus;
import com.shopfast.userservice.events.KafkaUserProducer;
import com.shopfast.userservice.model.User;
import com.shopfast.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KafkaUserProducer kafkaUserProducer;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, KafkaUserProducer kafkaUserProducer) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.kafkaUserProducer = kafkaUserProducer;
    }

    @Transactional
    public User registerNewUser(RegisterRequestDto dto) {
        if(userRepository.existsByEmailIgnoreCase(dto.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = User.builder()
                .email(dto.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(dto.getPassword()))
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .role(Role.ROLE_USER)
                .status(UserStatus.ACTIVE)
                .build();

        User saved =  userRepository.save(user);

        // Publish user registered event (non-blocking)
        try{
            kafkaUserProducer.publishUserRegistered(user);
        } catch (Exception e){
            throw new RuntimeException(e.getMessage());
        }
        return saved;
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email);
    }

    public User getById(UUID id) {
        return userRepository.findById(id).orElseThrow(NoSuchElementException::new);
    }

    @Transactional
    public User updateStatus(UUID id, UserStatus status) {
        User user = getById(id);
        user.setStatus(status);
        return userRepository.save(user);
    }

}
