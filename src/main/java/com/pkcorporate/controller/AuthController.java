package com.pkcorporate.controller;

import com.pkcorporate.dto.request.AuthDTOs.*;
import com.pkcorporate.dto.response.ApiResponse;
import com.pkcorporate.dto.response.AuthResponse;
import com.pkcorporate.entity.User;
import com.pkcorporate.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "JWT auth endpoints")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate refresh token")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal User user) {
        // user can be null if called without a valid JWT (public endpoint under /auth/**)
        // Treat missing-token logout as already-logged-out: return 200 (idempotent)
        if (user == null) {
            log.debug("[Auth] Logout called with no authenticated principal — treating as already logged out");
            return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
        }
        log.debug("[Auth] Logout request for user: {} ({})", user.getEmail(), user.getRole());
        authService.logout(user.getId());
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully", null));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset email")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Reset link sent if email exists", null));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using token")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password reset successfully", null));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password (authenticated)")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(user.getId(), request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully", null));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<AuthResponse.UserResponse>> me(@AuthenticationPrincipal User user) {
        AuthResponse.UserResponse profile = AuthResponse.UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .avatar(user.getAvatar())
                .phone(user.getPhone())
                .agentCode(user.getAgentCode())
                .build();
        return ResponseEntity.ok(ApiResponse.success(profile));
    }
}
