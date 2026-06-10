# Web Shop (Online Catalog & Ordering) — Implementation Plan

Goal: let customers browse a curated **draft/live item list** on a public website (mobile-friendly)
and place orders online without visiting the office. Orders land in the staff app, get confirmed,
and convert into existing POS sales / AR invoices (debt) — reusing the modules that already exist.

This plan is grounded in the current codebase:

- `com.hisobnoma.platform.web` already exists as an **empty stub** documented as
  *"Web E-commerce module. Handles public shopping cart, checkout, and phone+OTP authentication."*
- `SecurityConfig` already whitelists `/api/v1/web/**` (public) and `/uploads/**` (product images).
- `ExpenseRecordController` establishes the public-endpoint pattern: resolve tenant from
  `TenantContext` (X-Tenant-ID header), default tenant `1`.
- `Product` is storefront-ready: descriptions, category/brand, sorted `ProductImage` list,
  `sellingPrice`/`wholesalePrice`, `active` + `sellable` flags.
- Reusable infrastructure: delivery regions/villages module, `SmsService.sendSms(phone, message)`
  (for OTP), `TelegramNotificationService` (staff alerts), `CustomerService.createCustomer`
  (auto-generates customer code), `StockService.getStockByProduct` (availability),
  `ARInvoiceService` (supports zero-price lines).
- Test conventions (352 existing test files): `@DataJpaTest` repository tests (H2, Flyway off),
  Mockito service/mapper unit tests, standalone-MockMvc controller tests, and
  `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional` full-flow tests.
- Frontend: staff Vue 3 SPA (no test infra). Latest Flyway migration: **V53** — new ones start at V54.

---

## Architecture decisions

1. **Dedicated catalog table, not flags on `products`.**
   New `web_catalog_items` table (product_id FK, optional display-name/price overrides, sort_order,
   status `DRAFT`/`LIVE`). This directly models the requested "draft or live item list":
   staff stage items as DRAFT, review, then publish. Core inventory tables stay untouched;
   the heavily-used `products` table needs no migration. Price fallback: item override if set,
   otherwise the product's live `sellingPrice` at read time (no staleness).

2. **New `WebOrder` entity, not POS transactions or a generic SalesOrder.**
   POS transactions are bound to shifts/registers (shift close blocks on PENDING/HELD), so online
   orders must not enter that lifecycle. A generic SalesOrder module is bigger scope than needed.
   `WebOrder` has its own lifecycle: `NEW → CONFIRMED → DELIVERING → COMPLETED` / `CANCELLED`.
   Conversion to an AR invoice (debt) or recording as a sale stays a **manual staff action**.

3. **Separate lightweight storefront app (`storefront/`), not public routes in the staff SPA.**
   Same stack (Vue 3 + Vite + Tailwind), minimal pages, served by nginx on the VPS (e.g.
   `shop.<domain>` or `/shop`). Keeps admin code out of the public bundle. Mobile users get the
   responsive storefront; the public API is equally usable by a future native app.

4. **Customer identity is deferred and isolated.**
   Phases 1–3 need no customer login (checkout = name + phone + delivery address, cash on
   delivery / pay-later debt). Phase 4 adds `web_customers` + SMS OTP with tokens that are
   explicitly **not** staff JWTs (separate claim type, never resolved by the staff
   `JwtAuthenticationFilter` into a `UserPrincipal`).

5. **Public API hygiene.**
   Dedicated public DTOs that whitelist fields (never `costPrice`, `minSellingPrice`, stock
   numbers — only an `inStock` boolean). Tenant scoping in every query. Rate limiting on
   checkout/OTP. Orders record `sourceIp` + `userAgent`.

---

## Phase 1 — Curated catalog: backend + admin UI

**Value delivered:** staff can build the item list (draft → live); a public JSON API serves live items.

### Backend
| Item | Detail |
|---|---|
| Migration `V54__web_catalog_items.sql` | Table + indexes (tenant, status, sort_order, unique tenant+product); permissions `WEB_CATALOG_VIEW`, `WEB_CATALOG_MANAGE` seeded to ADMIN/SUPER_ADMIN (V48 pattern) |
| Entity `web/entity/WebCatalogItem.java` | extends `TenantAwareEntity`; `@ManyToOne Product`; status enum `WebCatalogStatus { DRAFT, LIVE }` |
| Repository | `findByTenantIdAndStatusOrderBySortOrder`, `existsByTenantIdAndProductId`, paged admin search |
| Service `WebCatalogService` | add/remove products, bulk add, price/name override, reorder (move up/down like invoice lines), publish/unpublish single + bulk, effective-price fallback |
| Staff controller `/api/v1/web-catalog` (authenticated) | CRUD + publish endpoints guarded by `@RequiresPermission` |
| Public controller `/api/v1/web/catalog` (anonymous) | `GET /products` (paged, search, category filter), `GET /products/{id}`, `GET /categories`; LIVE items only; public DTOs with image URLs, effective price, `inStock` via `StockService.getStockByProduct` |

