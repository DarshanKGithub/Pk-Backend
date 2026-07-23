package com.pkcorporate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers", indexes = {
    @Index(name = "idx_customer_email", columnList = "email"),
    @Index(name = "idx_customer_phone", columnList = "phone"),
    @Index(name = "idx_customer_agent", columnList = "agent_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Customer extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String phone;

    private String company;
    private String gstin;
    private String address;
    private String city;
    private String state;
    private String pincode;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id")
    private User agent;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Order> orders = new ArrayList<>();
}
