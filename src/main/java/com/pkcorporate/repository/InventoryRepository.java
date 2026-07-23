package com.pkcorporate.repository;

import com.pkcorporate.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory, UUID> {
    List<Inventory> findByActiveTrue();
    List<Inventory> findByCategory(String category);

    @Query("SELECT i FROM Inventory i WHERE i.currentStock <= i.minimumStockLevel AND i.active = true")
    List<Inventory> findLowStockItems();
}
