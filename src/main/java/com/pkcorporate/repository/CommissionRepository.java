package com.pkcorporate.repository;

import com.pkcorporate.entity.Commission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, UUID> {
    List<Commission> findByAgentId(UUID agentId);
    List<Commission> findByAgentIdAndStatus(UUID agentId, String status);

    @Query("SELECT SUM(c.commissionAmount) FROM Commission c WHERE c.agent.id = :agentId AND c.status = 'PAID'")
    BigDecimal totalPaidCommissionForAgent(@Param("agentId") UUID agentId);

    @Query("SELECT SUM(c.commissionAmount) FROM Commission c WHERE c.agent.id = :agentId AND c.status = 'PENDING'")
    BigDecimal totalPendingCommissionForAgent(@Param("agentId") UUID agentId);
}
