-- ============================================================================
-- Foreign-key & hot-path indexes for pkcorporate ERP (PostgreSQL)
-- ============================================================================
-- Why this file exists:
--   Production runs spring.jpa.hibernate.ddl-auto=validate, so Hibernate will
--   NOT create the @Index definitions added to the entities. Apply these
--   statements manually (psql / Render dashboard) ONCE to create the indexes
--   without enabling schema diffing. Names match the entity @Index names so
--   `validate` is satisfied afterward.
--
-- All statements use IF NOT EXISTS and CONCURRENTLY so they are idempotent and
-- do not lock the table for writes on an existing dataset.
-- NOTE: CREATE INDEX CONCURRENTLY cannot run inside a transaction block.
--       Run this file with psql in autocommit mode (the default for \i), NOT
--       wrapped in BEGIN/COMMIT.
-- ============================================================================

-- commissions: agent payout lookups + aggregation (CommissionRepository)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_commission_agent        ON commissions (agent_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_commission_order        ON commissions (order_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_commission_agent_status ON commissions (agent_id, status);

-- customers: agent-scoped customer list (CustomerRepository.findByAgentId)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_customer_agent ON customers (agent_id);

-- orders: designer dashboard / assignment queries (OrderRepository)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_order_designer ON orders (designer_id);

-- invoices / dispatch: lookup by order (InvoiceRepository.findByOrderId, dispatch)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_invoice_order  ON invoices (order_id);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_dispatch_order ON dispatch (order_id);

-- inventory: active + category filters (InventoryRepository)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inventory_active   ON inventory (active);
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_inventory_category ON inventory (category);
