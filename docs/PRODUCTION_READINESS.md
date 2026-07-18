# Production Readiness — Findings & Punch List

**Status: NOT production-ready.** Date of audit: 2026-07-06. Version audited: `0.0.1-SNAPSHOT` (greenfield — no live data yet, which is the only reason the landmines below have not gone off).

This document is the output of an adversarial, code-traced production audit (security, payments,
multi-tenancy, migrations, GL/money correctness, transaction boundaries). Every CRITICAL/HIGH item
was verified against source. Severity reflects **real, reachable production impact**, not theory.

The code compiles and the distribution module is well-tested (90 tests). Several things you'd
expect to be broken are actually solid (see "Verified solid" at the bottom). But there are
confirmed defects that silently corrupt the **general ledger** and **tenant boundaries** — the two
things an ERP cannot get wrong. None are architectural; all are fixable in days.

Each item is written so it can be pasted into a GitHub issue as-is.

---

## 🔴 CRITICAL / HIGH — corrupts money or leaks/misroutes data. Fix before go-live.

### 1. AR ledger silently overstates forever on invoice/payment cancel  ·  CRITICAL  ·  confirmed
- **Where:** `finance/service/ARInvoiceService.java:254` (and `ARPaymentService.java:142`)
- **What:** `glIntegrationService.postARInvoice(invoice)` returns the journal-entry id, but the caller
  **discards it** and never sets `glJournalEntryId` / `glPosted` on the invoice.
  `GLIntegrationService.reverseARInvoice` then hits
  `if (invoice.getGlJournalEntryId() == null) { log.warn(...); return; }` — so it **no-ops**.
- **Scenario:** Post a $10,000 AR invoice → GL debits AR, credits Sales Revenue. Cancel it →
  customer balance reverses but **AR and Sales Revenue stay inflated permanently**. Every void
  drifts the GL from reality.
- **Note:** The AP side does this correctly (`APInvoiceService.java:487` sets `glPosted`). This is an
  omission, not a design choice.
- **Fix:** In `ARInvoiceService.postInvoice` / `ARPaymentService.completePayment`, capture the returned
  entry id and set `glJournalEntryId` + `glPosted` on the entity (mirror the AP path).
- **✅ FIXED (this branch):** both call sites now capture the journal-entry id and set
  `glJournalEntryId` + `glPosted` + `glPostedAt`, so `reverseARInvoice`/`reverseARPayment` actually
  reverse the GL entry on cancel/void instead of no-opping. Regression tests assert the id is stored.

### 2. Multi-tenancy rests on a spoofable header + silent `tenant = 1` fallback  ·  HIGH  ·  confirmed
- **Where:** `common/tenant/TenantFilter.java:45-65`; fallback in `web/service/WebCatalogPublicService.java:85`,
  `WebOrderPublicService.java:154`, `WebPaymentService.java:162`, `WebAuthService.java:216`,
  `web/controller/WebCartPublicController.java:55,78`, `sms/service/SmsTemplateService.java:31`,
  `expense/controller/ExpenseRecordController.java:104`, **`distribution/b2b/service/B2bAuthService.java:34`**.
- **What:** Nothing populates `TenantContext` from a web-customer or B2B token. On the public
  storefront, tenant selection is **only** the client-supplied `X-Tenant-ID` header; when absent,
  every service falls back to `DEFAULT_TENANT_ID = 1L`.
