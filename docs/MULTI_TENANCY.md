# Multi-Tenancy — Data Isolation Between Warehouse Owners

**Question this answers:** the platform hosts many independent warehouse owners on one deployment.
If several owners add their own products, customers, orders, etc., do those records mix together in
the shared database? **No.** Every owner-owned row is tagged with a `tenant_id`, and every query is
filtered by it. This document explains exactly how that isolation works, where it is enforced, and
its known limits.

---

## 1. The model: shared database, shared schema, row-level isolation

There is **one database and one set of tables**. Owners are **not** separated by database or by
schema. Instead, each owner ("tenant") is a row-discriminator:

- Every tenant-owned table has a **`tenant_id`** column.
- A product owned by tenant 5 is the same table as a product owned by tenant 9 — they are told apart
  by their `tenant_id` value.

This is the standard *discriminator-column* multi-tenancy pattern. It is simpler to operate than
schema-per-tenant or database-per-tenant, at the cost of relying on the application to always filter
by `tenant_id` (see [§6 Limits](#6-limits-and-residual-risk)).

```
tenants (id, name, …)
products (id, tenant_id → tenants.id, sku, name, …)      -- one owner's products
web_orders (id, tenant_id, order_number, …)              -- one owner's online orders
customers, pos_transactions, accounts, …                 -- all carry tenant_id
```

### Which tables are tenant-scoped?
Any entity that extends **`TenantAwareEntity`** (`common/entity/TenantAwareEntity.java`), which adds
the `tenant_id` column:

```java
@MappedSuperclass
public abstract class TenantAwareEntity extends AuditableEntity {
    @Column(name = "tenant_id")
    private Long tenantId;
}
```

Products, inventory, POS transactions, finance (AR/AP/GL), customers, web-shop, distribution, HR,
etc. all extend it. A handful of truly global tables (e.g. the `tenants` table itself, some
reference data) are not tenant-scoped by design.

---

## 2. Where the tenant comes from (per request)

The current tenant is held in a **`TenantContext`** (a `ThreadLocal`, so it is isolated per request
thread) and populated by **`TenantFilter`** on every incoming request:

1. **Staff / authenticated API** — the tenant is read from the caller's **JWT** (`tenantId` claim).
   A staff user physically cannot act outside their own tenant, because the tenant is baked into the
   token the server issued them, not supplied by the client.
2. **Anonymous storefront** (`/api/v1/web/**`, `/api/v1/b2b/**`) — there is no JWT, so the tenant
   comes from the **`X-Tenant-ID`** header (which shop the app is pointing at).
3. **Missing tenant → rejected.** If neither a token nor a header provides a tenant, the request is
   **rejected with `400 TENANT_REQUIRED`** — the system never silently falls back to "tenant 1".
   (This "fail closed" behaviour was hardened across the web storefront, B2B, SMS and expense paths.)

`TenantContext` is **cleared after every request**, so one request can never inherit another's
tenant.

---

## 3. On WRITE — rows are stamped with the owner's tenant automatically

When any owner-owned entity is saved, its `tenant_id` is set to the current tenant:

- **`TenantAspect`** (`common/tenant/TenantAspect.java`) intercepts every
  `…Repository.save*(…)` call. If the entity is a `TenantAwareEntity` with no `tenant_id` yet, it
  stamps the current `TenantContext` tenant onto it:

  ```java
  @Before("execution(* …repository.*Repository.save*(..)) && args(entity, ..)")
  public void setTenantBeforeSave(JoinPoint jp, Object entity) {
      if (entity instanceof TenantAwareEntity e && e.getTenantId() == null && TenantContext.hasTenant())
          e.setTenantId(TenantContext.getCurrentTenant());
  }
  ```

- Services also set `tenantId` explicitly in most builders (belt-and-suspenders).

**Result:** when Owner A creates a product, it is written with A's `tenant_id`; Owner B's product
gets B's. They cannot collide.

---

## 4. On READ — every query is filtered by tenant

Repositories deliberately **do not** expose "fetch everything". They expose tenant-scoped finders,
and every query includes `WHERE tenant_id = :tenantId`. Example (`ProductRepository`):

```java
@Query("SELECT p FROM Product p WHERE p.tenantId = :tenantId OR p.tenantId IS NULL")
Page<Product> findAllByTenantId(Long tenantId, Pageable pageable);

@Query("SELECT p FROM Product p WHERE p.id = :id AND (p.tenantId = :tenantId OR p.tenantId IS NULL)")
Optional<Product> findByIdAndTenantId(Long id, Long tenantId);
```

Services resolve the tenant from `SecurityContextHelper.getRequiredTenantId()` (staff) or
`TenantContext` (public) and pass it into these finders. So Owner A's product list only ever returns
A's rows; Owner A can never load Owner B's product by id, because `findByIdAndTenantId` filters it
out and returns "not found".

---

## 5. Guardrail: the build fails on an unscoped lookup

Isolation is protected by an **ArchUnit test that runs in CI**:

- **`TenantIsolationArchTest`** scans the code and **fails the build** if any call to
  `findById` / `getById` / `getReferenceById` on a **tenant-scoped repository** is used without an
  explicit allowlist entry + justification. Raw `findById(id)` skips the tenant filter, so a
  request-supplied id could otherwise reach another tenant's row — the test makes that a build error,
  not a production incident. (It caught a real case during recent work.)
- **`AuthorizationCoverageArchTest`** separately ensures every staff endpoint is permission-guarded.

---

## 6. Limits and residual risk

Be honest about what this design does and does not guarantee:

1. **Enforcement is in the application, not the database.** There is no Postgres Row-Level Security
   (RLS) and no schema separation. Isolation holds **as long as every query carries the tenant
   filter**. The convention is strong (every finder is `…AndTenantId`) and `TenantIsolationArchTest`
   guards the common case — **but that test only inspects id-style lookups, not arbitrary custom
   `@Query` list methods.** A future hand-written query that forgets `AND tenant_id = :tenantId`
   would not be caught automatically and could leak. **Rule for developers:** every query on a
   tenant-scoped table must filter by `tenant_id`; never add a raw `findById` finder — use
   `findByIdAndTenantId`.

2. **`tenant_id IS NULL` means "global / shared".** Several queries read
   `WHERE tenant_id = :tenantId OR tenant_id IS NULL`. A row with a NULL `tenant_id` is visible to
   **all** tenants — this is intentional for shared reference/seed data. Application code cannot
   create NULL-tenant rows (the aspect always stamps the current tenant), so owner-created data is
   always scoped; but seed migrations or manual SQL that insert NULL-tenant rows make them global.
   Only put genuinely shared reference data there.

3. **Cross-tenant joins.** When adding a query that joins two tenant-scoped tables, filter **both**
   by the same `tenant_id` — do not assume a child row's parent is same-tenant.

---

## 7. Recommended hardening (defense-in-depth)

For a platform hosting genuinely independent owners, consider turning "we are careful" into "the
database will not allow it":

1. **Postgres Row-Level Security (strongest).** Add an RLS policy on each tenant table
   (`USING (tenant_id = current_setting('app.tenant_id')::bigint OR tenant_id IS NULL)`) and set
   `app.tenant_id` per connection from `TenantContext`. Then even a query that forgets the filter
   cannot return another tenant's rows — the database refuses. This closes limit §6.1 at the source.
2. **Extend `TenantIsolationArchTest`** to also scan custom `@Query`/derived list methods on
   tenant-scoped repositories for a missing tenant predicate — cheaper than RLS, closes the one gap
   the current test does not cover.

Either can be added without changing the data model or existing queries.

---

## 8. Quick reference for developers

| You are… | Do this |
|---|---|
| Adding a finder to a tenant-scoped repository | Name it `…AndTenantId` and include `WHERE tenant_id = :tenantId`. Never expose raw `findById`. |
| Writing a custom `@Query` list/report | Add `AND e.tenantId = :tenantId` (filter every joined tenant table). |
| Saving an entity | Nothing special — `TenantAspect` stamps `tenant_id`; or set it explicitly from `getRequiredTenantId()`. |
| Reading the current tenant in a service | Staff: `securityContextHelper.getRequiredTenantId()`. Public: `TenantContext.getCurrentTenant()` (already validated non-null by the fail-closed resolvers). |
| Building a public (anonymous) endpoint | Require `X-Tenant-ID`; fail closed if absent — do **not** default to a tenant. |
| Adding shared reference data | Only then may `tenant_id` be NULL (visible to all). Owner data must never be NULL. |
