package com.pkcorporate.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class UserResponse {
    private UUID id;
    private String name;
    private String email;
    private String role;
    private String phone;
    private String avatar;
    private boolean active;
    private LocalDateTime createdAt;
}