- **Scenario:**
  - Headerless public call → writes orders / mints loyalty / **charges OTP SMS cost to tenant 1**.
  - A logged-in tenant-5 customer whose app omits the header is priced against and orders from
    **tenant 1's** catalog — their token's `tenantId` claim is never consulted for context.
  - Anonymous caller sets `X-Tenant-ID: <n>` and performs writes (checkout, OTP dispatch on that
    tenant's SMS budget) against any tenant.
- **Fix:** Derive tenant from the authenticated customer/B2B token (add a filter that sets
  `TenantContext` from the parsed token), validate the header against the token, and **remove the
  `1L` fallback (fail closed)**.
- **Ownership:** The B2B slice inherited this via `B2bAuthService.resolveTenantId()`.
- **✅ B2B portion FIXED (this branch):** `B2bAuthService.resolveTenantId()` no longer defaults to
  tenant 1 — it throws `400 TENANT_REQUIRED` when `X-Tenant-ID` is absent. Post-login calls were
  already token-bound (`requireCustomer` uses the token's `tenantId`, not the header). **The web
  services' fallback (`WebCatalogPublicService`/`WebOrderPublicService`/etc.) is still open — that
  is pre-existing platform code and remains an open item.**
- **✅ WEB PORTION FIXED (this branch):** `WebCatalogPublicService`, `WebOrderPublicService`,
  `WebAuthService`, `WebPaymentService`, and `WebCartPublicController` no longer fall back to
  `DEFAULT_TENANT_ID = 1L` — each now throws `400 TENANT_REQUIRED` when neither a customer token
  nor `X-Tenant-ID` sets the tenant (fail closed), matching the B2B fix. Authenticated customers
  are unaffected (TenantFilter derives the tenant from the token). **Client requirement:** anonymous
  storefront calls (catalog, checkout, cart price, OTP) MUST send `X-Tenant-ID`; single-tenant
  deployments must set it too. `SmsTemplateService`/`ExpenseRecordController` fallbacks are separate
  non-storefront paths and remain open.

### 3. Anonymous rate-limit bypass → SMS-cost bomb / OTP abuse  ·  HIGH  ·  confirmed
- **Where:** `common/util/ClientIpResolver.java` (`app.security.trust-proxy-headers=true`, takes
  `X-Forwarded-For.split(",")[0]`, the **leftmost**); nginx `docker/nginx/conf.d/default.conf:74` uses
  `$proxy_add_x_forwarded_for` (which **appends**). Limiter call sites: `WebAuthService.java:75`
  (OTP), `WebOrderPublicService.java:57` (checkout), `WebCartPublicController.java:50,73`.
- **What:** The leftmost XFF value is attacker-controlled even behind the proxy, so every per-IP
  limit is defeated by rotating the header.
- **Scenario:** Rotate spoofed IPs + rotate phone numbers → blast OTP SMS at machine speed
  (third-party SMS bombing / SMS-cost abuse). The per-IP throttle contributes nothing; only the DB
  cap (5/phone/day, 60s cooldown) survives. Also corrupts audit-log and login-attempt IPs.
- **Fix:** Use nginx `X-Real-IP` (already set at `default.conf:73`) or take the rightmost/Nth-from-edge
  XFF value instead of the appendable leftmost.
- **✅ FIXED (this branch):** `ClientIpResolver` now prefers `X-Real-IP` (nginx-set, overwrites any
  client value), then falls back to the **rightmost** non-empty `X-Forwarded-For` entry (the real
  peer nginx appends) instead of the spoofable leftmost. Tests cover both.

### 4. Discounted AR invoices post UNBALANCED → hard fail; reaches distribution  ·  HIGH  ·  confirmed
- **Where:** `finance/service/GLIntegrationService.java:681` (debit AR = `totalAmount` = Σlines −
  headerDiscount) vs `:693` (credit Revenue = Σ`lineTotal`). Distribution path:
  `distribution/service/DistributionOrderService.java` `buildInvoiceRequest` forwards the order's
  header `discountAmount` into the AR invoice.
- **What:** Any invoice with `discountAmount > 0` is unbalanced → `JournalEntry.isBalanced()` throws
  → the invoice can never post. A **discounted distribution order becomes an uninvoiceable order.**
- **Fix:** Post the header discount as its own line (contra-revenue/discount account) so debits =
  credits, or reduce the revenue credit by the discount.
- **✅ FIXED (this branch):** `postARInvoice` now credits revenue as `totalAmount − Σ line tax`
  (the total already nets the header discount) instead of summing gross line totals, so the entry
  always balances against the AR debit regardless of discount. Tests assert a discounted invoice
  posts balanced.

### 5. Distribution stock deduction is best-effort and swallows failures → silent inventory loss  ·  HIGH  ·  (this module)
- **Where:** `distribution/service/DistributionStockService.java:29` (`REQUIRES_NEW`);
  `distribution/service/DistributionOrderService.java:451-458` (`deductStock` swallows all exceptions).
  Same swallow in `reserveStock:418-424`, `releaseReservation:434-440`.
- **What:** If deduction fails at delivery (insufficient stock, DB error), the order still goes
  DELIVERED → INVOICED and the customer is billed, but **stock is never decremented — no flag, no
  retry, no dead-letter.**
- **Context:** Built following the web module's "best-effort" convention. For a delivery (a physical
  fact already done) not blocking the status change is defensible, but silently dropping the stock
  movement with no reconciliation is not acceptable for a system of record.
- **Fix:** Either make deduction fail loud (block the transition / surface an error), or record a
  durable "stock adjustment pending" marker for reconciliation. At minimum, alert on the swallow.
- **✅ FIXED (this branch):** `distribution_orders.stock_settled` (V75) now flips to `FALSE` when any
  reserve/release/deduct best-effort op fails, logged at ERROR ("manual reconciliation required")
  and exposed on the order DTO (partial index `idx_dist_orders_stock_unsettled` for querying). The
  delivery still commits (goods physically moved), but the stock gap is durable and queryable
  instead of silently dropped.

### 6. POS → GL posts in `REQUIRES_NEW`, contradicting its own contract → phantom revenue  ·  HIGH
- **Where:** `finance/service/GLIntegrationService.java:929` (`postPOSTransaction`), `:1072`
  (`reverseSalesTransaction`); class Javadoc `:30-34` states GL posting must commit/roll back with
  the caller.
- **What:** If the outer POS transaction rolls back **after** the GL post commits in its own tx →
  a committed GL sales entry for a sale that no longer exists (phantom revenue/COGS).
- **Fix:** Post in the caller's transaction (`REQUIRED`), per the module's own stated contract.
- **⚠️ ASSESSED — deliberately NOT changed (this branch):** the POS path is intentionally
  non-atomic: `completeTransaction` wraps `postPOSTransaction` in a try/catch that swallows GL
  failures ("POS must work even if GL accounts aren't configured"), with `findFailedGlPostings`,
  a `retryGlPosting` endpoint, and `POSRetryScheduler` to post later. Flipping to `REQUIRED` would
  make a transient GL failure mark the shared transaction rollback-only — the swallowed exception
  no longer saves it, so the **entire sale rolls back** (`UnexpectedRollbackException`), losing the
  sale. The correct fix is a transactional outbox (post GL only for committed sales), a larger
  change tracked separately. The narrow phantom-revenue window (GL commits, then the sale rolls back
  on a later step) remains, but is preferable to losing sales on every GL hiccup. AR/AP correctly
  use `REQUIRED` and are unaffected.

### 7. Line tax credited entirely to Sales Revenue; no VAT/QQS liability line  ·  HIGH (VAT jurisdiction)
- **Where:** `finance/service/GLIntegrationService.java:693` — `lineTotal` includes tax, credited to
  revenue; no output-tax liability posted.
- **What:** Revenue overstated, output VAT never booked. Balances (AR debit also includes tax) so it
  posts — money in the wrong account.
- **Fix:** Split the tax portion into a VAT-payable liability credit.
- **✅ FIXED (this branch):** `postARInvoice` now credits `Σ line tax` to `2130 VAT Payable`
  (liability) and revenue only net of tax. `V78__seed_vat_payable_account.sql` guarantees the `2130`
  account exists for every tenant (idempotent), matching the code-based default chart. Tests assert
  output VAT lands in the liability account, not revenue.

### 8. POS card payments auto-approve with no gateway  ·  MEDIUM/HIGH (financial integrity)
- **Where:** `pos/service/POSPaymentService.java:107-108` — `payment.approve()` for **all** payment
  types incl. `CARD`; comment admits "in a real system, card payments would go through a gateway."
- **What:** A card tender is marked APPROVED / driven toward PAID purely on staff-entered
  `authCode`/`cardLastFour` — no authorization ever happens. Reachable only by an authenticated
  staff user (internal-fraud gap, not anonymous). Overpayment/credit-limit checks bound it.
- **Fix:** Integrate a real card gateway, or add an explicit "manual / unverified" tender flag and
  keep it out of the auto-approve path until then.

---

## 🟠 OPERATIONAL — blocks a clean deploy / breaks at scale

### 9. Phantom Flyway auto-repair; checksum-mutated migrations won't self-heal  ·  MEDIUM
- **Where:** `application.yml:30` `repair-on-migrate: true` — **not a real Spring Boot/Flyway
  property** (silently ignored); no `FlywayMigrationStrategy` bean calls `flyway.repair()`.
- **What:** `V62__web_push_referrals.sql` and `V63__web_wishlists.sql` were content-edited after
  first commit (commit `29e7246` added audit columns). Any DB that applied the earlier version has
  old checksums; with `validate-on-migrate: true` and no working repair, the app **won't boot**
  ("migration checksum mismatch"). Greenfield → currently moot, but a live landmine.
- **Fix:** Remove the fake `repair-on-migrate`; add a real `FlywayMigrationStrategy` that calls
  `repair()` then `migrate()`, or accept that a mismatch requires manual `flyway repair`. Confirm
  V62/V63 were never applied at their earlier checksums before the next deploy.
- **✅ FIXED (this branch):** removed the fake `repair-on-migrate` property; added `FlywayConfig`
  with a real `FlywayMigrationStrategy` bean that runs `flyway.repair()` before `migrate()`, so a
  content-edited migration's checksum drift self-heals on boot instead of failing validation.

### 10. Rate limiter is in-memory / per-instance  ·  MEDIUM (compounds #3)
- **Where:** `web/service/InMemoryCheckoutRateLimiter.java` (`ConcurrentHashMap` in JVM heap). Redis
  is provisioned in prod but unused for limiting.
- **What:** N pods = N× every limit (load-balancer dependent). At scale the OTP/checkout throttle is
  near-decorative.
- **Fix:** Move rate limiting to the already-present Redis.

### 11. CORS defaults to `*`  ·  LOW/MEDIUM
- **Where:** `config/SecurityConfig.java:47,112` — `app.cors.allowed-origins` defaults to `*` via
  `setAllowedOriginPatterns`.
- **Mitigating:** `setAllowCredentials(true)` is never called and auth is a stateless `Authorization`
  header (not cookies), so a wildcard origin can't ride a victim's credentials.
- **Fix:** Pin `CORS_ALLOWED_ORIGINS` in prod. Add a guard that forbids `*` patterns if credentials
  are ever enabled (would become CRITICAL).
- **✅ FIXED (this branch):** `corsConfigurationSource` now sets `setAllowCredentials(false)`
  explicitly (making the no-cookies invariant enforced, not incidental) and logs a startup warning
  when a wildcard origin is configured. Pinning `CORS_ALLOWED_ORIGINS` in prod remains an ops step.

### 12. Full test suite not verified green; Postgres migration test Docker-gated  ·  process
- **What:** 402 test classes / 203 full-context `@SpringBootTest`. The distribution module (90 tests)
  is green and everything compiles, but the full suite was **not run** in the audit session. CI
  (`.github/workflows/ci.yml`, `mvnw verify` against a real Postgres service) is the real gate;
  `FlywayMigrationPostgresTest` is `@Testcontainers(disabledWithoutDocker=true)` and is silently
  skipped without Docker.
- **Fix:** Confirm CI is green end-to-end (incl. the Postgres migration test) before any deploy.

---

## ⚪ Lower priority / cosmetic

- **POS→AR proportional line allocation rounding residue** — `finance/service/ARInvoiceService.java:441-462`:
  per-line `creditRatio` at 4dp with no largest-remainder correction, so invoice header ≠ Σlines.
  Not re-posted to GL, so books stay balanced. Cosmetic.
- **GL-integration path skips fiscal-period-close / account-active checks** —
  `finance/service/JournalEntryService.java:318-381` (`createAndPostEntry`) never calls
  `validatePostingAllowed` / `account.isActive()`, unlike the manual `createJournalEntry` path
  (`:98,131-136`). AR/AP/POS can post into a **closed period** or to inactive accounts. (MEDIUM if
  period-close is used operationally.)
- **`/uploads/**` is anonymous static serving** — `config/SecurityConfig.java` PUBLIC_ENDPOINTS.
  Not confirmed vulnerable; flag for a follow-up IDOR / filename-guessability check.
- **JWT enforcement is keyed on the literal `prod` profile** — a deployment that boots a non-prod
  profile would silently run on the published placeholder secret (forgeable JWTs). Deployment
  discipline, not a code bug. (`Dockerfile.prod` / `docker-compose.prod.yml` correctly set `prod`.)

---

## ✅ Verified solid — do NOT spend effort here
- **JWT secret enforcement works** — `auth/security/JwtTokenProvider.java:61-78` throws at startup on
  a default/short secret under the `prod` profile; web + B2B customer tokens derive their keys from
  the same secret, so they're transitively protected.
- **Online payments cannot fake success** — providers 503 by default (`enabled=false` + merchant id
  required); there is **no wired path that marks an online order PAID** (no webhook controller;
  `WebPaymentService.markPaid` has no caller).
- **Actuator locked down in prod** — only `health,info,prometheus`; `health.show-details: never`;
  Swagger off by default. No `env`/`heapdump`/`beans`.
- **PII / OTP not logged** — phones masked, OTP code never logged, device tokens truncated.
- **Migration SQL is clean Postgres-16** — V69–V74 apply cleanly (FK ordering, `ON CONFLICT`,
  `TIME`/`NUMERIC`/`BIGSERIAL` all valid).
- **Zero entity/migration drift in the distribution module**; prod `ddl-auto: validate` refuses to
  boot on drift (a safety net).
- **GL balance guard** — every posting funnels through `JournalEntry.isBalanced()` and throws before
  persist; silently-unbalanced entries are impossible.
- **Distribution's own money math is clean** — proper `BigDecimal`, no `==`, no `double`, no
  unguarded divide.

---

## Recommended fix order (minimum before go-live)
1. #1 — AR/AR-payment GL reversal (store `glJournalEntryId`).
2. #2 — tenant from token; remove the `tenant = 1` fallback.
3. #3 — fix XFF client-IP resolution.
4. #4 + #7 — AR posting: header-discount balance + tax segregation.
5. #5 — distribution stock: fail loud or reconcile (this module).
6. #6 — POS→GL `REQUIRED` transaction.
7. #9 — remove phantom `repair-on-migrate`; verify V62/V63 checksums.
8. #12 — confirm CI green end-to-end.

Items #2 (B2B fallback) and #5 (stock swallow) are in the distribution module built in this work
stream and are **✅ FIXED on this branch** (see the fix notes under each). The rest are pre-existing
platform code (finance/POS/web/config) and remain open.
