package com.pkcorporate.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private String company;
    private String gstin;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private boolean active;
    private String notes;
    private UUID agentId;
    private String agentName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
