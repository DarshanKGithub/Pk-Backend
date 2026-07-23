package com.pkcorporate.service;

import com.pkcorporate.dto.request.AuthDTOs.*;
import com.pkcorporate.dto.response.AuthResponse;
import com.pkcorporate.entity.User;
import com.pkcorporate.exception.BusinessException;
import com.pkcorporate.repository.UserRepository;
import com.pkcorporate.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (BadCredentialsException e) {
            // Constant-time delay to prevent email enumeration via timing
            try { Thread.sleep(100); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
            throw new BusinessException("Invalid email or password");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("Invalid email or password"));

        if (!user.isActive()) {
            throw new BusinessException("Account is deactivated. Contact administrator.");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        user.setRefreshToken(refreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        userRepository.save(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        User user = userRepository.findByRefreshToken(refreshToken)
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (!jwtService.isTokenValid(refreshToken)) {
            throw new BusinessException("Refresh token expired. Please login again.");
        }

        // Validate DB-side expiry as well (allows server-side revocation)
        if (user.getRefreshTokenExpiry() == null || user.getRefreshTokenExpiry().isBefore(LocalDateTime.now())) {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
            throw new BusinessException("Refresh token expired. Please login again.");
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpiry(LocalDateTime.now().plusDays(7));
        userRepository.save(user);

        return buildAuthResponse(user, newAccessToken, newRefreshToken);
    }

    public void logout(UUID userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            userRepository.save(user);
        });
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            user.setResetPasswordToken(token);
            user.setResetPasswordExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);
        });
        // Always return success to prevent email enumeration
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetPasswordToken(request.getToken())
                .orElseThrow(() -> new BusinessException("Invalid or expired reset token"));

        if (user.getResetPasswordExpiry().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Reset token has expired. Please request a new one.");
        }

        validatePasswordStrength(request.getPassword());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiry(null);
        userRepository.save(user);
    }

    public void changePassword(UUID userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }

        validatePasswordStrength(request.getNewPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
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

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .user(AuthResponse.UserResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .role(user.getRole().name())
                        .avatar(user.getAvatar())
                        .phone(user.getPhone())
                        .agentCode(user.getAgentCode())
                        .build())
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }
}
