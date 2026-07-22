# Online Shop (Mobile App + Curated Catalog) — Implementation Plan

Goal: let customers browse a curated **draft/live item list** and place orders through the
**store's mobile app** — no visit to the office needed. There is **no public website**: the only
customer-facing client is the mobile app. Orders land in the staff app (with the admin dashboard
kept up to date), get confirmed, and convert into existing AR invoices (debt) — reusing the
modules that already exist.

This plan is grounded in the current codebase:

- `com.hisobnoma.platform.web` already exists as an **empty stub** documented as
  *"Web E-commerce module. Handles public shopping cart, checkout, and phone+OTP authentication."*
- `SecurityConfig` already whitelists `/api/v1/web/**` (public) and `/uploads/**` (product images).
  The customer-facing API lives under this prefix (despite the "web" name) to avoid touching the
  security chain; it is client-agnostic and serves the mobile app.
- `ExpenseRecordController` establishes the public-endpoint pattern: resolve tenant from
  `TenantContext` (X-Tenant-ID header), default tenant `1`.
- `Product` is shop-ready: descriptions, category/brand, sorted `ProductImage` list,
  `sellingPrice`/`wholesalePrice`, `active` + `sellable` flags.
- Reusable infrastructure: delivery regions/villages module, `SmsService.sendSms(phone, message)`
  (for OTP), `TelegramNotificationService` (staff alerts), `CustomerService.createCustomer`
  (auto-generates customer code), `StockService.getStockByProduct` (availability),
  `ARInvoiceService` (supports zero-price lines).
- Admin dashboard: `frontend/src/views/dashboard/DashboardView.vue` fed by
  `AdminDashboardController`/`AdminDashboardService` (both already covered by unit + full-flow
  tests) — every phase that changes staff-visible state extends these.
- Test conventions (352 existing test files): `@DataJpaTest` repository tests (H2, Flyway off),
  Mockito service/mapper unit tests, standalone-MockMvc controller tests, and
  `@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional` full-flow tests.
- Latest Flyway migration: **V53** — new ones start at V54. *(historical planning baseline — the tree has since moved on, V80+ at last update)*

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
   Conversion to an AR invoice (debt) stays a **manual staff action**.

3. **Customer client = dedicated mobile app (website cancelled).**
   A new **Flutter** app (single codebase for Android + iOS) in a separate `mobile-shop/`
   directory (or its own repo — decide at kickoff). It consumes only the public
   `/api/v1/web/**` API with the X-Tenant-ID header baked into the build config. Guest browsing
   works without login; checkout starts as guest (name + phone), SMS OTP accounts come in
   Phase 4. Distribution: direct APK first, app stores later.

4. **Customer identity is deferred and isolated.**
   Phases 1–3 need no customer login (checkout = name + phone + delivery address, cash on
   delivery / pay-later debt). Phase 4 adds `web_customers` + SMS OTP with tokens that are
   explicitly **not** staff JWTs (separate claim type, never resolved by the staff
   `JwtAuthenticationFilter` into a `UserPrincipal`).

5. **Public API hygiene + app-compatibility discipline.**
   Dedicated public DTOs that whitelist fields (never `costPrice`, `minSellingPrice`, stock
   numbers — only an `inStock` boolean). Tenant scoping in every query. Rate limiting on
   checkout/OTP. Orders record `sourceIp` + `userAgent`. Because installed mobile apps cannot be
   force-updated, the public API must stay **backward compatible** — additive changes only, and
   every endpoint documented in `docs/API.md` as a contract for the app team.

---

## Phase 1 — Curated catalog: backend + admin UI

> **Status: ✅ implemented** (migration V54, `web` module backend, admin “Веб-каталог” page,
> dashboard counters, public catalog API; covered by repository/service/full-flow/security tests).

**Value delivered:** staff can build the item list (draft → live); a public JSON API serves live
items — the contract the mobile app will be built against.

