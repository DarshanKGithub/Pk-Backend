# Throughput Audit & Optimization Plan — pkcorporate backend

**Target host:** Render free tier — 512 MB RAM, 0.1 CPU. Every change sized for that.
**Method:** Fresh independent audit (3 parallel agents: data-access, controller/service, config). Findings below are evidence-backed with exact `file:line`. The prior 8-step plan is already fully implemented — these are the gaps a checklist-driven pass missed.
**Status:** PROPOSED — awaiting approval before any edit.

---

## What is already optimal (confirmed — no action)

- N+1 on Order/Dashboard hot paths killed via `@EntityGraph` — `OrderRepository.java:47-71`.
- Product-image N+1 killed via eager join — `TShirtProductRepository.java:20-26`.
- `spring.jpa.open-in-view=false` — `application.properties:93`. ✅ classic throughput killer, correctly off.
- HikariCP `maximum-pool-size=5`, `minimum-idle=2` — sized right for 0.1 CPU.
- `@Async` pool core=2/max=4/queue=50/CallerRunsPolicy — bounded, correct.
- Caffeine cache `maximumSize(500)` + `expireAfterWrite(10m)`; rate-limit filter bounded.
- Gzip on, Tomcat threads capped (50), Hibernate batch_size=25, SerialGC + TieredStopAtLevel=1 + Xss512k.

---

## Findings — ranked by impact-to-effort

### TIER 1 — high impact, very low risk (do first)

**F1. Missing FK indexes (full-table scans on hot paths).** All confirmed by grep:
| Table | Missing index | Evidence | Queried by |
|---|---|---|---|
| `commissions` | `agent_id`, `status`, `order_id` | `Commission.java:10` (no indexes), `:19,:23` | `CommissionRepository` findByAgentId, SUM payouts |
| `customers` | `agent_id` | `Customer.java:10-12` (only email/phone), `:44` | `CustomerRepository.findByAgentId` |
| `orders` | `designer_id` | `Order.java:15-19` (4 idx, not designer), `:40` | `OrderRepository` designer queries `:70,:149,:156` |
| `invoices` | `order_id` | `Invoice.java:10` (none), `:19` | `InvoiceRepository.findByOrderId` |
| `dispatch` | `order_id` | `Dispatch.java:9` (none), `:18` | dispatch lookup by order |
| `inventory` | `active`, `category` | `Inventory.java:10` (none), `:22,:43` | `InventoryRepository.findByActiveTrue/findByCategory` |

- **Fix:** add `@Index` entries to each `@Table`. Additive only — no API/behavior change.
- **Impact:** HIGH (financial + dashboard hot paths stop scanning). **Risk:** very low. **Effort:** low.
- **Note:** `ddl-auto=validate` on Render means Hibernate WON'T auto-create these. After merge, either (a) temporarily flip `DDL_AUTO=update` for one boot, or (b) run the `CREATE INDEX` DDL manually. Plan ships a `db/indexes.sql` with the raw statements so prod can apply them without schema diffing.

**F2. `MaxRAMPercentage=70.0` too high for 512 MB hard cap.** `Dockerfile:22`.
- 70% = ~358 MB heap, leaving only ~154 MB for Metaspace(128m) + thread stacks + NIO direct buffers + GC + code cache. Container OOM-kills before JVM OOM handler fires.
- **Fix:** `MaxRAMPercentage=60.0` (~307 MB heap, ~205 MB native headroom) + add `-XX:MaxDirectMemorySize=32m`.
- **Impact:** HIGH (prevents OOM kills under load). **Risk:** MED (heap shrinks — watch GC). **Effort:** trivial.

### TIER 2 — high impact, low/medium risk

