package com.pkcorporate.repository;

import com.pkcorporate.entity.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);
    // Bounded variant: caps the result so a user's notification history can't
    // load unbounded into memory on a 512MB box. Caller passes PageRequest.of(0, N).
    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    List<Notification> findByUserIdAndReadFalse(UUID userId);
    long countByUserIdAndReadFalse(UUID userId);

    @Modifying
    @Transactional
    @Query("UPDATE Notification n SET n.read = true WHERE n.user.id = :userId")
    void markAllAsRead(@Param("userId") UUID userId);
}