### Backend
| Item | Detail |
|---|---|
| Migration `V54__web_catalog_items.sql` | Table + indexes (tenant, status, sort_order, unique tenant+product); permissions `WEB_CATALOG_VIEW`, `WEB_CATALOG_MANAGE` seeded to ADMIN/SUPER_ADMIN (V48 pattern) |
| Entity `web/entity/WebCatalogItem.java` | extends `TenantAwareEntity`; `@ManyToOne Product`; status enum `WebCatalogStatus { DRAFT, LIVE }` |
| Repository | `findByTenantIdAndStatusOrderBySortOrder`, `existsByTenantIdAndProductId`, paged admin search, live/draft counts |
| Service `WebCatalogService` | add/remove products, bulk add, price/name override, reorder (move up/down like invoice lines), publish/unpublish single + bulk, effective-price fallback |
| Staff controller `/api/v1/web-catalog` (authenticated) | CRUD + publish endpoints guarded by `@RequiresPermission` |
| Public controller `/api/v1/web/catalog` (anonymous) | `GET /products` (paged, search, category filter), `GET /products/{id}`, `GET /categories`; LIVE items only; public DTOs with image URLs, effective price, `inStock` via `StockService.getStockByProduct` |
| Dashboard stats | `AdminDashboardService` extended with `catalogLiveCount` / `catalogDraftCount` |

### Admin frontend (staff app)
- New page “Веб-каталог” (`/web-catalog`): product picker, per-row Draft/Live toggle with status
  badge, price-override input, up/down reorder buttons (same pattern as invoice-line editor),
  bulk publish/unpublish, search. Router + sidebar entry + uz-Cyrl i18n keys.
- **Dashboard:** new stat card on `DashboardView.vue` — “Онлайн каталог: X live / Y draft”,
  linking to the catalog page.

### Tests
- **Repository:** only LIVE returned for storefront query; tenant isolation; sort order respected; unique product-per-tenant constraint; live/draft counts.
- **Service (Mockito):** price fallback (override vs `sellingPrice`); publish/unpublish transitions; reorder swaps; rejects adding inactive/non-sellable products; bulk operations.
- **Controller (standalone MockMvc):** staff CRUD happy paths + validation errors.
- **Full-flow (`WebCatalogFullFlowTest`):** anonymous `GET /api/v1/web/catalog/products` returns 200 with only LIVE items; DRAFT item absent; JSON contains no `costPrice`/stock numbers; staff endpoint returns 401 anonymous / 403 without permission; X-Tenant-ID scoping.
- **Dashboard:** extend `AdminDashboardServiceTest` + `AdminDashboardControllerFullFlowTest` for the new counters.
- **Security:** extend `SecurityConfigIntegrationTest` for the new public path.
- **Manual checklist:** curate 3 products (1 draft), verify public JSON, verify image URLs load via `/uploads/**`, dashboard card shows correct counts.

**Acceptance:** staff curates a list; `curl` of the public endpoint shows exactly the LIVE items
with correct prices and images; dashboard reflects live/draft counts. ~2–3 dev-days.

---

## Phase 2 — Customer mobile app: catalog browsing

> **Status: ✅ implemented** (Flutter app in `mobile-shop/`: catalog grid with search/category
> chips/pagination, product detail with image gallery and phone/Telegram order buttons,
> uz-Cyrl strings, error/empty/offline states; 21 unit+widget tests green, analyzer clean;
> build & release checklist in `mobile-shop/README.md`).

**Value delivered:** customers install the store's app on a phone and see the live item list —
the minimal answer to “item list for mobile app”.

### Work
- Scaffold Flutter app in `mobile-shop/`: API client (base URL + X-Tenant-ID from build config),
  catalog grid screen (search, category filter, price, image, in-stock badge), product detail
  screen, uz-Cyrl strings, pull-to-refresh, offline/error states.
- Guest browsing only — no login. “Order by phone/Telegram” buttons (deep links) until in-app
  checkout ships in Phase 3.
