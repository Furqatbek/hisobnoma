# Loyalty & Marketing Plan — Online Shop (Mobile App)

Goal: turn the customer mobile app from a passive order channel into a retention and
marketing engine — promotions visible in the catalog, coupons at checkout, targeted SMS
campaigns, a simple cashback loyalty program, and (once Firebase exists) push notifications
and referrals.

Builds on `docs/WEB_SHOP_PLAN.md` (phases 1–5 implemented): web catalog, in-app checkout,
SMS-OTP customer accounts (`WebCustomer`), staff order inbox, stock reservation, delivery fees.

> **Scope change (2026-06-10):** the customer mobile app is now owned by a **separate
> mobile team**. Our deliverables from Phase 2 onward are the **backend (public API +
> staff API), the admin frontend, and documented API contracts** — the "Mobile app
> (mobile team)" sections in each phase describe the expected client behaviour as a
> handoff spec, not our work. The existing Flutter app in `mobile-shop/` (implemented
> through Phase 1) stays in the repo as a working reference implementation of every
> public endpoint; the mobile team can reuse or replace it.

## Codebase context — what we reuse, what's missing

**Already in the platform (POS module, fully tested, NOT used by the online shop today):**

| Piece | Detail |
|-------|--------|
| `Promotion` + `PromotionCondition` + `PromotionAction` | 7 types (PERCENTAGE_OFF, FIXED_AMOUNT_OFF, BUY_X_GET_Y, BUNDLE, TIERED_DISCOUNT, SPEND_X_GET_Y, FREE_ITEM), date/time/day-of-week windows, priority, stackable flag, min order amount, max uses (total + per customer) |
| `PricingService.calculatePrices(PriceCalculationRequest)` | **decoupled from POS Sale entities** — takes arbitrary `{productId, quantity}` items + optional customerId + optional couponCode, returns per-line discounts, applied promotions, grand total. Directly reusable for the web cart |
| `Coupon` + `CouponRedemption` | unique codes (SecureRandom, unambiguous charset), per-customer limits, validity windows; `PromotionService.recordCouponRedemption()` locks the coupon row (FOR UPDATE) against concurrent over-redemption |
| `CouponService.generateCoupons(promotionId, count, request)` | bulk code generation for campaigns |
| `SmsService.sendBulk(templateId, recipients, from)` | template variables per recipient, balance check (200 UZS/SMS), success/failure summary |
| `SmsTemplate` | tenant-scoped templates with `{placeholder}` interpolation |
| Admin UI | `PromotionsView.vue` / promotion form + coupons exist for POS staff |

**Missing (to be built):**
- Web checkout ignores promotions entirely — `WebOrderPublicService.checkout()` snapshots
  `WebCatalogItem.getEffectivePrice()` with no discount logic.
- `CouponRedemption.transaction` references `POSTransaction` — no way to link a redemption
  to a `WebOrder`.
- No customer segmentation, no campaign entity, no loyalty ledger, no referral tracking.
- `Promotion` has no channel flag — every promotion would instantly apply to the web shop,
  which staff may not want (POS-only promos exist).

## Architecture decisions

1. **One promotion engine, channel-scoped.** Do not fork the promotion logic for the web
   shop. Add a `channel` column to `promotions` (`POS` / `WEB` / `ALL`, default `POS` so
   existing behaviour is unchanged) and filter in the existing
   `PromotionService.applyPromotions()` via a channel parameter (default `POS`).
2. **Server computes all discounts at checkout — client shows previews only.** The app may
   call a public "preview cart" endpoint to display discounts live, but `checkout()`
   recomputes everything server-side; client-submitted discount amounts are ignored, same
   rule as prices today.
3. **Identity for promotion limits = `WebCustomer` (phone), not AR `Customer`.** Per-customer
   coupon limits and first-purchase conditions key off `web_customer_id` for web orders.
   `CouponRedemption` gets a nullable `web_order_id` column (alongside the existing nullable
   POS transaction link).
4. **Coupon is redeemed when the order is CONFIRMED, not at checkout.** Checkout only
   *validates* and snapshots the coupon; staff confirmation triggers
   `recordCouponRedemption()` (with the row lock). Cancelling a NEW order never burns a
   coupon; cancelling a CONFIRMED order reverses the redemption (`isReversed`).
5. **Loyalty = simple cashback ledger, not tiers.** Points accrue only when an order reaches
   `COMPLETED` (cash actually received), expire after a configurable number of days, and are
   spent as a checkout payment-like discount. An append-only `web_loyalty_transactions`
   ledger is the source of truth; the balance is `SUM(amount)` of non-expired entries —
   no mutable balance column to drift.
6. **Campaigns are fire-and-forget SMS batches with an audit row.** No scheduler in v1 —
   staff build a segment, preview the recipient count and SMS cost, and send. A
   `web_campaigns` row records segment criteria, template, count, cost, and outcome so
   marketing spend is auditable.
