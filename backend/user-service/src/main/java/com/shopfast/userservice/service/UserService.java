package com.shopfast.userservice.service;

import com.shopfast.userservice.dto.PagedResponse;
import com.shopfast.userservice.dto.RegisterRequestDto;
import com.shopfast.userservice.dto.UserDto;
import com.shopfast.userservice.enums.Role;
import com.shopfast.userservice.enums.UserStatus;
import com.shopfast.userservice.events.KafkaUserProducer;
import com.shopfast.userservice.model.User;
import com.shopfast.userservice.repository.UserRepository;
import com.shopfast.userservice.util.UserMapper;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.awt.print.Pageable;
import java.util.List;
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
        if (userRepository.existsByEmailIgnoreCase(dto.getEmail())) {
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

        User saved = userRepository.save(user);

        // Publish user registered event (non-blocking)
        try {
            kafkaUserProducer.publishUserRegistered(user);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
        return saved;
    }

    @Transactional
    public PagedResponse<UserDto> getAllUsers(int pageNumber, int pageSize) {
        PageRequest pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<User> userPage =  userRepository.findAll(pageable);
        List<UserDto> userDtos = userPage.stream().map(UserMapper::getUserDto).toList();
        return new PagedResponse<>(
                userDtos,
                userPage.getTotalElements(),
                userPage.getTotalPages(),
                pageNumber,
                pageSize
        );
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

