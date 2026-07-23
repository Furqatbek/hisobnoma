# Mobile Shop Module — Customer API

The **mobile shop** is the customer-facing online store: shoppers browse the catalog, build a
cart, place an order, and track it. The client is the Flutter app in [`mobile-shop/`](../../mobile-shop);
this document is the HTTP contract it (or any client) talks to.

- **Backend package:** `com.hisobnoma.platform.web` (the `web` prefix is a security-whitelist
  convention, not a browser UI).
- **Base path:** `/api/v1/web`
- **Payments:** **cash-on-delivery only** right now — every order is created `CASH` / payment
  status `NONE`, and any payment method the client sends is ignored. (The card endpoints exist but
  return `503` until a provider is configured — see [Payments](#payments).)

---

## Conventions

### Tenant selection — required
Every request must identify the shop via the **`X-Tenant-ID`** header (the store's tenant id).
Authenticated calls may instead rely on the customer token, which carries the tenant. **A request
with neither is rejected with `400 TENANT_REQUIRED`** (the shop never silently defaults to a tenant).

```
X-Tenant-ID: 1
```

### Response envelope
Single objects are wrapped in `ApiResponse`; lists that page are wrapped in `PageResponse`.

```jsonc
// ApiResponse<T>
{ "success": true, "message": "…", "data": { /* T */ } }

// PageResponse<T>
{ "success": true, "data": { "content": [ /* T[] */ ],
  "page": { "number": 0, "size": 20, "totalElements": 42, "totalPages": 3 } } }
```

### Authentication
Anonymous by default. Account features (`/me/**`) require a **customer token** obtained from
phone + SMS OTP, sent as a bearer token:

```
Authorization: Bearer <web-customer-token>
```

This token is **separate from staff/admin JWTs** and only grants access to the customer's own data.

---

## Endpoint reference

### Catalog  ·  `/api/v1/web/catalog`  ·  anonymous
| Method | Path | Purpose |
|---|---|---|
| GET | `/products?search=&categoryId=&page=0&size=20` | List LIVE products (paged) |
| GET | `/products/{id}` | Product detail |
| GET | `/categories` | Categories that have live products |

`PublicCatalogProductDto`: `id, name, shortDescription, description, price, salePrice,
promotionLabel, currency, categoryId, categoryName, brandName, unitName, inStock, fractional,
step, imageUrl, images[]`. `salePrice`/`promotionLabel` are set when a promotion applies;
`fractional` + `step` describe weighable goods (e.g. sold per 0.25 kg).

### Cart preview  ·  `/api/v1/web/cart`  ·  anonymous (rate-limited)
| Method | Path | Purpose |
|---|---|---|
| POST | `/price` | Server-side price of a cart incl. promotions (display only) |
| POST | `/validate-coupon` | Check a coupon against the cart |

These are **display previews** — checkout always recomputes server-side, so a tampered client
price is ignored. `/price` is rate-limited ~5 per 10s per IP; `/validate-coupon` 5/min per IP.

### Delivery lookups  ·  anonymous
| Method | Path | Purpose |
|---|---|---|
| GET | `/api/v1/web/delivery/regions` | Delivery regions + fees |
| GET | `/api/v1/web/delivery/villages?regionId=` | Villages (optionally by region) |

### Checkout & order tracking  ·  anonymous
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/web/orders` | Place an order (guest or logged-in) → `201` |
| GET | `/api/v1/web/orders/{orderNumber}?phone=` | Track an order (phone must match) |

Checkout is rate-limited 5/min per IP+phone. On success the tenant's staff are notified (in-app
alert + Telegram + APNs push). See [Placing an order](#5-place-an-order-checkout).

### Auth  ·  `/api/v1/web/auth`  ·  anonymous
| Method | Path | Purpose |
|---|---|---|
| POST | `/request-otp` | Send an SMS code to a phone |
| POST | `/verify` | Verify the code → returns a customer token |

Abuse limits: 60s cooldown between codes, max 5 codes/phone/day, 5 wrong attempts per code,
5-minute expiry, plus a per-IP limiter.

### Account (`/me`)  ·  requires `Authorization: Bearer`
| Method | Path | Purpose |
|---|---|---|
| GET | `/me` | Current customer (`phone`, `name`, `customerCode`, `tenantSlug`) |
| GET | `/me/orders?page=&size=` | Order history |
| GET | `/me/loyalty` | Points balance + ledger |
| GET | `/me/coupons` | Coupons available to the customer |
| GET | `/me/wishlist` · `/me/wishlist/ids` | Wishlist (full / id list) |
| PUT/DELETE | `/me/wishlist/{catalogItemId}` | Like / unlike a product |
| GET | `/me/notifications` · `/me/notifications/unread-count` | In-app notifications |
| PUT | `/me/notifications/{id}/read` · `/me/notifications/read-all` | Mark read |

Notification `type` values the app may receive: `ORDER_STATUS` (carries `orderNumber` in push data),
`GENERAL`, and `MANUAL` (a message staff composed in the admin panel — no reference data, just show it).
| GET | `/me/referral-code` · `/me/referral-stats` | Referral program |
| POST/DELETE | `/me/device-token` | Register / remove an FCM push token |

<a id="payments"></a>
### Payments  ·  `/api/v1/web`
| Method | Path | Purpose |
|---|---|---|
| POST | `/orders/{orderNumber}/payment` | Start an online payment |
| GET | `/payments/{id}` | Poll payment status |

**Cash-only today:** no payment provider is configured, so `POST …/payment` returns
`503 PAYMENT_NOT_CONFIGURED`. Orders are fulfilled cash-on-delivery; this section is documented for
when a provider (Payme/Click/Uzum) is enabled.

---

## Demo walkthrough — the full customer journey

A runnable `curl` sequence against a local server (`http://localhost:8080`, tenant `1`). Replace
values as needed.

### 0. Convenience
```bash
BASE=http://localhost:8080/api/v1/web
TENANT="X-Tenant-ID: 1"
```

### 1. Browse the catalog
```bash
# Categories
curl -s "$BASE/catalog/categories" -H "$TENANT"

# First page of products, optionally searching
curl -s "$BASE/catalog/products?search=cola&page=0&size=20" -H "$TENANT"
```
```jsonc
{ "success": true, "data": { "content": [
  { "id": 100, "name": "Cola 1L", "price": 12000.00, "salePrice": 10800.00,
    "promotionLabel": "-10%", "currency": "UZS", "categoryName": "Drinks",
    "unitName": "dona", "inStock": true, "fractional": false, "imageUrl": "/uploads/…" }
], "page": { "number": 0, "size": 20, "totalElements": 1, "totalPages": 1 } } }
```

### 2. Preview the cart price (optional, for the cart screen)
```bash
curl -s -X POST "$BASE/cart/price" -H "$TENANT" -H "Content-Type: application/json" -d '{
  "lines": [ { "catalogItemId": 100, "quantity": 2 },
             { "catalogItemId": 105, "quantity": 1 } ]
}'
```
```jsonc
{ "success": true, "data": {
  "lines": [ { "catalogItemId": 100, "productName": "Cola 1L", "quantity": 2,
               "unitPrice": 10800.00, "lineTotal": 21600.00 } ],
  "subtotal": 29600.00, "discountTotal": 2400.00, "total": 27200.00,
  "currency": "UZS", "appliedPromotions": ["WEB10"] } }
```

Check a coupon the same way:
```bash
curl -s -X POST "$BASE/cart/validate-coupon" -H "$TENANT" -H "Content-Type: application/json" -d '{
  "code": "SAVE5", "lines": [ { "catalogItemId": 100, "quantity": 2 } ]
}'
# → { "success": true, "data": { "couponCode": "SAVE5", "valid": true, "discount": 1000.00 } }
```

### 3. (Optional) Sign in to earn/spend loyalty and see order history
```bash
# Request an SMS code
curl -s -X POST "$BASE/auth/request-otp" -H "$TENANT" -H "Content-Type: application/json" \
  -d '{ "phone": "+998901234567" }'

# Verify it → returns the customer token
TOKEN=$(curl -s -X POST "$BASE/auth/verify" -H "$TENANT" -H "Content-Type: application/json" \
  -d '{ "phone": "+998901234567", "code": "123456", "name": "Ali" }' \
  | jq -r '.data.token')
AUTH="Authorization: Bearer $TOKEN"
```
```jsonc
// POST /auth/verify → data
{ "token": "eyJhbGciOi…", "phone": "998901234567", "name": "Ali" }
```

### 4. Look up delivery options
```bash
curl -s "$BASE/delivery/regions" -H "$TENANT"
# → [ { "id": 2, "name": "Toshkent", "deliveryFee": 15000.00 }, … ]
curl -s "$BASE/delivery/villages?regionId=2" -H "$TENANT"
```

### 5. Place an order (checkout)
Guest checkout needs only name + phone + lines; a logged-in customer adds their bearer token so
the order links to their account (and can redeem loyalty points).
```bash
curl -s -X POST "$BASE/orders" -H "$TENANT" -H "Content-Type: application/json" \
  ${TOKEN:+-H "$AUTH"} -d '{
    "customerName": "Ali Valiyev",
    "phone": "+998901234567",
    "regionId": 2,
    "villageId": 14,
    "address": "Chilonzor 5, dom 12, kv 3",
    "note": "Call on arrival",
    "couponCode": "SAVE5",
    "pointsToSpend": 5000,
    "lines": [ { "catalogItemId": 100, "quantity": 2 },
               { "catalogItemId": 105, "quantity": 1 } ]
  }'
```
```jsonc
// 201 Created
{ "success": true, "data": {
  "orderNumber": "WO-000042",
  "status": "NEW",
  "paymentMethod": "CASH",       // always cash for now
  "paymentStatus": "NONE",
  "address": "Chilonzor 5, dom 12, kv 3",
  "deliveryFee": 15000.00,
  "discountTotal": 2400.00,
  "couponCode": "SAVE5", "couponDiscount": 1000.00,
  "pointsSpent": 5000.00,
  "totalAmount": 33800.00,
  "currency": "UZS",
  "createdAt": "2026-07-18T10:45:00Z",
  "lines": [ { "productName": "Cola 1L", "quantity": 2,
               "unitPrice": 10800.00, "lineTotal": 21600.00 } ] } }
```
Notes:
- **Prices are always recomputed server-side** — any price in the request is ignored.
- **An invalid coupon rejects the checkout** (`400`) rather than silently dropping the discount.
- `pointsToSpend` is capped server-side by the shop's max-redeem-percent and the customer's balance
  (ignored for guests / when loyalty is off).
- On success the tenant's staff get a **new-order notification** (in-app alert + Telegram + APNs).

### 6. Track the order (guest — by number + phone)
```bash
curl -s "$BASE/orders/WO-000042?phone=%2B998901234567" -H "$TENANT"
# → same PublicOrderDto shape, with the current status (NEW → CONFIRMED → … )
```

### 7. Account screens (logged-in)
```bash
curl -s "$BASE/me"          -H "$TENANT" -H "$AUTH"   # { "phone": "998…", "name": "Ali", "customerCode": "WC-00042", "tenantSlug": "…" }
curl -s "$BASE/me/orders"   -H "$TENANT" -H "$AUTH"   # PageResponse<PublicOrderDto>
curl -s "$BASE/me/loyalty"  -H "$TENANT" -H "$AUTH"
```
```jsonc
// GET /me/loyalty → data
{ "balance": 12000.00, "enabled": true, "minRedeem": 1000.00, "maxRedeemPercent": 50,
  "entries": [ { "type": "EARN", "amount": 1200.00, "orderNumber": "WO-000042",
                 "createdAt": "2026-07-18T10:45:00Z" } ] }
```

Wishlist and push token:
```bash
curl -s -X PUT    "$BASE/me/wishlist/100"      -H "$TENANT" -H "$AUTH"   # like
curl -s -X DELETE "$BASE/me/wishlist/100"      -H "$TENANT" -H "$AUTH"   # unlike
curl -s -X POST   "$BASE/me/device-token" -H "$TENANT" -H "$AUTH" \
  -H "Content-Type: application/json" -d '{ "token": "<fcm-token>", "platform": "android" }'
```

---

## Errors

| Status | Code | Meaning |
|---|---|---|
| 400 | `TENANT_REQUIRED` | No `X-Tenant-ID` header and no customer token |
| 400 | `VALIDATION_ERROR` | Missing/invalid body (e.g. no lines, invalid coupon) |
| 401 | `UNAUTHORIZED` | Missing/expired customer token on a `/me/**` call |
| 404 | `NOT_FOUND` | Unknown product / order (or a tracking phone that doesn't match) |
| 429 | `TOO_MANY_REQUESTS` | Rate limit hit (OTP, checkout, cart preview, coupon) |
| 503 | `PAYMENT_NOT_CONFIGURED` | Online payment requested while cash-only |

Error body:
```json
{ "success": false, "error": { "code": "VALIDATION_ERROR", "message": "Coupon is invalid or expired" } }
```

## Rate limiting
Distributed (Redis) in production, in-memory in dev. Key limits: OTP request per IP, checkout
5/min per IP+phone, cart price ~5/10s per IP, coupon 5/min per IP. On a Redis outage the limiter
fails open, and DB-level caps (5 OTP/phone/day, 60s cooldown) still bound the costly paths.

## Related
- **Staff-side order management** (confirm, fulfil, cancel) lives under `/api/v1/web-orders`,
  `/api/v1/web-catalog`, `/api/v1/web-customers`, `/api/v1/web-campaigns` — staff JWT + permissions,
  not part of this customer API.
- **Client app:** [`mobile-shop/`](../../mobile-shop) (Flutter) — see its `README.md` and
  `DESIGN_BRIEF.md`.
- **Plan / scope:** [`docs/WEB_SHOP_PLAN.md`](../WEB_SHOP_PLAN.md).
- The broader platform contract also documents these endpoints in [`docs/API.md`](../API.md).