- Build/distribution docs: debug APK for the owner's phone; release signing checklist;
  store submission deferred.
- No admin frontend changes in this phase (catalog management shipped in Phase 1) — stated
  explicitly so the dashboard scope is clear.

### Tests
- **Flutter unit tests:** API client JSON mapping for catalog DTOs; UZS price formatting helper.
- **Flutter widget tests:** catalog grid renders items; empty-catalog state; error/retry state.
- **Manual checklist (documented in repo):** install APK on a real device; draft items invisible;
  out-of-stock badge; images load; behavior on airplane mode.

**Acceptance:** an installable APK shows the live catalog on a phone. ~3 dev-days
(includes one-time Flutter project setup).

---

## Phase 3 — In-app ordering (checkout → staff inbox → invoice)

> **Status: ✅ implemented** (migration V55, WebOrder backend with rate-limited public
> checkout + status lookup + public delivery lookups, Telegram ORDER_PLACED alerts,
> staff inbox page with sidebar NEW badge, dashboard order card + recent-orders widget,
> convert-to-invoice with customer auto-create; Flutter cart/checkout/success/status
> screens; backend + Flutter test suites green).

**Value delivered:** customers purchase from the app without visiting the office; staff confirm
and convert orders into existing flows; the admin dashboard surfaces new orders immediately.

### Backend
| Item | Detail |
|---|---|
| Migration `V55__web_orders.sql` | `web_orders` (order_number, status, customer_name, phone, delivery_region_id/village_id + names, address note, totals, source_ip, user_agent, linked customer_id + ar_invoice_id nullable) + `web_order_lines` (product_id, name snapshot, qty, unit_price snapshot, line_total); permissions `WEB_ORDER_VIEW`, `WEB_ORDER_MANAGE` |
| Entities | `WebOrder` (+`WebOrderStatus`), `WebOrderLine` — lines use the orphanRemoval-safe mutate-in-place pattern (clear + add, never replace the list) |
| Public endpoints | `POST /api/v1/web/orders` (checkout: name, phone, region/village from delivery module, lines), `GET /api/v1/web/orders/{number}?phone=` (status lookup for the app). Validation: max 50 lines, qty 0.001–10 000, products must be LIVE, phone format; prices always taken server-side from catalog |
| Rate limiting | simple in-memory bucket per IP+phone on checkout (e.g. 5/min) — interface designed so a Redis impl can replace it later |
| Staff endpoints `/api/v1/web-orders` | paged list with status filter + NEW count, detail, confirm/cancel (with reason), status updates, **convert-to-AR-invoice** (`ARInvoiceService` + auto-create Customer via `CustomerService` when not linked — code auto-generation already exists) |
| Notifications | on new order: `TelegramNotificationService` alert to staff + optional SMS to owner (reuse `SmsService`), behind tenant settings |
| Dashboard stats | `AdminDashboardService` extended with `newOnlineOrders` count, today's online-order count/total, and a recent-online-orders list for the dashboard widget |

### Admin frontend (staff app)
- **Orders inbox:** new page “Онлайн буюртмалар” — list with status filter, NEW count badge in the
  sidebar item, detail modal (lines, phone, delivery village), confirm / cancel /
  convert-to-invoice buttons, link to the created invoice.
- **Dashboard:** “Янги онлайн буюртмалар” stat card (count, highlighted when > 0, links to inbox)
  + “Сўнгги онлайн буюртмалар” widget listing the latest 5 orders with status chips.
  Stats refresh picks up new orders without manual reload (reuse existing dashboard polling, or
  add a lightweight interval if none).

### Mobile app
- Cart (local storage), checkout screen (name, phone, region→village cascading selects from the
  delivery API, note), success screen with order number, “My order status” lookup screen
  (order number + phone).

