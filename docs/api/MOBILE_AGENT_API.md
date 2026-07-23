# Distribution Agent Module — Mobile API

The **agent app** is the field-sales client for distribution (van-sales) agents: an agent logs in
by phone, sees their assigned routes and today's plan, checks in at each customer, places and
fulfils orders from the van, and collects cash against outstanding invoices. This document is the
HTTP contract the app (or any client) talks to.

- **Backend package:** `com.hisobnoma.platform.distribution` (`AgentAuthController`,
  `AgentPortalController`).
- **Base path:** `/api/v1/agent`
- **Who it's for:** distribution **agents**, not shop customers and not back-office staff. It is a
  separate public, token-authenticated surface — agents never need a staff account. The back office
  keeps full control over the same data through the staff endpoints in
  [`../API.md`](../API.md) (`DISTRIBUTION_*` permissions).

---

## Conventions

### Tenant selection — required on login
The **login** calls identify the store via the **`X-Tenant-ID`** header. After login the tenant is
carried inside the agent token and is **never** taken from a header again — every `/me/**` call is
scoped to the tenant and agent baked into the token. A login request with no tenant is rejected
with `400 TENANT_REQUIRED`.

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
All `/me/**` endpoints require an **agent token** obtained from phone + SMS OTP, sent as a bearer
token:

```
Authorization: Bearer <distribution-agent-token>
```

The token is signed with a key **derived** from the staff secret (`secret + "::distribution-agent"`),
so it can never be interchanged with a staff, web-customer, or B2B token. It carries the agent id +
tenant and expires after 30 days (`app.jwt.agent-expiration`). A `SUSPENDED`/`TERMINATED` agent is
rejected on use even with a still-valid token (`401`).

---

## Endpoint reference

### Auth  ·  `/api/v1/agent/auth`
| Method | Path | Purpose |
|---|---|---|
| POST | `/auth/request-otp` | Send a 6-digit SMS code to the agent's phone |
| POST | `/auth/verify` | Exchange phone + code for a bearer token |
| GET | `/me` (auth) | The authenticated agent's profile |

**`POST /auth/request-otp`** — body `{ "phone": "998901234567" }`. **Always returns `200`** (it never
reveals whether a phone is registered); an SMS is sent only if the phone maps to exactly one
**ACTIVE** agent in the header tenant. Abuse limits: per-IP limiter, 60 s resend cooldown, ≤5
codes/day/phone, 5-minute code expiry, 5 wrong attempts per code. Only a salted SHA-256 hash is
stored.

**`POST /auth/verify`** — body `{ "phone": "998901234567", "code": "123456" }`.

```jsonc
// ApiResponse<AgentAuthResponse>
{ "success": true, "data": {
  "token": "<distribution-agent-token>",
  "agentId": 42, "code": "AG-1", "name": "Alisher", "phone": "998901234567" } }
```

The phone must resolve to exactly one active agent; a phone shared by more than one active agent is
rejected (`AGENT_PHONE_AMBIGUOUS`) rather than logging in the wrong person.

**`GET /me`** — `ApiResponse<AgentProfileDto>`:
`{ agentId, code, name, phone, vehiclePlate, vehicleName, status }`.

### Portal  ·  `/api/v1/agent/me`  ·  agent token
| Method | Path | Purpose |
|---|---|---|
| GET | `/me/summary` | Today's snapshot |
| GET | `/me/routes` | The agent's assigned routes |
| GET | `/me/visits` | The agent's visits (paginated) |
| GET | `/me/loadout/current` | The agent's current (most recent LOADED) van loadout, or `null` |
| GET | `/me/orders` | The agent's distribution orders (paginated) |
| POST | `/me/orders` | Place an order from the field |
| POST | `/me/orders/{id}/deliver` | Deliver + invoice the order on the spot (van sale) |
| POST | `/me/visits/check-in` | Record a check-in |
| POST | `/me/visits/{id}/check-out` | Record outcome + optional cash collection |

Every endpoint scopes strictly to `(tenant, agentId)` from the token — a client **never** supplies
an agentId, and an order/visit that belongs to another agent returns `404`.

**`GET /me/summary`** → `ApiResponse` of
`{ agentId, agentName, date, routes, visitsToday, hasActiveLoadout }`.

**`GET /me/routes`** → `ApiResponse<DistributionRouteDto[]>`:
`{ id, code, name, agentId, territoryRegionId, dayOfWeek, estimatedDurationMinutes, distanceKm, status, notes }`.

**`GET /me/loadout/current`** → `ApiResponse<VanLoadoutDto>` (or `data: null` when no loadout is out):
`{ id, loadoutNumber, status, agentId, vehicleLocationId, loadoutDate, totalLoadedValue, lines: [{ productId, productName, productSku, quantityLoaded, unitName }], … }`.

**`POST /me/visits/check-in`** — body
`{ "customerId": 100, "routeId": 80, "routeStopId": 5, "visitType": "PLANNED",
   "latitude": 41.31, "longitude": 69.28, "notes": "…" }`. There is **no agentId** (forced to the
token holder). `visitType` ∈ `PLANNED | AD_HOC | RETURN_VISIT`. Returns the created
`DistributionVisitDto` with `outcome: "PENDING"`.

