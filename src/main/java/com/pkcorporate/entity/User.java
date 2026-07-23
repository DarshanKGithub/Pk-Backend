package com.pkcorporate.entity;

import com.pkcorporate.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(
        name = "users",
        indexes = {
                @Index(
                        name = "idx_user_email",
                        columnList = "email",
                        unique = true
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends BaseEntity implements UserDetails {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    private String phone;

    private String avatar;

    private String agentCode;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    private String resetPasswordToken;

    private LocalDateTime resetPasswordExpiry;

    private String refreshToken;

    private LocalDateTime refreshTokenExpiry;

    // Agent specific
    private Double commissionRate;

    @OneToMany(
            mappedBy = "agent",
            cascade = CascadeType.ALL,
            fetch = FetchType.LAZY
    )
    @Builder.Default
    private List<Order> orders = new ArrayList<>();

    // ==============================
    // Spring Security UserDetails
    // ==============================

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + role.name())
        );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return emailVerified;
    }
}