package com.pkcorporate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_entity", columnList = "entityType,entityId"),
    @Index(name = "idx_audit_user", columnList = "userId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog extends BaseEntity {

    private String userId;
    private String userName;
    private String userRole;

    @Column(nullable = false)
    private String action; // CREATE, UPDATE, DELETE, VIEW, LOGIN, LOGOUT

    @Column(nullable = false)
    private String entityType; // ORDER, PAYMENT, USER, PRODUCT, etc.

    private String entityId;

    @Column(columnDefinition = "TEXT")
    private String oldValue;

    @Column(columnDefinition = "TEXT")
    private String newValue;

    private String description;
    private String ipAddress;
    private String userAgent;
}
