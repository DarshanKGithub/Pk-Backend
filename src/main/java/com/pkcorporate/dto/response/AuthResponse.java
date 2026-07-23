package com.pkcorporate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private UserResponse user;
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserResponse {
        private UUID id;
        private String name;
        private String email;
        private String role;
        private String avatar;
        private String phone;
        private String agentCode;
    }
}