### Tests
- **Repository:** order-number generation/uniqueness per tenant; status+tenant filtered queries; NEW count; recent-orders query.
- **Service (Mockito):** checkout rejects DRAFT/unknown products, empty/oversized carts, bad qty; server-side price snapshot (client price ignored); status transition matrix (e.g. cannot confirm a CANCELLED order); conversion builds correct `CreateARInvoiceRequest` (works with zero-price lines) and links customer/invoice ids; Telegram/SMS called once (mocked); rate limiter unit tests (window, reset).
- **Full-flow:** anonymous checkout → 201 with order number; staff list shows it; confirm; convert → AR invoice persisted with matching totals + lines and customer auto-created; cancelled order can't convert; checkout with DRAFT product → 400; rate-limit burst → 429; staff endpoints 401/403 matrix.
- **Dashboard:** `AdminDashboardServiceTest` + full-flow test assert new-order counters and recent-orders payload.
- **Flutter:** unit tests for cart store math (add/remove/qty/total) and checkout payload mapping; widget test for the checkout form validation.
- **Manual checklist:** end-to-end order from a real phone; Telegram message received; dashboard card lights up; invoice visible in debtors flow.

**Acceptance:** an order placed from the app appears on the admin dashboard and in the inbox
within seconds (with Telegram ping) and one click turns it into an AR invoice. ~4 dev-days.

---

## Phase 4 — Customer accounts (SMS OTP) + hardening

> **Status: ✅ implemented** (migration V56, hashed OTP codes with cooldown/daily-cap/attempt
> limits via SmsService, web-customer tokens signed with a derived key strictly separated from
> staff JWTs, /me/orders scoped by verified phone, staff "Онлайн мижозлар" page with AR-customer
> linking that order conversion respects, dashboard online-customers counter, app login +
> "Буюртмаларим" screens with checkout pre-fill; backend + Flutter (44) test suites green).
> Note: app session token is stored in SharedPreferences rather than flutter_secure_storage —
> it only grants access to the customer's own order history.

**Value delivered:** returning customers log in with phone + SMS code inside the app and see
their order history; staff can manage online customers; abuse protections are production-grade.

### Backend
| Item | Detail |
|---|---|
| Migration `V56__web_customers_otp.sql` | `web_customers` (phone unique per tenant, name, verified_at, optional `customer_id` link set by staff), `web_otp_codes` (code hash, expires_at, attempts, cooldown); permissions `WEB_CUSTOMER_VIEW`, `WEB_CUSTOMER_MANAGE` |
| OTP flow | `POST /api/v1/web/auth/request-otp` (SmsService; 60 s cooldown, max 5/day/phone, code hashed, 5-min expiry, 5 attempts) and `POST /api/v1/web/auth/verify` → web-customer token (separate claim `type=web_customer`; staff filter never resolves it as a `UserPrincipal`) |
| Customer endpoints | `GET /api/v1/web/me/orders` (token-scoped); checkout auto-links orders by verified phone |
| Staff endpoints `/api/v1/web-customers` | paged list/search, detail with order history, link/unlink to an AR `Customer` record |
| Hardening | per-IP limiter on OTP; audit log entries for OTP requests |
| Dashboard stats | `AdminDashboardService` extended with registered-online-customers count |

### Admin frontend (staff app)
- New page “Онлайн мижозлар”: list/search of registered app customers, detail (orders, verified
  date), action to link a web customer to an existing AR customer (so debt history connects).
- **Dashboard:** online-customers counter added to the stats grid.

### Mobile app
- Login screen (phone → SMS code), “My orders” tab with statuses, logout; token stored securely
  (flutter_secure_storage) with expiry handling; checkout pre-filled for logged-in customers.

