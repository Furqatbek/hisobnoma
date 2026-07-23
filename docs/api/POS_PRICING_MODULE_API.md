# POS Pricing & Promotions API

Companion to [`POS_MODULE_API.md`](POS_MODULE_API.md) (which covers terminals, shifts,
transactions, and payments). This document covers the four pricing/marketing sub-modules under
`/api/v1/pos`:

- **Promotions** — rule-based discounts (percentage, fixed, buy-X-get-Y, …) with conditions/actions.
- **Coupons** — single- or multi-use codes that unlock a promotion.
- **Price lists** — per-customer / per-tier product pricing (wholesale, VIP, …).
- **Pricing** — the calculation engine the POS/checkout calls to resolve prices and apply coupons.

## Conventions

- **Base path:** `/api/v1/pos`
- **Auth:** staff JWT (`Authorization: Bearer <token>`); every endpoint is guarded by a
  `@RequiresPermission("POS_…")` authority (listed per endpoint below).
- **Tenant:** taken from the staff JWT; all data is tenant-scoped.
- **Envelope:** these controllers return the DTO (or Spring `Page`) directly — **not** wrapped in
  `ApiResponse`. Paged lists are Spring `Page<T>` (`{ content, totalElements, totalPages, number, size }`);
  pass `?page=&size=&sort=` as usual.
- **Money** is `BigDecimal`; dates are ISO (`yyyy-MM-dd`), times `HH:mm`.

---

## Promotions  ·  `/api/v1/pos/promotions`

| Method | Path | Permission | Purpose |
|---|---|---|---|
| GET | `/promotions` | `POS_PROMOTION_READ` | List (paged) |
| GET | `/promotions/search?query=` | `POS_PROMOTION_READ` | Search by code/name (paged) |
| GET | `/promotions/active` | `POS_PROMOTION_READ` | Currently-active promotions |
| GET | `/promotions/{id}` | `POS_PROMOTION_READ` | By id |
| GET | `/promotions/code/{code}` | `POS_PROMOTION_READ` | By code |
| POST | `/promotions` | `POS_PROMOTION_CREATE` | Create |
| PUT | `/promotions/{id}` | `POS_PROMOTION_UPDATE` | Update |
| DELETE | `/promotions/{id}` | `POS_PROMOTION_DELETE` | Delete |
| POST | `/promotions/{id}/activate` | `POS_PROMOTION_UPDATE` | Activate |
| POST | `/promotions/{id}/deactivate` | `POS_PROMOTION_UPDATE` | Deactivate |
| POST | `/promotions/{promotionId}/conditions` | `POS_PROMOTION_CONDITIONS_MANAGE` | Add a condition |
| DELETE | `/promotions/{promotionId}/conditions/{conditionId}` | `POS_PROMOTION_CONDITIONS_MANAGE` | Remove a condition |
| POST | `/promotions/{promotionId}/actions` | `POS_PROMOTION_ACTIONS_MANAGE` | Add an action |
| DELETE | `/promotions/{promotionId}/actions/{actionId}` | `POS_PROMOTION_ACTIONS_MANAGE` | Remove an action |

**`type`** (`PromotionType`): `PERCENTAGE_OFF`, `FIXED_AMOUNT_OFF`, `BUY_X_GET_Y`, `BUNDLE`,
`FREE_ITEM`, `TIERED_DISCOUNT`, `SPEND_X_GET_Y`.
**`scope`** (`PromotionScope`): `ORDER`, `LINE_ITEM`, `SHIPPING`, `CATEGORY`, `PRODUCT`.
**`channel`** (`PromotionChannel`): `POS`, `WEB`, `ALL` — controls where the promotion applies (the
online shop matches `WEB`/`ALL`, the register `POS`/`ALL`).

**Create body** (`CreatePromotionRequest`):
```jsonc
{
  "code": "SUMMER10",              // required, unique per tenant
  "name": "Summer 10% off",        // required
  "type": "PERCENTAGE_OFF",        // required
  "scope": "ORDER",
  "channel": "ALL",
  "priority": 10,                  // higher wins when stacking is off
  "discountValue": 10,             // percent for PERCENTAGE_OFF; amount for FIXED_AMOUNT_OFF
  "maxDiscountAmount": 50000,
  "buyQuantity": 2, "getQuantity": 1, "getDiscountPercent": 100,  // BUY_X_GET_Y
  "startDate": "2026-06-01", "endDate": "2026-08-31",
  "startTime": "09:00", "endTime": "21:00", "daysOfWeek": "MON,TUE,WED,THU,FRI",
  "stackable": false,
  "requiresCoupon": true,          // true → only redeemable via a coupon (below)
  "maxUses": 1000, "maxUsesPerCustomer": 1,
  "minOrderAmount": 100000,
  "locationId": null,
  "conditions": [], "actions": []  // optional; also manageable via the sub-resources
}
```
`PromotionDto` adds `active`, `currentUses`, `locationName`, `couponCount`, and the resolved
`conditions[]` / `actions[]`.