**`POST /me/visits/{id}/check-out`** — body
`{ "outcome": "PAYMENT_COLLECTED", "latitude": 41.31, "longitude": 69.28,
   "distributionOrderId": 50, "collectedAmount": 60000, "notes": "…" }`.
`outcome` ∈ `ORDER_PLACED | NO_ORDER | PAYMENT_COLLECTED | RESCHEDULED | CLOSED`.
When `collectedAmount > 0` the check-out **creates a completed, GL-posted AR payment** allocated
oldest-due-first across the customer's open invoices (`PENDING`/`SENT`/`PARTIAL`/`OVERDUE`); any
excess stays as a customer advance. Idempotent per visit — a repeat check-out that carries a
collection on a visit that already has one is rejected (`COLLECTION_EXISTS`).

**`POST /me/orders`** — field order placement. Body:

```jsonc
{ "customerId": 100, "visitId": 50, "routeId": 80,
  "paymentMethod": "CASH", "paymentTermsDays": 14,
  "discountAmount": 0, "deliveryFee": 0,
  "deliveryAddress": "…", "deliveryLat": 41.31, "deliveryLng": 69.28,
  "expectedDeliveryDate": "2026-07-24", "notes": "…",
  "confirmNow": true,
  "lines": [ { "productId": 10, "quantity": 3, "discountPercent": 0 } ] }
```

There is **no agentId** (forced to the token holder) and **no sourceLocationId** — the sale defaults
to drawing down the agent's **current van loadout** location. Prices are resolved **server-side**
(the client's numbers, if any, are ignored). An attached `visitId` must be the agent's own.
`paymentMethod` ∈ `CASH | CREDIT | MIXED`. The order is created `DRAFT`; set `confirmNow: true` to
immediately CONFIRM it (reserving van stock) — the typical van-sale capture. Returns the created
`DistributionOrderDto`.

**`POST /me/orders/{id}/deliver`** — on-the-spot fulfilment. Body optional `{ "cashCollected": 20000 }`.
Takes the agent's **own** order all the way to `INVOICED` in one call: the goods are already on the
van, so the warehouse states (`PICKING`/`LOADED`/`IN_TRANSIT`) are auto-advanced and a `DRAFT` order
is CONFIRMED first (reserving stock). Delivery **deducts van stock** and applies the cash/credit
split (`cashCollected` defaults to the full total for a `CASH` order); the invoice step raises an AR
receivable **only** for any credit portion (a fully cash-settled sale raises none). **Idempotent** —
a repeat call on an already-`INVOICED` order returns it unchanged — and `404` on another agent's
order. Returns the `DistributionOrderDto` with final `status`, `cashCollected`, `creditAmount`, and
`arInvoiceNumber` (present only when a receivable was raised).

---

## Demo walkthrough — an agent's day

All requests send `X-Tenant-ID: 1` on login; `/me/**` calls send `Authorization: Bearer <token>`.

**0. Log in** — the agent enters their phone; the app requests a code and the agent types it back.

```bash
curl -X POST $BASE/api/v1/agent/auth/request-otp -H 'X-Tenant-ID: 1' \
  -H 'Content-Type: application/json' -d '{"phone":"998901234567"}'
# → 200 { "success": true, "message": "Code sent if the phone is registered" }

curl -X POST $BASE/api/v1/agent/auth/verify -H 'X-Tenant-ID: 1' \
  -H 'Content-Type: application/json' -d '{"phone":"998901234567","code":"123456"}'
# → 200 { data: { token, agentId, code, name, phone } }
```

**1. Home screen** — `GET /me/summary` for today's routes / visits / loadout flag, and
`GET /me/loadout/current` for what's on the van.

**2. Arrive at a customer** — `POST /me/visits/check-in` with GPS.

**3a. Take an order** — `POST /me/orders` with `confirmNow: true`, then
`POST /me/orders/{id}/deliver` to hand over the goods and (for CASH) settle on the spot.

**3b. Collect an old debt** — on `POST /me/visits/{id}/check-out` pass `collectedAmount`; the payment
lands against the customer's oldest open invoices automatically.

**4. Leave** — `POST /me/visits/{id}/check-out` with the outcome. Repeat 2–4 down the route.

---

## Errors

Standard envelope: `{ "success": false, "message": "…", "code": "…" }`.

| Code | When |
|---|---|
| `TENANT_REQUIRED` (400) | A login call without `X-Tenant-ID` |
| `AGENT_PHONE_AMBIGUOUS` | Verify: the phone maps to more than one active agent |
| `COLLECTION_EXISTS` | Check-out carries a collection on a visit that already has one |
| `ORDER_CANCELLED` | Deliver called on a cancelled order |
| `401 Unauthorized` | Missing/invalid/expired token, or a suspended/terminated agent |
| `404 Not Found` | An order/visit that belongs to another agent (never leaks existence) |

## Related
- Staff-side distribution API and the full lifecycle: [`../API.md`](../API.md) (Slices 1–7).
- Customer shop app: [`MOBILE_SHOP_API.md`](MOBILE_SHOP_API.md). Admin/POS mobile app:
  [`MOBILE_MODULE_API.md`](MOBILE_MODULE_API.md). Push: [`MOBILE_PUSH_API.md`](MOBILE_PUSH_API.md).