### Tests
- **Service:** OTP issue/verify happy path; expiry; wrong-code attempt limit → locked; cooldown enforced; daily cap; token claims; link/unlink to AR customer.
- **Repository:** phone uniqueness per tenant; lookup of orders by verified phone.
- **Full-flow:** request OTP (SmsService mocked) → verify → fetch own orders only (another phone's orders invisible); web token rejected on staff endpoints and staff JWT rejected on `/web/me/**`; staff customer-management 401/403 matrix.
- **Dashboard:** counter covered in `AdminDashboardServiceTest`.
- **Flutter:** unit tests for auth store (token lifecycle); widget test for OTP input flow.
- **Manual checklist:** real SMS via configured provider on VPS; login from the app; staff links a web customer to a debtor and verifies invoice conversion uses the linked customer.

**Acceptance:** returning customer sees order history after SMS login; staff manage online
customers from the admin app; OTP abuse is throttled. ~3–4 dev-days.

---

## Phase 5 (partially implemented)

> **Status: ✅ stock reservation + delivery fees implemented** (migration V57).
>
> - **Stock reservation on confirm:** confirming an online order reserves each line's
>   quantity (existing `StockService` reservation system, reference type `WEB_ORDER`,
>   location with the most availability). Cancelling or completing the order releases the
>   reservation. Reservation is best-effort — insufficient stock logs a warning but never
>   blocks confirmation. Actual stock deduction still happens when staff record the sale.
> - **Delivery fee per region:** staff set a fee on each delivery region (admin region
>   form); the public regions endpoint exposes it; checkout snapshots it onto the order
>   and includes it in the total; the app shows the fee and grand total before submitting;
>   invoice conversion adds an explicit "Етказиб бериш" line so totals stay equal.

### Remaining items — blocked on external prerequisites

| Item | What it needs from the owner before it can be built |
|---|---|
| Online payment (Payme/Click) | A merchant account with Payme and/or Click: merchant ID, secret keys, registered webhook URL (HTTPS). The integration is webhook-driven (Payme Merchant API is JSON-RPC; Click uses prepare/complete callbacks) and cannot be safely implemented or tested without real credentials and their sandbox access |
| Push notifications (FCM) | A Firebase project: `google-services.json` for the app and a service-account key for the backend. Without these files the app build fails, so the dependency must not be added speculatively. Backend side can mirror the existing `DeviceToken`/`PushNotificationService` pattern once the project exists |
| uz-Latn / ru app locales | A decision to invest: ~80 strings × 2 translations + a locale switcher (the `S` strings class is already the single point of change) |
| Play Store / App Store publication | Developer accounts ($25 Google one-time / $99 Apple yearly), privacy policy URL, store assets; iOS additionally needs a Mac for building |

---

## Cross-cutting

- **Definition of done per phase:** code + migration + tests green (`mvn test`, `flutter test`),
  staff UI i18n'd (uz-Cyrl), **admin dashboard updated wherever the phase introduces
  staff-relevant state**, `docs/API.md` updated for new endpoints (the app team's contract),
  deployed to VPS, manual checklist executed.
- **Key risks & mitigations:**
  - *Installed apps can't be force-updated* → public API changes are additive-only; version field in app config; full-flow tests pin response shapes.
  - *Public endpoint abuse* → rate limiting (Phase 3), SMS cooldowns/caps (Phase 4), payload size caps, server-side prices.
  - *Hibernate orphanRemoval on order lines* → mutate-in-place pattern (already learned on `ARInvoice.lines`).
  - *H2-vs-Postgres drift* → keep entity types simple (no JSONB); full-flow tests cover JPQL.
  - *Price disputes* → order lines snapshot name+price at checkout time.
  - *Tenant leakage* → every public query filters tenant; full-flow tests assert cross-tenant invisibility.
- **Open questions (defaults chosen, easy to change):**
  1. Flutter vs React Native? (default: Flutter — single codebase, good offline story)
  2. `mobile-shop/` directory in this repo or a separate repo? (default: this repo, shares CI and docs)
  3. Distribution: direct APK first, stores later? (default: yes — direct APK to customers via link/QR)
  4. Show retail `sellingPrice` only, or wholesale for logged-in customers? (default: retail only)
  5. Should confirmed orders become AR invoices automatically? (default: manual button)
  6. Delivery fee? (default: none; staff arranges by phone)