### Frontend (staff app)
- New page “Веб-каталог” (`/web-catalog`): product picker, per-row Draft/Live toggle with status
  badge, price-override input, up/down reorder buttons (same pattern as invoice-line editor),
  bulk publish/unpublish, search. Router + sidebar entry + uz-Cyrl i18n keys.

### Tests
- **Repository:** only LIVE returned for storefront query; tenant isolation; sort order respected; unique product-per-tenant constraint.
- **Service (Mockito):** price fallback (override vs `sellingPrice`); publish/unpublish transitions; reorder swaps; rejects adding inactive/non-sellable products; bulk operations.
- **Controller (standalone MockMvc):** staff CRUD happy paths + validation errors.
- **Full-flow (`WebCatalogFullFlowTest`):** anonymous `GET /api/v1/web/catalog/products` returns 200 with only LIVE items; DRAFT item absent; JSON contains no `costPrice`/stock numbers; staff endpoint returns 401 anonymous / 403 without permission; X-Tenant-ID scoping.
- **Security:** extend `SecurityConfigIntegrationTest` for the new public path.
- **Manual checklist:** curate 3 products (1 draft), verify public JSON, verify image URLs load via `/uploads/**`.

**Acceptance:** staff curates a list; `curl` of the public endpoint shows exactly the LIVE items with correct prices and images. ~2–3 dev-days.

---

## Phase 2 — Public storefront website (browse-only)

**Value delivered:** customers open a link on any phone/PC and see the live item list — the
minimal answer to “item list for website or mobile app”.

### Work
- New `storefront/` Vite app: Catalog grid (search, category filter, price, image, in-stock badge),
  Product detail page, uz-Cyrl text, mobile-first layout. “Order by phone/Telegram” buttons
  (links configurable) until checkout ships in Phase 3.
- Tenant + API base URL via `.env` build vars; X-Tenant-ID header set by the API client.
- Deployment: nginx site config documented in `docs/DEPLOYMENT.md` (static dist + `/api` proxy).

### Tests
- **Vitest (storefront only, minimal setup):** API client mapping, price/format helpers (UZS formatting).
- **Manual checklist (documented in repo):** catalog renders on mobile viewport; draft items invisible; out-of-stock badge; empty-catalog state; slow-network skeletons.

**Acceptance:** a shareable URL shows the live catalog on a phone. ~2 dev-days.

---

## Phase 3 — Online ordering (checkout → staff inbox → invoice)

**Value delivered:** customers purchase without visiting the office; staff confirm and convert
orders into existing flows (AR invoice/debt or completed sale).

### Backend
| Item | Detail |
|---|---|
| Migration `V55__web_orders.sql` | `web_orders` (order_number, status, customer_name, phone, delivery_region_id/village_id + names, address note, totals, source_ip, user_agent, linked customer_id + ar_invoice_id nullable) + `web_order_lines` (product_id, name snapshot, qty, unit_price snapshot, line_total); permissions `WEB_ORDER_VIEW`, `WEB_ORDER_MANAGE` |
| Entities | `WebOrder` (+`WebOrderStatus`), `WebOrderLine` — lines use the orphanRemoval-safe mutate-in-place pattern (clear + add, never replace the list) |
| Public endpoints | `POST /api/v1/web/orders` (checkout: name, phone, region/village from delivery module, lines), `GET /api/v1/web/orders/{number}?phone=` (status lookup). Validation: max 50 lines, qty 0.001–10 000, products must be LIVE, phone format; prices always taken server-side from catalog |
| Rate limiting | simple in-memory bucket per IP+phone on checkout (e.g. 5/min) — interface designed so a Redis impl can replace it later |
| Staff endpoints `/api/v1/web-orders` | paged list with status filter + NEW count badge, detail, confirm/cancel (with reason), status updates, **convert-to-AR-invoice** (`ARInvoiceService` + auto-create Customer via `CustomerService` when not linked — code auto-generation already exists) |
| Notifications | on new order: `TelegramNotificationService` alert to staff + optional SMS to owner (reuse `SmsService`), behind tenant settings |

### Frontend
- **Storefront:** cart (localStorage store), checkout form (name, phone, region→village cascading
  selects from delivery API, note), success page with order number + status-lookup page.
