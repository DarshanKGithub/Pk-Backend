package com.pkcorporate.repository;

import com.pkcorporate.entity.Order;
import com.pkcorporate.entity.User;
import com.pkcorporate.enums.OrderStatus;
import com.pkcorporate.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
public interface OrderRepository extends JpaRepository<Order, UUID> {

    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByTrackingToken(String trackingToken);

    Page<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status, Pageable pageable);
    Page<Order> findByAgentOrderByCreatedAtDesc(User agent, Pageable pageable);

    List<Order> findByStatus(OrderStatus status);
    List<Order> findByPaymentStatus(PaymentStatus paymentStatus);

    @Query("SELECT o FROM Order o WHERE o.agent.id = :agentId AND o.status = :status")
    List<Order> findByAgentAndStatus(@Param("agent") User agent, @Param("status") OrderStatus status);

    // ─── Step 1: EntityGraph variants to eliminate N+1 queries ──────────────

    /**
     * Used by OrderService.getAllOrders() — eagerly joins customer, agent, designer, and
     * items + item.product in a single SQL JOIN, eliminating the N+1 pattern when
     * mapToResponse() accesses those associations.
     * <p>
     * Note: We cannot eagerly join all four @ElementCollection lists in the same JPQL
     * query (Hibernate MultipleBagFetchException). The file URL lists (logos, references,
     * mockups, printables) are fetched separately via @BatchSize(size=20) on the entity.
     */
    @EntityGraph(attributePaths = {"customer", "agent", "designer", "items", "items.product"})
    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC")
    Page<Order> findAllWithAssociations(Pageable pageable);

    /**
     * Used by OrderService.getOrdersByAgent() — same eager join strategy, filtered by agent.
     */
    @EntityGraph(attributePaths = {"customer", "agent", "designer", "items", "items.product"})
    @Query("SELECT o FROM Order o WHERE o.agent = :agent ORDER BY o.createdAt DESC")
    Page<Order> findByAgentWithAssociations(@Param("agent") User agent, Pageable pageable);

    /**
     * Agent dashboard: all orders for an agent with customer + items eagerly loaded.
     * The in-Java loop in DashboardService.getAgentDashboard() touches customer.name
     * and items per order — without this, each access fires a separate query.
     */
    @EntityGraph(attributePaths = {"customer", "items"})
    @Query("SELECT o FROM Order o WHERE o.agent.id = :agentId ORDER BY o.createdAt DESC")
    List<Order> findByAgentIdWithCustomerAndItems(@Param("agentId") UUID agentId);

    /**
     * Designer dashboard: all orders for a designer with customer + items eagerly loaded.
     */
    @EntityGraph(attributePaths = {"customer", "items"})
    @Query("SELECT o FROM Order o WHERE o.designer.id = :designerId ORDER BY o.createdAt DESC")
    List<Order> findByDesignerIdWithCustomerAndItems(@Param("designerId") UUID designerId);

    // ─── Step 1: Aggregation queries to replace in-Java loops ───────────────

    /** Total order count */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :start AND o.createdAt <= :end")
    Long countByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status = 'COMPLETED' AND o.createdAt >= :start")
    BigDecimal sumRevenueFrom(@Param("start") LocalDateTime start);

    /** Admin dashboard: scalar aggregates computed in SQL instead of loaded into memory */
    @Query("SELECT COUNT(o) FROM Order o WHERE o.createdAt >= :monthStart " +
           "AND o.status IN ('COMPLETED', 'DISPATCHED', 'PAYMENT_VERIFIED')")
    Long countRevenueOrdersSince(@Param("monthStart") LocalDateTime monthStart);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.status IN ('COMPLETED', 'DISPATCHED')")
    BigDecimal sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.createdAt >= :monthStart " +
           "AND o.status IN ('COMPLETED', 'DISPATCHED', 'PAYMENT_VERIFIED')")
    BigDecimal sumMonthRevenue(@Param("monthStart") LocalDateTime monthStart);

    @Query("SELECT COALESCE(SUM(o.balanceAmount), 0) FROM Order o " +
           "WHERE o.paymentStatus IN ('PENDING', 'ADVANCE_PAID')")
    BigDecimal sumPendingPayments();

    @Query("SELECT COUNT(o) FROM Order o " +
           "WHERE o.status NOT IN ('COMPLETED', 'CANCELLED', 'REFUNDED')")
    Long countActiveOrders();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN ('PRODUCTION', 'QUALITY_CHECK')")
    Long countInProduction();

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status = 'DISPATCH_READY'")
    Long countDispatchReady();

    @Query("SELECT COUNT(o) FROM Order o " +
           "WHERE o.status = 'COMPLETED' AND o.updatedAt >= :monthStart")
    Long countCompletedSince(@Param("monthStart") LocalDateTime monthStart);

    /** Monthly revenue grouped by year-month for chart data */
    @Query("SELECT YEAR(o.createdAt), MONTH(o.createdAt), COALESCE(SUM(o.totalAmount), 0), COUNT(o) " +
           "FROM Order o " +
           "WHERE o.createdAt >= :since " +
           "GROUP BY YEAR(o.createdAt), MONTH(o.createdAt) " +
           "ORDER BY YEAR(o.createdAt), MONTH(o.createdAt)")
    List<Object[]> sumMonthlyRevenueSince(@Param("since") LocalDateTime since);

    /** Agent dashboard aggregates */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.agent.id = :agentId")
    BigDecimal sumRevenuByAgent(@Param("agentId") UUID agentId);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o " +
           "WHERE o.agent.id = :agentId AND o.createdAt >= :monthStart")
    BigDecimal sumRevenueByAgentSince(@Param("agentId") UUID agentId,
                                      @Param("monthStart") LocalDateTime monthStart);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.agent.id = :agentId AND o.createdAt >= :monthStart")
    Long countOrdersByAgentSince(@Param("agentId") UUID agentId,
                                 @Param("monthStart") LocalDateTime monthStart);

    // ─── Kept from original ──────────────────────────────────────────────────

    @Query("SELECT o FROM Order o WHERE o.customer.id = :customerId ORDER BY o.createdAt DESC")
    List<Order> findByCustomerId(@Param("customerId") UUID customerId);

    @Query("SELECT o FROM Order o WHERE " +
           "LOWER(o.orderNumber) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(o.customer.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(o.customer.company) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Order> searchOrders(@Param("q") String query, Pageable pageable);

    @Query("SELECT o FROM Order o WHERE o.status IN ('PAYMENT_VERIFIED', 'DESIGN_APPROVED') AND o.designer IS NULL")
    List<Order> findUnassignedDesignOrders();

    @Query("SELECT o FROM Order o WHERE o.designer.id = :designerId AND o.status = 'DESIGN_IN_PROGRESS'")
    List<Order> findDesignerActiveOrders(@Param("designerId") UUID designerId);

    // Kept for backward compat — callers can migrate to findByAgentIdWithCustomerAndItems
    @Query("SELECT o FROM Order o WHERE o.agent.id = :agentId")
    List<Order> findByAgentId(@Param("agentId") UUID agentId);

    @Query("SELECT o FROM Order o WHERE o.designer.id = :designerId")
    List<Order> findByDesignerId(@Param("designerId") UUID designerId);
}
