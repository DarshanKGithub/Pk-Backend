package com.pkcorporate.repository;

import com.pkcorporate.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByOrderId(UUID orderId);
    List<Payment> findByVerifiedFalse();

    @Query("SELECT p FROM Payment p WHERE p.order.agent.id = :agentId ORDER BY p.createdAt DESC")
    List<Payment> findByAgentId(@Param("agentId") UUID agentId);

    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.verified = true AND p.createdAt >= :start AND p.createdAt <= :end")
    BigDecimal sumVerifiedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
