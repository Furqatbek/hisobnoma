# Mobile Push Notifications (APNs) — Backend API

## Overview

APNs (Apple Push Notification service) delivery for the **admin mobile app**. The backend
holds the Apple `.p8` provider auth key, signs a short-lived ES256 JWT, and delivers over
HTTP/2 directly to Apple. Two surfaces:

1. **Device token registration** — the app registers/removes its opaque APNs device token
   (self-service, any authenticated mobile user).
2. **Staff broadcast** — staff with `MOBILE_PUSH_SEND` fan a notification out to a tenant's
   app users (or a single user).

This subsystem is **distinct from the FCM `device_tokens` table** used by the customer web
shop — it talks to Apple directly and stores tokens in `device_push_tokens`.

### Disabled by default

The subsystem is a **logged no-op until credentials are configured**. With no `.p8` key,
team id, and key id set, the send API skips every recipient and never fakes a delivery
(`apnsConfigured: false` in the response). Registration still works so tokens accumulate
before go-live.

Enable in production by setting:

| Env var | Meaning |
|---|---|
| `APNS_ENABLED` | `true` to enable delivery |
| `APNS_TEAM_ID` | Apple Developer Team ID (10 chars) → JWT `iss` |
| `APNS_KEY_ID` | APNs Auth Key ID (10 chars) → JWT header `kid` |
| `APNS_BUNDLE_ID` | App bundle id → `apns-topic` (default `com.hisobnoma.admin`) |
| `APNS_PRIVATE_KEY` | Contents of the `.p8` auth key (PKCS#8 PEM) |

Tokens are routed to the correct Apple host by their own environment: `SANDBOX` →
`api.sandbox.push.apple.com`, `PRODUCTION` → `api.push.apple.com`. A token minted in one
environment is rejected by the other, so the app MUST report which build it is (a
TestFlight/debug build is sandbox; App Store is production).

## Base URL

```
/api/v1/mobile/devices     # token registration (mobile user)
/api/v1/admin/notifications # broadcast (staff)
```

---

## Register a device token

```http
POST /api/v1/mobile/devices/push-token
Authorization: Bearer <mobile JWT>
```

Upserts on the device token itself — re-registering the same token just refreshes ownership,
environment, and last-seen. No special permission (a user manages their own device).

**Request Body:**
```json
{
  "token": "a1b2c3...",       // required, opaque APNs device token (≤512 chars)
  "platform": "ios",          // optional, defaults to "ios"
  "environment": "production", // "sandbox" | "production" (default production)
  "appVersion": "1.4.0"        // optional
}
```

**Response:** `200 OK`
```json
{ "success": true, "message": "Push token registered", "data": null }
```

## Remove a device token

```http
DELETE /api/v1/mobile/devices/push-token
Authorization: Bearer <mobile JWT>
```

Call on logout / notifications-disabled. Tenant-scoped delete; a blank token is a no-op.

**Request Body:**
```json
{ "token": "a1b2c3..." }
```

**Response:** `200 OK`
```json
{ "success": true, "message": "Push token removed", "data": null }
```

---

## Broadcast a notification (staff)

```http
POST /api/v1/admin/notifications/send
Authorization: Bearer <staff JWT>
```

Requires the `MOBILE_PUSH_SEND` permission (seeded to `SUPER_ADMIN`, `ADMIN`).

**Request Body:**
```json
{
  "audience": "tenant",       // "tenant" (all app users) | "user" (one user). Default tenant.
  "userId": 42,               // required only when audience = "user"
  "title": "Order shipped",   // required, ≤200
  "body": "Your order is on the way", // required, ≤1000
  "type": "new_order",        // optional routing type (≤40), e.g. system/new_order/payment_due
  "id": 555,                  // optional deep-link entity id
  "route": "/orders/555",     // optional explicit route (≤200)
  "badge": 3                  // optional badge count
}
```

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "recipients": 12,        // tokens targeted
    "sent": 11,              // delivered (APNs 200)
    "failed": 1,             // non-delivered this run
    "pruned": 1,             // dead tokens deleted (see below)
    "apnsConfigured": true   // false ⇒ credentials absent, nothing actually sent
  }
}
```

### Delivered payload shape

The APNs body Apple receives:
```json
{
  "aps": { "alert": { "title": "...", "body": "..." }, "sound": "default", "badge": 3 },
  "type": "new_order",
  "id": 555,
  "route": "/orders/555"
}
```
`badge`, `type`, `id`, and `route` are omitted when null. `title`/`body` are already localized
by the caller; `type`/`id`/`route` are language-neutral routing keys the app dispatches on.

### Dead-token cleanup

When APNs reports a token is permanently invalid — HTTP `410`, or reason `BadDeviceToken` /
`Unregistered` — the backend **deletes** that token (counted in `pruned`). Transient failures
(e.g. 503) are counted in `failed` but the token is kept. `DeviceTokenNotForTopic` is treated
as a (non-pruning) failure, not a dead token: it signals a wrong `apns-topic`/bundle id — a
provider-side config error — so pruning on it would wipe an entire tenant's tokens on the first
misconfigured broadcast.

---

## Error Responses

| Code | When |
|---|---|
| `USER_ID_REQUIRED` (400) | `audience="user"` but no `userId` |
| `ACCESS_DENIED` (403) | Broadcast without `MOBILE_PUSH_SEND` |
| `VALIDATION_ERROR` (400) | Missing/oversized `token`, `title`, or `body` |