7. **Everything tenant-scoped and feature-flagged.** Loyalty accrual rate, points expiry and
   minimum redemption come from tenant settings (reuse `SystemSetting`), default **off** so
   no tenant gets surprise cashback liabilities.

---

## Phase 1 — Promotions in the online shop

> **Status: ✅ implemented** (channel-scoped promotion engine, catalog sale badges,
> cart price preview endpoint, checkout discounts, usage tracking on confirm/cancel,
> header discount on invoice conversion, admin + Flutter UI)

Customers see sale prices in the catalog and automatic discounts at checkout; staff control
which promotions reach the web channel.

### Backend (as built)

| Item | Detail |
|------|--------|
| Migration `V58__promotion_channel_web_discounts.sql` | `promotions.channel VARCHAR(10) NOT NULL DEFAULT 'POS'` + index `(tenant_id, channel, is_active)`; `web_orders.discount_total NUMERIC(18,4) DEFAULT 0` + `web_orders.applied_promotions VARCHAR(500)` |
| `PromotionChannel` enum + `Promotion` entity | POS / WEB / ALL, default POS; `decrementUsage()` added for cancellation release |
| `PromotionService.applyPromotions(...)` | new overload with explicit `tenantId` + `channel` (the security context is unavailable for anonymous web calls); old signature delegates with channel=POS — POS behaviour unchanged. Repository query `findActivePromotionsForChannels` filters `channel IN (:channel, ALL)` |
| `WebPricingService` (web package) | resolves cart lines against LIVE catalog items (price overrides win as the engine's base price), maps the AR customer linked to the phone's web account (so customer-group / first-purchase conditions work), calls the engine with channel=WEB; discount clamped to subtotal |
| `POST /api/v1/web/cart/price` (`WebCartPublicController`, anonymous) | same `lines` shape as checkout; optional bearer token personalises conditions (phone from token, never from body); rate-limited 5 calls / 10s per IP (time-bucketed key on the shared `CheckoutRateLimiter`); returns lines, subtotal, discountTotal, total, applied promotion **names only** |
| Checkout integration | `WebOrderPublicService.checkout()` snapshots `discountTotal` + comma-joined promotion codes on the order; **line snapshots stay at full price** — the discount lives at order level (the engine computes order-level discounts; distributing them per line would invent numbers). `recalculateTotal()` = lines − discount (floor 0) + delivery. Engine failure never blocks checkout (falls back to undiscounted) |
| Usage tracking | `WebOrderService.updateStatus()`: NEW→CONFIRMED increments usage per applied code (best-effort, missing promo skipped), CONFIRMED/DELIVERING→CANCELLED decrements; COMPLETED never releases |
| Invoice conversion | order discount travels as the **header** `discountAmount` on the AR invoice (`ARInvoiceService.createInvoice` now subtracts header discount from the computed total) — invoice lines stay at full snapshot prices and totals match exactly, no per-line percent rounding |
| Catalog badges | `WebPromotionBadgeService` runs the real engine on a single-unit anonymous cart per product (60s in-memory cache keyed by tenant+item+price); only **PERCENTAGE_OFF** promotions badge (the only per-unit-accurate type); `salePrice` + `promotionLabel` ("-15%") on both the public DTO and the staff `WebCatalogItemDto` |

### Admin frontend

- Promotion form (`PromotionsView.vue` / form): channel selector (Касса / Онлайн дўкон /
  Иккаласи) — one radio group, default Касса.
- Promotions list: channel badge column.
- Web catalog view: show computed sale price next to base price when a WEB promotion targets
  that product, so staff see what the customer will see.
- Web order detail modal: discount lines + applied promotion names; order list shows
  discounted totals.

### Mobile app

- `PublicProduct`: add `salePrice`, `promotionLabel`. Product card and detail show
  struck-through base price + accent sale price + label badge.
- Cart screen: calls `/web/cart/price` (debounced) → shows line discounts and a
  "Чегирма" (discount) row before the total.
- Checkout summary: discount row between subtotal and delivery fee; grand total from server
  preview. Success screen + order status show the discount.

### Tests (as built)

- **Repository:** `PromotionRepositoryTest` — channel filter (WEB query excludes POS,
  POS query excludes WEB, ALL on both), tenant isolation.
- **POS regression:** `PromotionServiceTest`, `PricingServiceTest`,
  `PricingControllerFullFlowTest`, `PromotionControllerFullFlowTest`,
  `ARInvoiceServiceTest` all pass unchanged.
- **Service (Mockito):** `WebPricingServiceTest` (9) — override-aware base price, WEB
  channel, AR-customer resolution by phone, discount clamping, draft/unknown rejection,
  code joining/truncation; `WebPromotionBadgeServiceTest` (8) — percentage-only badges,
  engine-failure safety, TTL cache + price-change bypass; `WebOrderPublicServiceTest` —
  discount snapshot + engine-failure fallback; `WebOrderServiceTest` — usage
  increment/release, COMPLETED keeps usage, header discount on conversion.
- **Full-flow:** `WebPromotionFullFlowTest` (12) — catalog badge per channel, cart preview
  (discount, names, validation, 429 burst), checkout discount + code snapshot, staff order
  fields, confirm/cancel usage round-trip, invoice conversion with matching totals.
- **Flutter (54 total):** salePrice/promotionLabel mapping, `CartPrice.fromJson`,
  `priceCart` HTTP contract, cart screen discount row + fallback-on-failure widget tests.
- **Manual checklist:** create −15% WEB promo in admin → see badge in app → order →
  confirm → numbers match everywhere.

**Implementation note:** per-line `discount_amount` columns from the original sketch were
dropped — the engine computes order-level discounts, so the honest snapshot is an
order-level `discount_total` + promotion codes, with full-price line snapshots everywhere
(web order lines and invoice lines alike).

**Acceptance: met** — a staff-created web promotion is visible in the app and correctly
reduces the order total, with POS promotions untouched.

---

## Phase 2 — Coupons at checkout

> **Status: ✅ implemented** (validate endpoint, checkout snapshot, redemption on
> confirm with web-order audit link, reversal on cancel, channel check, admin UI)

Customer types a coupon code at checkout; per-customer limits enforced; redemption recorded
on confirmation with full audit.

### Backend (as built)

| Item | Detail |
|------|--------|
| Migration `V59__web_coupons.sql` | `coupon_redemptions.web_order_id BIGINT NULL REFERENCES web_orders` + index; `web_orders.coupon_code VARCHAR(50)` + `coupon_discount NUMERIC(18,4) DEFAULT 0` |
| `PromotionService.applyCoupon(...)` | new overload with explicit `tenantId` + `channel`; old signature delegates with POS. A coupon is rejected when its promotion's channel doesn't match (ALL works on both) — WEB coupons can't redeem at the POS terminal and vice versa |
| `WebCouponService` (web package) | resolves the AR customer linked to the phone's web account for per-customer limits (unknown phones get only global limits — self-reported phones are never trusted as identity); collapses every invalid reason into one generic `CouponOutcome.invalid()`; clamps the discount to the cart total |
| `POST /api/v1/web/cart/validate-coupon` (`WebCartPublicController`) | body `{code, lines}` + optional bearer token; prices the cart through `WebPricingService` first so the coupon discounts the post-promotion total; strict 5/min-per-IP rate limit (no time-bucket widening — this is a brute-force target); response `{couponCode, valid, discount}` — provably identical payloads for all invalid reasons (full-flow test asserts it) |
| Checkout | `CheckoutRequest.couponCode` (optional); re-validated server-side against the goods total after automatic promotions; an **invalid coupon rejects checkout with 400** — silently dropping a typed-in discount would surprise the customer on the bill; code + discount snapshotted on the order; `recalculateTotal()` = lines − promo discount − coupon discount (floor 0) + delivery |
| Confirmation | `recordWebCouponRedemption()` (coupon row locked FOR UPDATE): creates a redemption row with `web_order_id`, increments coupon + promotion usage. If the coupon got depleted between checkout and confirm, confirmation still succeeds, the discount is kept, and the redemption row notes the override. A deleted coupon is skipped silently. Best-effort — never blocks confirmation |
| Cancellation (after confirm) | `reverseWebCouponRedemption()`: marks redemption rows reversed (who/when/why), `Coupon.releaseUsage()` decrements uses **and re-activates a coupon depleted only by the released usage**, promotion usage decremented — the customer can use the coupon again |
| Invoice conversion | header `discountAmount` = promotion + coupon discounts combined (`WebOrder.totalDiscounts()`); invoice total still matches the order total exactly |

### Admin frontend

- Coupon form: nothing new (channel comes from the linked promotion).
- Web order detail: coupon code + discount shown; redemption/reversal status.
- Coupons list: redemption rows now show "Онлайн буюртма WO-000012" as source when web.

### Mobile app (mobile team — handoff spec)

- Checkout: "Купон код" field with an "apply" button → calls validate endpoint → shows
  green discount row or inline error ("Купон нотўғри ёки муддати ўтган"). Code is sent with
  the order; success screen shows the coupon discount.

### Tests (as built)

- **Service (Mockito):** `WebCouponServiceTest` (7) — discount mapping/clamping/trim,
  generic collapse of invalid reasons, zero-discount = invalid, AR-customer resolution by
  phone, unknown phone stays anonymous; `WebOrderPublicServiceTest` — coupon snapshot,
  invalid coupon rejects checkout (nothing persisted), no coupon = no coupon-service call;
  `WebOrderServiceTest` — redemption recorded on confirm, redemption failure never blocks
  confirmation, reversal on cancel, no-coupon orders skip it, combined header discount on
  conversion.
- **Full-flow:** `WebCouponFullFlowTest` (11) — validate (valid / generic outcome proven
  identical for unknown vs wrong-channel codes / 5-min 429), checkout discount + totals,
  invalid and POS-channel coupons rejected, redemption row linked to the web order with
  coupon+promotion counters, cancel reverses and the coupon becomes usable again
  (max-uses=1 round-trip), per-customer limit blocks a second use for the linked customer
  while an unlinked phone passes, staff order fields, invoice conversion totals.
- **Manual checklist:** bulk-generate 10 coupons, use one twice from the same login (second
  rejected), cancel a confirmed couponed order and verify the coupon is usable again.

**Implementation note:** invalid coupons at checkout return 400 (the codebase's
`ValidationException` convention), not the 422 sketched originally.

**Acceptance: met** — a coupon distributed by staff can be redeemed exactly per its limits
from the app, with a clean audit trail.

---

## Phase 3 — Customer segments + SMS campaigns

> **Status: ✅ implemented** (5 segments, mandatory cost preview, async single-blast
> send, per-recipient personal coupons, opt-out, admin UI + dashboard widget)

Staff pick a customer segment ("hasn't ordered in 30 days"), attach an SMS template and
optionally a coupon batch, preview cost, send, and see the outcome.

### Backend (as built)

| Item | Detail |
|------|--------|
| Migration `V60__web_campaigns.sql` | `web_campaigns` (segment_type, segment_param NUMERIC, sms_template_id, promotion_id NULL, status, recipient/sent/failed counts, cost_estimate, failure_reason, sent_at); `web_customers.sms_opt_out`; permissions `WEB_CAMPAIGN_VIEW/MANAGE` → ADMIN/SUPER_ADMIN |
| `WebCampaign` + `WebCampaignStatus` (DRAFT/SENDING/SENT/FAILED) + `WebSegmentType` enums | single numeric `segmentParam` (days or amount) rather than JSON — v1 has one param |
| Segment queries (`WebCustomerRepository`) | `segmentAll`, `segmentOrderedSince`, `segmentNoOrderSince`, `segmentNeverOrdered`, `segmentMinSpent` — JPQL over web customers joined to orders by normalized phone; **every query excludes `smsOptOut = true`** |
| `WebCampaignService` | `preview(id)` → count + `count × 200 UZS` + balance (`SmsService.getBalanceAmount()`, unknown balance treated as sufficient); `send(id)` → resolve segment, reject empty, `{coupon}`-without-promotion guard, **balance block before going async**, generate one single-use coupon per recipient via `CouponService.generateCoupons`, set SENDING, hand off to the dispatcher. Only DRAFT is sendable (no double-blast) |
| `WebCampaignDispatcher` | separate bean so the `@Async` proxy applies (self-invoked async would run sync); sets `TenantContext` explicitly (not propagated to async threads) for template resolution; `sendBulk` → finalize SENT (any success, failed count recorded) / FAILED (all failed or exception) with a `failureReason` |
| `WebCampaignAdminController` `/api/v1/web-campaigns` | CRUD drafts, `POST /{id}/preview`, `POST /{id}/send`, all `@RequiresPermission` |
| Opt-out | `WebCustomer.smsOptOut`; `POST /web-customers/{id}/sms-opt-out`; `WebCustomerDto` gains `smsOptOut` + `lastOrderAt` |

**Context note (gotcha solved):** the service uses `SecurityContextHelper` for the tenant
but the SMS template/balance helpers read `TenantContext`. To avoid the two diverging in
tests (where only the principal is set), template lookups go through
`SmsTemplateRepository` with an explicit tenant; only the async dispatcher relies on
`TenantContext`, which it sets itself.

### Admin frontend

- New sidebar entry "SMS кампаниялар" under the web shop group.
- `WebCampaignsView.vue`: list with status/counters; create form = name + segment type
  (select with parameter inputs) + SMS template select (link to existing template management)
  + optional promotion for personal coupons; **preview step is mandatory** — shows recipient
  count, cost in UZS, current SMS balance, and a red warning if balance is insufficient;
  send button with confirm dialog.
- `WebCustomersView.vue`: opt-out toggle + last-order column (helps eyeball segments).
- Dashboard: "last campaign" mini-widget (name, sent/failed).

### Mobile app (mobile team — handoff spec)

- No app changes (SMS arrives out-of-band). Coupon codes from campaigns work via Phase 2.

### Tests (as built)

- **Repository:** `WebCustomerSegmentTest` — all 5 segments partition seeded
  customers+orders correctly (recent vs sleeping vs never-ordered vs min-spend),
  opted-out excluded, tenant isolation; uses a backdated `created_at` to age orders.
- **Service (Mockito):** `WebCampaignServiceTest` (13) — preview count/cost/balance
  sufficiency (incl. unknown balance), send transitions to SENDING + dispatch payload,
  one coupon per recipient injected as `{coupon}`, `{coupon}`-without-promotion rejected,
  insufficient balance blocks before dispatch, empty segment rejected, non-draft rejected,
  segment routing, param + POS-channel-promotion validation.
- **Dispatcher (Mockito):** `WebCampaignDispatcherTest` (4) — all-sent → SENT, partial →
  SENT with failed count, all-failed → FAILED, exception → FAILED with reason.
- **Full-flow:** `WebCampaignFullFlowTest` (8) — permission matrix (anonymous 403, view-only
  403 on create), create starts DRAFT, preview excludes opted-out + computes cost, send →
  SENDING and second send rejected, empty segment rejected, segment-param validation, list.
- **Manual checklist:** real campaign to 2 test phones with personal coupons; redeem one via
  the app; opt a customer out and confirm exclusion from the next preview.

**Implementation note:** async finalization (SENT/FAILED counts) runs in a separate
transaction the `@Transactional` full-flow test can't observe — same constraint as stock
reservation — so it's covered by `WebCampaignDispatcherTest` instead.

**Acceptance: met** — staff can run a targeted, costed, consent-respecting SMS campaign with
trackable coupon redemptions, without leaving the admin.

---

## Phase 4 — Loyalty cashback

> **Status: ✅ implemented** (append-only ledger, earn on COMPLETED, spend at checkout,
> reversal on cancel, nightly expiry, manual ADJUST, tenant settings, staff + public
> endpoints, admin UI with ledger/adjustment, dashboard liability counter)

Customers earn X% of completed orders as points and spend them at checkout. Logged-in
customers only (points need an identity).

### Backend (as built)

| Item | Detail |
|------|--------|
| Migration `V61__web_loyalty.sql` | `web_loyalty_transactions` append-only ledger (tenant_id, web_customer_id, web_order_id NULL, type EARN/SPEND/EXPIRE/ADJUST, amount signed NUMERIC(18,4), expires_at, note, created_by, created_at); indexes on `(tenant_id, web_customer_id)` and `(web_order_id)`; unique partial index `uk_wlt_earn_per_order` on `(tenant_id, web_order_id, type) WHERE type = 'EARN'` for idempotent earning; `web_orders.points_spent NUMERIC(18,4) DEFAULT 0`; permissions `WEB_LOYALTY_VIEW/MANAGE` → ADMIN/SUPER_ADMIN |
| Tenant settings (`TenantSetting`) | `loyalty.enabled` (default false), `loyalty.earn_percent` (default 0), `loyalty.expiry_days` (default 180), `loyalty.min_redeem` (default 5000), `loyalty.max_redeem_percent_of_order` (default 50) — all read via `TenantSettingService` with system fallback |
| `WebLoyaltyService` | `balance(tenantId, customerId)` = `SUM(amount)` of non-expired entries; `earn(order)` on →COMPLETED: `earn_percent × (total − delivery_fee)`, idempotent (unique index catches double-complete), expiry date set; `spend(tenantId, customerId, orderId, requested, goodsTotal)` at checkout: clamps to balance, max-percent-of-order cap, min-redeem threshold; `reverseOrder(order)` on CANCELLED: writes ADJUST reversals for all EARN/SPEND rows on that order; `adjust(customerId, request)` staff manual adjustment with reason + audit trail; `expirePoints()` nightly `@Scheduled(cron="0 0 2 * * *")`: zeros out expired earn batches |
| Checkout | `CheckoutRequest.pointsToSpend` (optional BigDecimal); `WebOrderPublicService.applyLoyaltyPoints()` resolves web customer by phone, calls `spend()`, snapshots `points_spent` on the order; `recalculateTotal()` = lines − promotions − coupon − points (floor 0) + delivery; best-effort — loyalty failure never blocks checkout |
| `WebOrder` entity | added `pointsSpent` field; `recalculateTotal()` subtracts it; `totalDiscounts()` includes it (so invoice conversion stays correct) |
| `GET /api/v1/web/me/loyalty` (auth) | balance + last 20 ledger entries with order numbers, enabled flag, min/max settings |
| Staff `GET /api/v1/web-customers/{id}/loyalty` | same shape; `POST /{id}/loyalty/adjust` with `{amount, reason}` — permission-gated `WEB_LOYALTY_MANAGE` |
| Dashboard | `loyaltyLiability` field = `SUM(amount)` of all non-expired entries across the tenant |
| Order lifecycle | COMPLETED → `earnLoyaltyPoints()` (best-effort); CANCELLED → `reverseLoyaltyPoints()` (best-effort) |

### Admin frontend (as built)

- Web customer table: loyalty icon button per row opens the loyalty modal.
- Loyalty modal: balance card (purple), manual adjustment form (amount + reason), scrollable ledger with type badges (EARN green, SPEND red, EXPIRE gray, ADJUST blue) and order number links.
- Web order detail: purple "Балл сарфланди" row for orders with points_spent > 0.
- Dashboard: `loyaltyLiability` counter in the online-shop stat group.
- Settings: loyalty configuration via the existing tenant settings page (5 keys under `loyalty.*`).

### Mobile app (mobile team — handoff spec)

- Profile tab (logged in): points balance card + ledger list ("Кешбек: 12 500 сўм").
- Checkout (logged in, balance ≥ min_redeem): "Баллардан фойдаланиш" row with a slider or
  amount field, clamped client-side to the cap; totals update; success screen shows points
  spent and (later, when completed) earned.
- Order status/my-orders: show points earned on completed orders.

### Tests (as built)

- **Service (Mockito):** `WebLoyaltyServiceTest` (13) — balance enabled/disabled, spend
  clamps to balance+percent cap, spend below min-redeem returns zero, spend disabled/zero
  no-ops, earn excludes delivery fee, earn idempotent (DataIntegrityViolation caught), earn
  skips when no web customer, earn disabled skips, reverseOrder creates ADJUST reversals,
  manual adjust with audit trail.
- **Full-flow:** `WebLoyaltyFullFlowTest` (8) — permission matrix (anonymous 403, view-only
  GET OK, manage-only 403 on adjust), adjust creates ADJUST entry with correct balance,
  negative adjust works, ledger shows all transaction types with correct balance, tenant
  isolation (unknown customer 404), disabled loyalty shows disabled state.
- **Manual checklist:** enable loyalty in tenant settings → place + complete an order →
  balance appears in customer card → spend on second order → cancel confirmed → points
  returned; settings off hides the balance.

**Implementation note:** earn base = `total − deliveryFee` (not subtracting points_spent
from earn base, because the total already has points subtracted — so the earn is on the
actual paid amount). The nightly expiry job zeros out the earn row's amount and writes a
matching EXPIRE row so the ledger stays consistent (the balance query filters by
`expires_at > now` anyway, but the EXPIRE row provides the audit trail).

**Acceptance: met** — with loyalty enabled, a completed order produces spendable, expiring
cashback whose every movement is visible in an auditable ledger.

---

## Phase 5 — Push notifications + referrals

> **Status: planned — push blocked on a Firebase project**

### Backend — Push (FCM) — prerequisite: Firebase project (`google-services.json` + service-account key)

| Item | Detail |
|------|--------|
| Migration `V62__web_push_referrals.sql` | `web_device_tokens` (tenant_id, web_customer_id, token, platform, created_at, last_seen_at, unique token per tenant); `web_customers ADD COLUMN referral_code VARCHAR(12) UNIQUE, referred_by BIGINT NULL REFERENCES web_customers(id)` |
| `WebPushService` | mirrors the existing `DeviceToken`/`PushNotificationService` pattern; `register(webCustomerId, token, platform)` (upsert, replaces stale token for the same device); `sendToCustomer(...)` removes tokens FCM reports as invalid; fan-out on order status changes (CONFIRMED/DELIVERING/COMPLETED) — fire-and-forget, never blocks the status update |
| `POST /api/v1/web/me/device-token` (auth) | register/refresh token; `DELETE` on logout |
| Campaign channel | `web_campaigns` gains `channel` (SMS / PUSH / PUSH_WITH_SMS_FALLBACK); `WebCampaignService.send()` pushes first, falls back to SMS for recipients without a live token; cost preview = SMS count after fallback split |

### Backend — Referrals

| Item | Detail |
|------|--------|
| `WebReferralService` | `getOrCreateCode(webCustomerId)` (SecureRandom, unambiguous charset); `applyCode(newCustomerId, code)` at OTP verify time — rejects self-referral (same normalized phone), rejects if `referred_by` already set; `rewardIfFirstCompletion(order)` hooked into the →COMPLETED transition: exactly-once (guarded by ledger ADJUST idempotency per referred customer), writes loyalty ADJUST for both sides from settings `referral.reward_referrer` / `referral.reward_referred`, respects per-referrer monthly cap (setting `referral.monthly_cap`) |
| `VerifyOtpRequest` | gains optional `referralCode` |
| Settings (`SystemSetting`) | `referral.enabled` (default false), reward amounts, monthly cap |

### Admin frontend

- Settings page: "Реферал дастури" section — enable toggle, both reward amounts, monthly
  cap (next to the Phase 4 loyalty section).
- Campaign form (`WebCampaignsView.vue`): channel selector SMS / Push / Push+SMS-fallback;
  preview shows push-vs-SMS split and the resulting SMS cost.
- Web customer card (`WebCustomersView.vue`): referral code, who referred this customer
  (link), count of successfully referred customers, device-token presence indicator
  ("push реachable" badge) so staff know which channel will reach them.
- Web order detail: no change (referral rewards appear in the Phase 4 loyalty ledger).
- Dashboard: referred-customers counter added to the online-shop stat group.

### Mobile app (mobile team — handoff spec; Firebase project shared with backend)

- `firebase_messaging` dependency; token registration after login, deletion on logout;
  notification tap → order status screen (deep link by order number).
- Profile tab: "Дўстингизни таклиф қилинг" card — referral code with share sheet
  (`share_plus`), short explainer of both-side rewards.
- Login screen: optional referral code field on the OTP verify step (pre-filled when the
  app was opened from a referral link).

### Tests

- **Repository:** token upsert/unique per tenant; referral code uniqueness; referred-by
  lookup; tenant isolation.
- **Service (Mockito):** `WebPushService` — register replaces stale token, invalid-token
  cleanup on FCM error, status-change fan-out never throws into the order transition;
  campaign fallback split (token vs no-token recipients) and its cost math;
  `WebReferralService` — self-referral rejected, second `applyCode` rejected, reward
  exactly-once on first completion (double-complete safe), no reward on later orders,
  monthly cap enforced, disabled flag = all no-ops.
- **Controller (standalone MockMvc):** device-token endpoints require auth (401 anonymous);
  verify-otp accepts/ignores referral code; campaign channel validation.
- **Full-flow (`@SpringBootTest`):** customer A gets code → customer B registers with it →
  B's first order completes → both ledgers show ADJUST rewards → B's second order adds
  nothing; status change creates push payload (fake FCM client); permissions matrix on new
  staff endpoints.
- **Manual checklist (with mobile team):** real push on a physical device for each status change; full referral
  loop between two phones; campaign with push channel falls back to SMS for a customer
  without the app.

**Acceptance:** status changes reach the customer's lock screen; a referral measurably
creates a rewarded second customer. (~2 days once Firebase exists)

---

## Phase 6 — Wishlists ("like") + price/restock alerts

> **Status: planned** — 6a (likes, wishlist screen, staff insights) has no external
> prerequisites and can ship right after Phase 1; 6b (alerts) prefers Phase 5 push,
> with an SMS fallback behind a paid, off-by-default tenant setting.

Customers tap a heart on any product; their wishlist lives in the profile tab. When a
wishlisted product gets a web discount or comes back (in stock again / re-published),
the system notifies them — turning the wishlist into a self-building remarketing list.
Staff see which products people want, which is exactly the list worth discounting.

### 6a — Backend: wishlist mechanics

| Item | Detail |
|------|--------|
| Migration `V63__web_wishlists.sql` | `web_wishlist_items` (tenant_id, web_customer_id → web_customers, catalog_item_id → web_catalog_items, created_at, `last_known_available BOOLEAN`, `last_notified_sale_price NUMERIC(18,4) NULL`, `notified_at TIMESTAMP NULL`); unique `(web_customer_id, catalog_item_id)`; index `(tenant_id, catalog_item_id)` for like counts |
| `WebWishlistService` | `toggle(webCustomerId, catalogItemId)` (idempotent like/unlike, item must exist for the tenant — DRAFT items allowed, they may come back); `getWishlist(...)` paged, each entry rendered through the existing public catalog mapping (price, salePrice/promotionLabel via `WebPromotionBadgeService`, inStock, image) plus an `available` flag (LIVE + active + sellable); `likeCount(catalogItemId)` for staff |
| Endpoints (web-customer auth, `/api/v1/web/me/wishlist`) | `GET` (paged), `PUT /{catalogItemId}` (like), `DELETE /{catalogItemId}` (unlike), `GET /ids` (lightweight id list so the app can paint hearts on the catalog grid without N calls). Wishlists require login — anonymous users are prompted to sign in when tapping the heart |
| Catalog DTO | `PublicCatalogProductDto` stays unchanged (hearts are painted client-side from `/ids`) — keeps the hottest endpoint cacheable and identical for all users |
| Staff insight | `WebCatalogItemDto.likeCount`; `GET /api/v1/web-catalog/most-wished` (top N with like counts, `WEB_CATALOG_VIEW`) |

### 6b — Backend: alerts (discount + back-in-stock)

| Item | Detail |
|------|--------|
| `WebWishlistAlertJob` (`@Scheduled`, e.g. every 30 min, per tenant) | loads distinct wishlisted catalog items, computes current state (salePrice via badge service, availability via the same LIVE/active/sellable+stock rules as the catalog): **discount alert** when salePrice exists and differs from `last_notified_sale_price` (dedupe — one alert per price level, never re-alert the same discount); **restock alert** when `available` flips false→true vs `last_known_available`. Updates the snapshot columns in the same transaction |
| Delivery channels | Phase 5 push when a device token exists (free, default); else SMS **only if** tenant setting `wishlist.sms_alerts_enabled` (default false — 200 UZS each adds up) with a per-customer daily cap (`wishlist.max_sms_per_day`, default 1); in-app fallback always: the wishlist screen shows a "нарх тушди" badge on changed items |
| Message content | uz-Cyrl template: product name + old→new price ("Coca-Cola 1.5л энди 10 200 сўм (-15%)") or "яна сотувда"; deep link to the product (push payload carries catalogItemId) |
| Safety | job failures are logged and never affect catalog/checkout; batch size capped; alerts always best-effort |

### Admin frontend

- Web catalog view: likes column (sortable) — the most-wished products are the discount
  shortlist.
- Dashboard: "Энг кўп исталган" (most wished) mini-widget, top 5 with like counts.
- Settings: wishlist SMS alerts toggle + daily cap (next to the loyalty/referral sections).
- Web customer card: wishlist count + expandable list of their liked items.

### Mobile app (mobile team — handoff spec)

- Heart icon on the product card and product detail (filled when liked); anonymous tap →
  login screen, then completes the like.
- Hearts painted from a cached `/ids` fetch after login (refreshed on app resume).
- Profile tab: "Севимлилар" (favorites) entry → wishlist screen — product cards with
  current price/sale badge, out-of-stock and unavailable states, unlike via heart, tap →
  product detail; "нарх тушди" change badges from 6b.
- Push tap (Phase 5) deep-links to the product detail screen.

### Tests

- **Repository:** unique like constraint, like counts per item, tenant isolation,
  cascade behaviour when a catalog item is deleted.
- **Service (Mockito):** toggle idempotency; wishlist rendering includes salePrice and
  availability; anonymous → 401; alert job — discount dedupe (same price never re-alerts,
  deeper discount re-alerts), restock flip detection, snapshot update, SMS cap + disabled
  flag = no SMS, push preferred over SMS, job failure isolation.
- **Controller (standalone MockMvc):** auth required on all wishlist endpoints; `/ids`
  shape; staff endpoint permission matrix.
- **Full-flow:** login → like → appears in wishlist with badge data → staff sees like
  count → create WEB promo → run job → alert recorded/dedup on second run → unlike →
  gone; second customer's wishlist untouched (tenant + customer scoping).
- **Manual checklist (with mobile team):** like on two devices/accounts; publish a −15% WEB promo → push/SMS
  arrives once, not again on the next job run; sell out and restock a product → restock
  alert; unlike stops alerts.

**Acceptance:** a customer who liked a product gets exactly one notification when it goes
on sale or returns, and staff can see the most-wanted products. (6a ≈ 1 day, 6b ≈ 1–1.5
days once a notification channel exists.)

---

## Cross-cutting

**Definition of done per phase** (mobile app excluded — owned by the mobile team since
Phase 2): code + migration + full backend suite green + admin uz-Cyrl i18n + dashboard
touched where relevant + **public API contract documented in `docs/API.md`** (endpoints,
request/response shapes, error codes — this is the mobile team's interface) + this doc's
status flipped + backend/admin manual checklist run against local stack.

**Risks / notes**

| Risk | Mitigation |
|------|-----------|
| Promotion preview endpoint becomes a hot path | 60s catalog-promo cache; preview endpoint rate-limited; `PricingService` is read-only transactional |
| Coupon brute-forcing via public validate endpoint | aggressive rate limit (reuse `CheckoutRateLimiter`), generic errors, 10-char SecureRandom codes ≈ 32^10 space |
| Points liability surprises the owner | dashboard liability counter, expiry default 180d, max-redeem-percent cap, feature off by default |
| Double SMS blast / wasted budget | mandatory preview with cost + balance, single-send state machine on campaigns |
| Existing POS promos leaking into the app on deploy | `channel` default `POS` in the migration — web exposure is always an explicit staff action |
| Ledger drift | no balance column anywhere; balance is always an aggregate of the append-only ledger; manual changes only via audited ADJUST |
| Wishlist alert spam / SMS cost runaway | one alert per price level (dedupe on `last_notified_sale_price`), SMS channel off by default + per-customer daily cap, push preferred when available |

**Suggested order:** 1 → 2 → 3 are sequential (each reuses the previous), 4 is independent
after 1, 6a is independent after 1 (good filler while waiting on Firebase), 5 then 6b last.
Total ≈ 10–12 working days excluding Firebase setup.

**Open questions for the owner**
1. Earn rate and expiry for cashback (proposal: 1%, 180 days, min redeem 5 000 сўм)?
2. Should campaign SMS sender ID differ from OTP sender?
3. Referral reward amounts?
4. Do POS and web coupons share budgets (max uses), or should campaigns always use
   dedicated coupon batches? (Plan assumes dedicated batches.)
