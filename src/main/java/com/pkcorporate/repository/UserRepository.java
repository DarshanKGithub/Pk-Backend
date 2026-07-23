package com.pkcorporate.repository;

import com.pkcorporate.entity.User;
import com.pkcorporate.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    Optional<User> findByResetPasswordToken(String token);
    Optional<User> findByRefreshToken(String token);
    List<User> findByRole(Role role);
    List<User> findByRoleAndActive(Role role, boolean active);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.role = :role AND u.active = true ORDER BY u.name")
    List<User> findActiveByRole(Role role);
}
