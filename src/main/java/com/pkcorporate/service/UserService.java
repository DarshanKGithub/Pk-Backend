package com.pkcorporate.service;

import com.pkcorporate.dto.request.CreateUserRequest;
import com.pkcorporate.dto.response.UserResponse;
import com.pkcorporate.entity.User;
import com.pkcorporate.enums.Role;
import com.pkcorporate.exception.BusinessException;
import com.pkcorporate.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Create a new user account (AGENT, ACCOUNTANT, or DESIGNER).
     * Called by ADMIN only.
     */
    public UserResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase())) {
            throw new BusinessException("An account with this email already exists");
        }

        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException("Invalid role: " + request.getRole());
        }

        // Admins cannot be created via this endpoint
        if (role == Role.ADMIN) {
            throw new BusinessException("Admin accounts cannot be created via this endpoint");
        }

        validatePasswordStrength(request.getPassword());

        User user = User.builder()
                .name(request.getName().trim())
                .email(request.getEmail().trim().toLowerCase())
                .phone(request.getPhoneNumber().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .active(true)
                .emailVerified(true)   // Admin-created accounts are pre-verified
                .build();

        // Auto-generate agent code for agents
        if (role == Role.AGENT) {
            String agentCode = generateAgentCode();
            user.setAgentCode(agentCode);
        }

        User saved = userRepository.save(user);

        // Send welcome email asynchronously (swallow errors so creation still succeeds)
        try {
            emailService.sendWelcomeEmail(saved.getEmail(), saved.getName(), request.getPassword(), role.name());
        } catch (Exception e) {
            log.warn("Welcome email could not be sent to {}: {}", saved.getEmail(), e.getMessage());
        }

        log.info("Created {} account for {} ({})", role, saved.getName(), saved.getEmail());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(String roleStr) {
        Role role = Role.valueOf(roleStr.toUpperCase());
        return userRepository.findByRole(role)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public UserResponse deactivateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        user.setActive(false);
        return toResponse(userRepository.save(user));
    }

    public UserResponse activateUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));
        user.setActive(true);
        return toResponse(userRepository.save(user));
    }

    private String generateAgentCode() {
        String code;
        boolean exists;
        do {
            code = "AGT-" + String.format("%04d", (int)(Math.random() * 9000) + 1000);
            final String finalCode = code;
            exists = userRepository.findAll().stream().anyMatch(u -> finalCode.equals(u.getAgentCode()));
        } while (exists);
        return code;
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new BusinessException("Password must be at least 8 characters long");
        }
        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else if (!Character.isLetterOrDigit(c)) hasSpecial = true;
        }
        if (!hasUpper || !hasLower || !hasDigit || !hasSpecial) {
            throw new BusinessException("Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character");
        }
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .active(user.isActive())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
