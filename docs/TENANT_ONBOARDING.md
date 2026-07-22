# Tenant Onboarding — Step by Step

How to bring a new warehouse owner (tenant) onto a running platform. Isolation model:
[`MULTI_TENANCY.md`](MULTI_TENANCY.md). Server setup: [`LAUNCH_PRODUCTION.md`](LAUNCH_PRODUCTION.md).

> **Preferred path — one API call.** Steps 1–4 below are automated by the provisioning endpoint
> (permission `TENANT_PROVISION`, seeded to SUPER_ADMIN):
>
> ```
> POST /api/v1/admin/tenants
> {
>   "name": "Acme Warehouse", "code": "ACME",
>   "adminUsername": "acme-admin", "adminPassword": "<8+ chars>",
>   "adminPhone": "+99890…", "fiscalYear": 2026
> }
> ```
>
> One transaction creates the tenant, seeds its default chart of accounts, creates the first
> ADMIN user, and opens the fiscal year with 12 monthly periods — a half-provisioned tenant
> cannot exist. The response returns the new `tenantId` (the storefront's `X-Tenant-ID`).
> Continue at **step 5**. The manual steps below remain as the fallback/reference.
> (Customer self-service signup is still a SaaS-roadmap item — MULTI_TENANCY.md §9.)

## 1. Create the tenant row (SQL, platform operator)

```sql
INSERT INTO tenants (name, code, description, active, timezone, currency, locale,
                     max_users, max_locations)
VALUES ('Acme Warehouse', 'ACME', 'Acme Warehouse LLC', TRUE,
        'Asia/Tashkent', 'UZS', 'uz', 25, 5)
RETURNING id;   -- note the id: it is the X-Tenant-ID for this owner's storefront
```

## 2. Seed the tenant's GL accounts (SQL, platform operator)

The per-tenant chart-of-accounts seeds ran at migration time (`INSERT … SELECT FROM tenants t
WHERE NOT EXISTS …`), so a tenant created afterwards has **no GL accounts** — and AR/POS posting
fails without them. Re-run the seed statements from these migrations verbatim (they are
idempotent — existing tenants are skipped, only the new tenant gets rows):

- `V35__seed_gl_integration_accounts.sql` (1130 AR, 1140 Inventory, 2110 AP)
- `V36__seed_remaining_gl_accounts.sql` (1110 Cash, 4100 Sales Revenue, 4200 Sales Discounts, 5100 COGS, …)
- `V24__sales_discounts_account.sql`
- `V78__seed_vat_payable_account.sql` (2130 VAT Payable)

Verify: `SELECT code, name FROM accounts WHERE tenant_id = <id> ORDER BY code;` — expect at
least 1110, 1130, 1140, 2110, 2130, 4100, 4200, 5100.

## 3. Create the tenant's admin user

As the platform super-admin, create the owner's first staff user (admin UI → Админ →
Пользователи, or `POST /api/v1/auth/users`) with the **ADMIN** role and the new tenant's id.
Hand over the credentials; they change the password on first login. All remaining steps are
done **logged in as this tenant admin** (the tenant comes from their JWT).

## 4. Open a fiscal year (required before any sale posts to GL)

GL posting refuses dates without an open fiscal period. Create the year with auto-generated
monthly periods:

```
POST /api/v1/fiscal-periods/years        { "year": 2026, "generatePeriods": true }
```

## 5. Master data

1. **Warehouse/locations** — Inventory → Warehouses (respect `max_locations`).
2. **Units of measure, categories, brands** — Inventory → справочники.
3. **Products** — manually or CSV/Excel import (`POST /api/v1/inventory/products/import`);
   set selling prices; receive opening stock (Receiving) so quantities are non-zero.
4. **Price lists / promotions / coupons** — POS module, as needed.

## 6. Point of sale

1. Create a POS terminal (POS → Терминалы).
2. Cashier opens a shift (`POST /api/v1/pos/shifts/open` or the POS screen) — sales can begin.

## 7. Online shop (optional per tenant)

1. **Delivery** — create regions (+fees) and villages (Delivery module); the checkout form
   depends on them.
2. **Catalog** — publish products to the web catalog (Веб-каталог): only **LIVE** items are
   visible to shoppers.
3. **Loyalty & referral** — the admin UI's **Лояллик дастури** page (typed form: cashback %,
   redeem caps, expiry, referral rewards). Defaults are seeded disabled (V82); enabling is a
   toggle. Other tenant settings (e.g. `wishlist.sms_alerts_enabled`) via the generic settings
   editor.
4. **App build** — the customer Flutter app is built per shop with
   `--dart-define=SHOP_TENANT_ID=<id>` (plus the server URL); every storefront request sends
   that id as `X-Tenant-ID`. Without it the API answers `400 TENANT_REQUIRED`.
5. Note: customer OTP login needs platform-level `SMS_ENABLED=true` + token.

## 8. B2B wholesale buyers (optional)

Create each buyer as a finance **Customer** (Финансы → Клиенты) with a customer **code**,
**phone**, credit limit and payment terms. Buyers authenticate against
`POST /api/v1/b2b/auth/login` with **code + phone** (no password) and the shop's `X-Tenant-ID`
— then browse `/b2b/catalog` and place orders on credit terms.

## 9. Staff notifications (optional)

- **Telegram** — each staff member links their chat via the bot (`/api/v1/telegram`), then
  enables alert types in the mobile app preferences.
- **APNs push** — staff devices register via the admin mobile app (needs platform `APNS_*`
  configured; see [`api/MOBILE_PUSH_API.md`](api/MOBILE_PUSH_API.md)).

## 10. Verification checklist

- [ ] `X-Tenant-ID: <id>` + `GET /api/v1/web/catalog/products` returns the tenant's LIVE items
      (and **only** theirs)
- [ ] Tenant admin logs in and sees empty-but-working dashboards (no other tenant's data)
- [ ] A test POS sale completes; within a minute the transaction shows `glPosted: true`
      (fiscal year missing → it stays false and the retry log complains)
- [ ] A test online order lands in Онлайн буюртмалар and staff get the new-order alert
- [ ] `SELECT count(*) FROM accounts WHERE tenant_id = <id>` > 0 (step 2 done)
- [ ] Remove/close the test data

## Common pitfalls

| Symptom | Cause |
|---|---|
| POS sale completes but `glPosted` stays `false`; retry log: "No fiscal period found" | Step 4 skipped |
| AR/POS posting: "Account not found with code: …" | Step 2 skipped |
| Storefront returns `400 TENANT_REQUIRED` | App built without `SHOP_TENANT_ID` / header missing |
| Customer OTP login silent | Platform `SMS_ENABLED=false` (code is generated but not sent) |
| B2B login fails | Customer code/phone mismatch — login is code + phone, not password |
