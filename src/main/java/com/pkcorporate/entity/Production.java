package com.pkcorporate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "production")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Production extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    @Builder.Default
    private String status = "NOT_STARTED"; // NOT_STARTED, CUTTING, STITCHING, PRINTING, QC, COMPLETED

    private LocalDate estimatedCompletionDate;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;

    private Integer totalQuantity;
    private Integer completedQuantity;
    private Integer rejectedQuantity;

    private String assignedUnit; // Production unit/machine
    private String supervisorNotes;

    @Column(columnDefinition = "TEXT")
    private String progressNotes;

    private Integer progressPercentage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private User supervisor;
}