- **Staff app:** “Онлайн буюртмалар” page — list with NEW badge in sidebar, detail modal,
  confirm / cancel / convert-to-invoice buttons, link to created invoice.

### Tests
- **Repository:** order-number generation/uniqueness per tenant; status+tenant filtered queries; NEW count.
- **Service (Mockito):** checkout rejects DRAFT/unknown products, empty/oversized carts, bad qty; server-side price snapshot (client price ignored); status transition matrix (e.g. cannot confirm a CANCELLED order); conversion builds correct `CreateARInvoiceRequest` (works with zero-price lines) and links customer/invoice ids; Telegram/SMS called once (mocked); rate limiter unit tests (window, reset).
- **Full-flow:** anonymous checkout → 201 with order number; staff list shows it; confirm; convert → AR invoice persisted with matching totals + lines and customer auto-created; cancelled order can't convert; checkout with DRAFT product → 400; rate-limit burst → 429; staff endpoints 401/403 matrix.
- **Vitest:** cart store math (add/remove/qty/total), checkout payload mapping.
- **Manual checklist:** end-to-end order from a phone; Telegram message received; invoice visible in debtors flow.

**Acceptance:** an order placed from a phone appears in the staff app within seconds (with
Telegram ping) and one click turns it into an AR invoice. ~3–4 dev-days.

---

## Phase 4 — Customer accounts (SMS OTP) + hardening

**Value delivered:** returning customers log in with phone + SMS code, see order history; abuse
protections are production-grade.

### Backend
| Item | Detail |
|---|---|
| Migration `V56__web_customers_otp.sql` | `web_customers` (phone unique per tenant, name, verified_at, optional `customer_id` link set by staff), `web_otp_codes` (code hash, expires_at, attempts, cooldown) |
| OTP flow | `POST /api/v1/web/auth/request-otp` (SmsService; 60 s cooldown, max 5/day/phone, code hashed, 5-min expiry, 5 attempts) and `POST /api/v1/web/auth/verify` → web-customer token (separate claim `type=web_customer`; staff filter never resolves it as a `UserPrincipal`) |
| Customer endpoints | `GET /api/v1/web/me/orders` (token-scoped); checkout auto-links orders by verified phone |
| Hardening | per-IP limiter on OTP; audit log entries for OTP requests; configurable storefront CORS origin instead of `*` for web endpoints (optional) |

### Frontend (storefront)
- Login modal (phone → code), “My orders” page, logout; token in localStorage with expiry handling.

### Tests
- **Service:** OTP issue/verify happy path; expiry; wrong-code attempt limit → locked; cooldown enforced; daily cap; token claims; staff-token-vs-web-token separation (web token rejected on staff endpoints and vice versa — full-flow).
- **Repository:** phone uniqueness per tenant; lookup of orders by verified phone.
- **Full-flow:** request OTP (SmsService mocked) → verify → fetch own orders only (another phone's orders invisible).
- **Manual checklist:** real SMS via configured provider on VPS; login from phone browser.

**Acceptance:** returning customer sees order history after SMS login; OTP abuse is throttled. ~3 dev-days.

---

## Phase 5 (future, not scheduled)

Online payment (Payme/Click) with payment-status webhooks; stock reservation on confirm;
delivery fee rules per region; uz-Latn/ru storefront locales; SSG/SEO for the storefront;
native mobile app consuming the same `/api/v1/web/**` API.

---

## Cross-cutting

- **Definition of done per phase:** code + migration + tests green (`mvn test`), staff UI i18n'd
  (uz-Cyrl), `docs/API.md` updated for new endpoints, deployed to VPS, manual checklist executed.
- **Key risks & mitigations:**
  - *Public endpoint abuse* → rate limiting (Phase 3), SMS cooldowns/caps (Phase 4), payload size caps, server-side prices.
  - *Hibernate orphanRemoval on order lines* → mutate-in-place pattern (already learned on `ARInvoice.lines`).
  - *H2-vs-Postgres drift* → keep entity types simple (no JSONB); full-flow tests cover JPQL.
  - *Price disputes* → order lines snapshot name+price at checkout time.
  - *Tenant leakage* → every public query filters tenant; full-flow tests assert cross-tenant invisibility.
- **Open questions (defaults chosen, easy to change):**
  1. Storefront domain/path on the VPS? (default: `/shop` behind nginx)
  2. Show retail `sellingPrice` only, or wholesale for logged-in customers? (default: retail only)
  3. Should confirmed orders become AR invoices automatically? (default: manual button)
  4. Delivery fee? (default: none; staff arranges by phone)
