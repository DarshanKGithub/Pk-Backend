package com.pkcorporate.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class AuthDTOs {

    @Data
    public static class LoginRequest {
        @NotBlank
        private String email;

        @NotBlank @Size(min = 6)
        private String password;

        private boolean rememberMe;
    }

    @Data
    public static class ForgotPasswordRequest {
        @NotBlank @Email
        private String email;
    }

    @Data
    public static class ResetPasswordRequest {
        @NotBlank
        private String token;

        @NotBlank @Size(min = 8)
        private String password;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String currentPassword;

        @NotBlank @Size(min = 8)
        private String newPassword;
    }

    @Data
    public static class RefreshTokenRequest {
        @NotBlank
        private String refreshToken;
    }
}
