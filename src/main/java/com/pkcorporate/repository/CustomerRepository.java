package com.pkcorporate.repository;

import com.pkcorporate.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    List<Customer> findByAgentId(UUID agentId);
    Page<Customer> findByAgentId(UUID agentId, Pageable pageable);
    boolean existsByEmailAndAgentId(String email, UUID agentId);
    // Step 1: SQL COUNT instead of loading all agents' customers into memory
    long countByAgentId(UUID agentId);

    @Query("SELECT c FROM Customer c WHERE c.agent.id = :agentId AND (" +
           "LOWER(c.name) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(c.company) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Customer> searchByAgent(@Param("agentId") UUID agentId, @Param("q") String q, Pageable pageable);

    @Query("SELECT c FROM Customer c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(c.email) LIKE LOWER(CONCAT('%',:q,'%')) OR " +
           "LOWER(c.company) LIKE LOWER(CONCAT('%',:q,'%'))")
    Page<Customer> searchAll(@Param("q") String q, Pageable pageable);
}
