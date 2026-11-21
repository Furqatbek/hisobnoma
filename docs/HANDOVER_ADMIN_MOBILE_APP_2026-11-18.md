# Handover (delta) — Hisobnoma backend → Admin mobile app team

**Date:** 2026-11-18 · **Backend branch:** `claude/auto-generate-customer-code-Qj6Eh`
**Covers:** backend changes since the previous admin-app handover (commit `d50449f`).

**Summary: one small change.** Quick-sale now refuses lines priced below a product's minimum
selling price, so the app must show that error. Everything else is optional.

---

## PROMPT — copy from here

You maintain the Sheben N1 **admin mobile app**. The backend had a round of changes to the web
cash register. Do item 1; the rest is optional.

### 1. Handle `PRICE_BELOW_MIN` on quick-sale (required once the backend is redeployed)
`POST /api/v1/mobile/pos/quick-sale` now returns
`400 { "code": "PRICE_BELOW_MIN", "message": "Price for <product> is below the minimum allowed (<floor>)" }`
when a line's `unitPrice` (after its `discountAmount`) is below the product's minimum selling
price. This is implemented (backend commit `337663e`) and takes effect when the backend is
redeployed. A rejected quick-sale saves nothing.
- Show the server `message` and let the user correct the price.
- To prevent the error up front: `GET /api/v1/mobile/inventory/barcode/{barcode}` already returns
  `minSellingPrice` on the product. Block or warn in the price field when the entered price is
  lower. Products with no minimum set are unaffected.
- The existing `PATCH /api/v1/mobile/inventory/price` already rejects below-floor prices with the
  same code.

### 2. Optional — create a product with suppliers and selling units in one call
Only relevant if the app creates products. `POST /api/v1/inventory/products` now also accepts:
```json
"vendors":       [ { "vendorId": 5, "unitCost": 950, "preferred": true } ],
"alternateUoms": [ { "uomId": 7, "conversionFactor": 12, "sellingPrice": 15000 } ]
```
Everything is saved in one transaction (an invalid vendor/unit saves nothing). The old
create-then-attach calls still work. Details: `docs/API.md`.

### 3. Optional — one-call checkout (not needed while quick-sale covers your flow)
For split payments or a sale-level discount, two staff endpoints now exist:
- `POST /api/v1/pos/transactions/quote` → the server's totals for a cart, nothing saved.
- `POST /api/v1/pos/transactions/checkout` → lines + discount + `payments[]` + completion in one
  atomic call; send `expectedTotal` and handle `409 TOTAL_CHANGED` by quoting again.
Permissions: `POS_SALE_CREATE` + `POS_PAYMENT_PROCESS` (+ `POS_DISCOUNT_APPLY` for a discount).
Details: `docs/api/POS_MODULE_API.md`, section "One-Call Checkout".
`quick-sale` is unchanged and still supported, including `clientRequestId` idempotency.

### Definition of done
- [ ] Quick-sale shows the server message on `PRICE_BELOW_MIN` (and ideally pre-checks against
      `minSellingPrice`).
- [ ] Nothing else required.

## PROMPT — copy to here

---

## Still open from the previous handover (unchanged)
- **Item names on debtor receipts not showing:** send the endpoint the screen calls and the JSON
  it receives, or which field it reads for the line name.
- **"App glitches / no response":** send the action and rough time so the backend logs can be checked.

## Operations note
A 502 outage on 2026-11-10 was caused by the edge proxy holding stale container addresses after
a redeploy. The proxy now re-resolves on each request, so backend redeploys should no longer
interrupt the app. No app change needed.