---

## Coupons  ·  `/api/v1/pos/coupons`

A coupon is a code bound to a promotion; redeeming the code applies that promotion. Use single-use
per-customer codes for personal offers, or `generate` a batch for a campaign.

| Method | Path | Permission | Purpose |
|---|---|---|---|
| GET | `/coupons` | `POS_COUPON_READ` | List (paged) |
| GET | `/coupons/promotion/{promotionId}` | `POS_COUPON_READ` | Coupons of a promotion (paged) |
| GET | `/coupons/status/{status}` | `POS_COUPON_READ` | By status (paged) |
| GET | `/coupons/{id}` | `POS_COUPON_READ` | By id |
| GET | `/coupons/code/{code}` | `POS_COUPON_READ` | By code |
| POST | `/coupons` | `POS_COUPON_CREATE` | Create one |
| POST | `/coupons/generate/{promotionId}?count=` | `POS_COUPON_GENERATE` | Generate `count` coupons |
| PUT | `/coupons/{id}` | `POS_COUPON_UPDATE` | Update |
| DELETE | `/coupons/{id}` | `POS_COUPON_DELETE` | Delete |
| POST | `/coupons/{id}/activate` | `POS_COUPON_UPDATE` | Activate |
| POST | `/coupons/{id}/deactivate` | `POS_COUPON_UPDATE` | Deactivate |
| POST | `/coupons/{id}/cancel` | `POS_COUPON_UPDATE` | Cancel |
| GET | `/coupons/{id}/redemptions` | `POS_COUPON_REDEMPTIONS_VIEW` | Redemption history |
| POST | `/coupons/update-expired` | `POS_COUPON_UPDATE` | Sweep: mark past-end coupons `EXPIRED` |
| POST | `/coupons/update-depleted` | `POS_COUPON_UPDATE` | Sweep: mark used-up coupons `DEPLETED` |

**`status`** (`CouponStatus`): `ACTIVE`, `INACTIVE`, `EXPIRED`, `DEPLETED`, `CANCELLED`.

**Create body** (`CreateCouponRequest`):
```jsonc
{
  "code": "VIP-A1B2C3",            // required, unique per tenant
  "promotionId": 42,               // required — the promotion this coupon unlocks
  "description": "Personal VIP coupon",
  "startDate": "2026-07-01", "endDate": "2026-07-31",
  "maxUses": 1, "maxUsesPerCustomer": 1,
  "customerId": 100,               // optional AR-customer binding
  "restrictedToCustomerId": null,  // optional hard restriction to one customer
  "minimumOrderAmount": 50000,
  "customerEmail": null, "notes": null
}
```
`generate/{promotionId}?count=N` takes the same body as a template and returns `N` coupons with
generated codes. `CouponDto` adds `status`, `currentUses`, `remainingUses`, `redemptionCount`,
`promotionCode`/`promotionName`, `firstUsedAt`/`lastUsedAt`.

> **Note — online-shop customers:** the mobile shop / distribution agent flows issue coupons bound
> to a *web* customer via the web module (`web_customer_id`), which is a different binding than the
> AR `customerId` here. See [`MOBILE_SHOP_API.md`](MOBILE_SHOP_API.md) `/me/coupons`.

---

## Price lists  ·  `/api/v1/pos/price-lists`

Per-tier or per-customer product prices. A price list holds items (product → price, with optional
min/max price, markup, and quantity breaks) and can be assigned to specific customers.

| Method | Path | Permission | Purpose |
|---|---|---|---|
| GET | `/price-lists` | `POS_PRICELIST_READ` | List (paged) |
| GET | `/price-lists/active` | `POS_PRICELIST_READ` | Active lists |
| GET | `/price-lists/{id}` | `POS_PRICELIST_READ` | By id |
| GET | `/price-lists/code/{code}` | `POS_PRICELIST_READ` | By code |
| POST | `/price-lists` | `POS_PRICELIST_CREATE` | Create |
| PUT | `/price-lists/{id}` | `POS_PRICELIST_UPDATE` | Update |
| DELETE | `/price-lists/{id}` | `POS_PRICELIST_DELETE` | Delete |
| POST | `/price-lists/{id}/activate` | `POS_PRICELIST_UPDATE` | Activate |
| POST | `/price-lists/{id}/deactivate` | `POS_PRICELIST_UPDATE` | Deactivate |
| GET | `/price-lists/{priceListId}/items` | `POS_PRICELIST_READ` | Items (paged) |
| GET | `/price-lists/{priceListId}/items/{itemId}` | `POS_PRICELIST_READ` | One item |
| POST | `/price-lists/{priceListId}/items` | `POS_PRICELIST_ITEMS_MANAGE` | Add item |
| PUT | `/price-lists/{priceListId}/items/{itemId}` | `POS_PRICELIST_ITEMS_MANAGE` | Update item |
| DELETE | `/price-lists/{priceListId}/items/{itemId}` | `POS_PRICELIST_ITEMS_MANAGE` | Remove item |
| GET | `/price-lists/customer/{customerId}` | `POS_PRICELIST_READ` | Lists assigned to a customer |
| POST | `/price-lists/{priceListId}/assign-customer/{customerId}` | `POS_PRICELIST_ASSIGN` | Assign to a customer |
| DELETE | `/price-lists/{priceListId}/unassign-customer/{customerId}` | `POS_PRICELIST_ASSIGN` | Unassign |