**F3. Unpaginated list endpoints (unbounded result sets + payload).**
- Products: `ProductService.java:165` `findByActiveTrue().stream()` → `ProductController GET /`; `:172` `findAllWithCollections()` → `AdminProductController GET /`. Repo returns `List`, no `Pageable` (`TShirtProductRepository.java:20-26`).
- Orders: `OrderRepository.java:30-31,138,147,153-157` — `findByStatus/PaymentStatus/CustomerId/AgentId/DesignerId` all return `List<Order>`, no `Pageable`.
- Notifications: `NotificationRepository.java:16-17` return `List`.
- Inventory: `InventoryRepository.java:13-14` return `List`.
- **Fix:** add `Pageable`/`Page<>` variants. **Changes response shape → coordinate with frontend** (flagged breaking).
- **Impact:** HIGH (heap + serialization grow with data). **Risk:** MED (API shape). **Effort:** med.
- **Decision needed:** paginate now (breaking) vs cap with a hard `LIMIT`/`PageRequest.of(0,N)` server-side (non-breaking stopgap).

**F4. N+1 on non-EntityGraph order finders feeding `mapToResponse`.**
- `OrderService.java:298` `mapToResponse` iterates `order.getItems()` → `item.getProduct()`, both LAZY (`OrderItem.java:19-25`). The finders in F3 (orders) have no `@EntityGraph`, so each order → extra item+product selects.
- **Fix:** add `@EntityGraph(attributePaths={"customer","items","items.product"})` to those finders, OR route them through the existing `*WithAssociations` queries.
- **Impact:** HIGH where these feed list responses. **Risk:** low. **Effort:** low.
- **Caveat:** agent confirmed lazy mappings + mapper access; did NOT trace every caller. Verify each finder's callers actually hit `mapToResponse` before editing.

### TIER 3 — medium impact, low risk

**F5. Dead `products` cache + uncached hot read.** `application.properties:125` declares `products`; only `getActiveProducts()` is `@Cacheable("activeProducts")` (`ProductService.java:163`). `getAllProducts()` (`:171`, heavy collection join) is uncached; `products` name is only `@CacheEvict`-ed (never written) — dead eviction work.
- **Fix:** either `@Cacheable("products")` on `getAllProducts()`, or remove the unused name + its evictions. **Impact:** MED. **Risk:** low.

**F6. `multipart.max-file-size=50MB`.** `application.properties:54-55`. Two concurrent 50 MB uploads can spike native memory on a 512 MB box.
- **Fix:** drop to 10–15 MB unless 50 MB is a real requirement. **Impact:** MED. **Risk:** MED (rejects big uploads — confirm requirement). **Effort:** trivial.

**F7. Over-fetch: no projection DTOs on order-list paths.** `Order` carries 4 `@ElementCollection` file-URL lists (`Order.java:93,100,107,114`); list finders load all columns + collections when a constructor projection would fetch only `mapToResponse` fields.
- **Fix:** per-endpoint `SELECT new ...Dto(...)` projections. **Impact:** MED. **Risk:** low (behavior-neutral if fields match). **Effort:** med (most work of the set).

### TIER 4 — low impact, trivial

**F8. `DDL_AUTO` default is `update`.** `application.properties:19` `${DDL_AUTO:update}`. If env var ever unset in prod → schema diffing on boot. **Fix:** confirm `DDL_AUTO=validate` set in Render env (config check, not a code edit).

**F9. Banner not disabled.** No `spring.main.banner-mode=off`. Add it — trivial startup saving.

**F10. SpringDoc in fat jar** (`pom.xml:119-124`) — runtime-disabled but still on classpath. Optional: scope to dev Maven profile. Lowest priority.

---

## Proposed execution order (if approved)

1. **F1 + F2** — indexes (`@Index` edits + `db/indexes.sql`) and Dockerfile heap. Highest leverage, near-zero risk. Build-verify.
2. **F4** — `@EntityGraph` on the N+1 finders (after caller-trace confirms). Build-verify.
3. **F5 + F9 + F6** — cache fix, banner off, multipart cap. Build-verify.
4. **F3 + F7** — pagination + projections. **Held for explicit go-ahead** — these touch API response shape (frontend impact).

Each tier: edit → offline Maven compile (cached 3.9.16) → confirm BUILD SUCCESS. No runtime k6 from here — that's your trigger.

## Open decisions for you
- **F3 pagination:** breaking paginate now, or non-breaking server-side cap as stopgap?
- **F6 multipart:** is 50 MB a real upload requirement, or safe to cut to 15 MB?
- **F1 index apply:** ship `db/indexes.sql` for manual apply (keeps `validate`), or one-boot `DDL_AUTO=update`?
