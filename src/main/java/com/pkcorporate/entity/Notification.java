package com.pkcorporate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notif_user", columnList = "user_id"),
    @Index(name = "idx_notif_read", columnList = "read")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private String type; // ORDER, PAYMENT, DESIGN, DISPATCH, SYSTEM

    private String referenceId; // order id, payment id etc.
    private String referenceType;

    @Column(nullable = false)
    @Builder.Default
    private boolean read = false;

    private String actionUrl;
}