**`type`** (`PriceListType`): `STANDARD`, `WHOLESALE`, `VIP`, `SEASONAL`, `EMPLOYEE`, `CUSTOM`.

**Create body** (`CreatePriceListRequest`):
```jsonc
{
  "code": "WHOLESALE",             // required
  "name": "Wholesale prices",      // required
  "type": "WHOLESALE",             // required
  "currency": "UZS",
  "priority": 5,                   // higher-priority list wins when several apply
  "defaultMarkupPercent": 0,
  "startDate": "2026-01-01", "endDate": null,
  "defaultPriceList": false, "locationId": null, "notes": null
}
```
**Item body** (`PriceListItemDto` on POST/PUT): `{ productId, variantId?, price, minPrice?, maxPrice?,
markupPercent?, minQuantity?, maxQuantity?, startDate?, endDate?, active, notes? }`.

---

## Pricing engine  ·  `/api/v1/pos/pricing`

What the register/checkout calls to resolve final prices, apply promotions, and validate/apply
coupons. Server-authoritative — clients never compute their own prices.

| Method | Path | Permission | Purpose |
|---|---|---|---|
| POST | `/pricing/calculate` | `POS_PRICING_CALCULATE` | Price a whole cart (price lists + promotions + coupon) |
| GET | `/pricing/product/{productId}?variantId=&quantity=&customerId=&locationId=` | `POS_PRICING_CALCULATE` | Resolve one product's unit price |
| POST | `/pricing/apply-coupon` | `POS_COUPON_APPLY` | Apply a coupon to an order total |
| POST | `/pricing/validate-coupon?couponCode=&customerId=` | `POS_COUPON_APPLY` | Check a coupon without consuming it |
| POST | `/pricing/record-coupon-redemption?couponCode=&customerId=&orderId=&discountApplied=` | `POS_COUPON_REDEEM` | Record a redemption after the sale |

**`POST /pricing/calculate`** body (`PriceCalculationRequest`):
```jsonc
{
  "items": [ { "productId": 10, "variantId": null, "quantity": 3, "overridePrice": null } ],
  "customerId": 100,               // selects assigned price lists
  "locationId": null,
  "couponCode": "SUMMER10",        // optional
  "applyPromotions": true
}
```
Response (`PriceCalculationResult`): per-line `{ productId, quantity, basePrice, unitPrice,
lineDiscount, lineTotal, priceListCode, appliedPromotionCodes }`, plus order-level `subtotal`,
`totalDiscount`, `taxAmount`, `grandTotal`, `appliedPromotions[]`, and `couponApplication`
`{ valid, couponCode, discountAmount, promotionName, errorMessage }`.

**`POST /pricing/apply-coupon`** body (`ApplyCouponRequest`):
`{ couponCode, customerId?, customerEmail?, orderTotal, transactionId? }`.
Response (`ApplyCouponResponse`): `{ valid, couponCode, message, discountAmount, newTotal,
promotionCode, promotionName, discountDescription, errorMessage }`. `validate-coupon` returns the
same shape but never consumes a use — call it while the cashier is still editing the cart, then
`record-coupon-redemption` once the sale commits.

> **Note:** the online shop validates/applies coupons through its own public endpoint
> (`POST /api/v1/web/cart/validate-coupon`, see [`MOBILE_SHOP_API.md`](MOBILE_SHOP_API.md)); these
> `/pos/pricing` endpoints are the **staff/register** side.

---

## Related
- POS core (terminals, shifts, transactions, payments): [`POS_MODULE_API.md`](POS_MODULE_API.md).
- Customer shop coupons/pricing: [`MOBILE_SHOP_API.md`](MOBILE_SHOP_API.md).
- Platform reference: [`../API.md`](../API.md).
