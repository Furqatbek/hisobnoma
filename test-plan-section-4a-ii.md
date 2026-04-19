# Section 4a-ii: POS — Terminals, Pricing, Promotions & Coupons — Test Plan

---

## 1. POSTerminalService Unit Tests

Framework: JUnit 5 + Mockito.

### 1.1 CRUD

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getTerminals_returnsPaged` | `getTerminals(tenantId, pageable)` | Multiple terminals | Returns `Page<POSTerminalDto>` |
| `getActiveTerminals_returnsOnlyActive` | `getActiveTerminals(tenantId)` | Mix of statuses | Returns only ACTIVE terminals |
| `getTerminal_found_returnsDto` | `getTerminal(tenantId, id)` | Terminal exists | Returns `POSTerminalDto` |
| `getTerminal_notFound_throwsNotFoundException` | `getTerminal(tenantId, id)` | Missing | Throws `NotFoundException` |
| `createTerminal_success` | `createTerminal(tenantId, request)` | Valid code + name | Returns dto |
| `createTerminal_duplicateCode_throwsDuplicateResourceException` | `createTerminal(tenantId, request)` | Code exists | Throws `DuplicateResourceException` |
| `updateTerminal_success` | `updateTerminal(tenantId, id, request)` | Valid update | Returns updated dto |
| `updateTerminal_notFound_throwsNotFoundException` | `updateTerminal(tenantId, id, request)` | Missing | Throws `NotFoundException` |

### 1.2 Activation

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `activateTerminal_inactive_becomesActive` | `activateTerminal(tenantId, id)` | INACTIVE terminal | Status ACTIVE |
| `activateTerminal_alreadyActive_idempotent` | `activateTerminal(tenantId, id)` | Already ACTIVE | No error; remains ACTIVE |
| `activateTerminal_notFound_throwsNotFoundException` | `activateTerminal(tenantId, id)` | Missing | Throws `NotFoundException` |
| `deactivateTerminal_active_becomesInactive` | `deactivateTerminal(tenantId, id)` | ACTIVE; no open shift | Status INACTIVE |
| `deactivateTerminal_hasOpenShift_throwsBusinessException` | `deactivateTerminal(tenantId, id)` | Has open shift | Throws `BusinessException` "Close shift first" |
| `deactivateTerminal_notFound_throwsNotFoundException` | `deactivateTerminal(tenantId, id)` | Missing | Throws `NotFoundException` |

---

## 2. PriceListService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getPriceLists_returnsPaged` | `getPriceLists(tenantId, pageable)` | Multiple | Returns paged |
| `getActivePriceLists_returnsOnlyActive` | `getActivePriceLists(tenantId)` | Mix | Returns only active |
| `getPriceList_found_returnsDto` | `getPriceList(tenantId, id)` | Exists | Returns dto |
| `getPriceList_notFound_throwsNotFoundException` | `getPriceList(tenantId, id)` | Missing | Throws `NotFoundException` |
| `createPriceList_success` | `createPriceList(tenantId, request)` | Valid | Returns dto |
| `createPriceList_duplicateName_throwsDuplicateResourceException` | `createPriceList(tenantId, request)` | Name exists | Throws `DuplicateResourceException` |
| `updatePriceList_success` | `updatePriceList(tenantId, id, request)` | Valid | Returns updated |
| `addPriceListItem_success` | `addPriceListItem(tenantId, listId, request)` | Product not in list | Item added |
| `addPriceListItem_duplicate_throwsDuplicateResourceException` | `addPriceListItem(tenantId, listId, request)` | Product already in list | Throws `DuplicateResourceException` |
| `addPriceListItem_productNotFound_throwsNotFoundException` | `addPriceListItem(tenantId, listId, request)` | Product missing | Throws `NotFoundException` |
| `removePriceListItem_success` | `removePriceListItem(tenantId, listId, itemId)` | Item exists | Item removed |
| `removePriceListItem_notFound_throwsNotFoundException` | `removePriceListItem(tenantId, listId, itemId)` | Item missing | Throws `NotFoundException` |
| `calculatePrice_productInList_returnsListPrice` | `calculatePrice(tenantId, productId, listId, qty)` | Product in list with override | Returns override price |
| `calculatePrice_productNotInList_returnsBasePrice` | `calculatePrice(tenantId, productId, listId, qty)` | Not in list | Returns base product price |
| `calculatePrice_qtyBreak_returnsLowerPrice` | `calculatePrice(tenantId, productId, listId, qty)` | Qty meets break threshold | Returns discounted tier price |

---

## 3. PricingService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `calculatePrice_basePrice_returned` | `calculatePrice(tenantId, productId, null, qty)` | No price list | Returns product base price |
| `calculatePrice_withPriceListOverride` | `calculatePrice(tenantId, productId, listId, qty)` | Price list override exists | Returns override price |
| `applyDiscount_10pct_reducesSubtotal` | `applyDiscount(subtotal, 10)` | 10% on 100 | Returns 90 |
| `applyDiscount_zeroPercent_noChange` | `applyDiscount(subtotal, 0)` | 0% discount | Returns original amount |
| `applyDiscount_over100pct_throwsValidationException` | `applyDiscount(subtotal, 101)` | 101% | Throws `ValidationException` |
| `applyDiscount_negative_throwsValidationException` | `applyDiscount(subtotal, -5)` | -5% | Throws `ValidationException` |
| `applyTax_standardRate_applied` | `applyTax(tenantId, amount, productId)` | Product has standard tax | Returns amount + tax |
| `applyTax_exemptProduct_returnsZeroTax` | `applyTax(tenantId, amount, productId)` | Product tax-exempt | Returns original amount; tax=0 |
| `calculateFinalPrice_combinedCorrectly` | `calculateFinalPrice(tenantId, request)` | Price + discount + tax | Returns correct final amount |

---

## 4. PromotionService Unit Tests

### 4.1 CRUD

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getPromotions_returnsPaged` | `getPromotions(tenantId, pageable)` | Multiple | Returns paged |
| `getActivePromotions_returnsOnlyActive` | `getActivePromotions(tenantId)` | Mix | Returns active within date range |
| `createPromotion_success` | `createPromotion(tenantId, request)` | Valid | Returns dto |
| `updatePromotion_success` | `updatePromotion(tenantId, id, request)` | Valid | Returns updated |
| `updatePromotion_notFound_throwsNotFoundException` | `updatePromotion(tenantId, id, request)` | Missing | Throws `NotFoundException` |
| `deactivatePromotion_success` | `deactivatePromotion(tenantId, id)` | Active | Status INACTIVE |
| `deactivatePromotion_alreadyInactive_idempotent` | `deactivatePromotion(tenantId, id)` | Already inactive | No error |

### 4.2 Evaluation

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `evaluateConditions_allMet_returnsTrue` | `evaluatePromotionConditions(tenantId, promoId, context)` | All conditions satisfied | Returns true |
| `evaluateConditions_oneFails_returnsFalse` | `evaluatePromotionConditions(tenantId, promoId, context)` | One condition fails | Returns false |
| `evaluateConditions_minQtyMet_returnsTrue` | `evaluatePromotionConditions(tenantId, promoId, context)` | qty ≥ MIN_QTY | Returns true |
| `evaluateConditions_minQtyNotMet_returnsFalse` | `evaluatePromotionConditions(tenantId, promoId, context)` | qty < MIN_QTY | Returns false |
| `evaluateConditions_minAmountMet_returnsTrue` | `evaluatePromotionConditions(tenantId, promoId, context)` | amount ≥ MIN_AMOUNT | Returns true |
| `applyAction_discountPct_applied` | `applyPromotionAction(tenantId, promoId, transaction)` | DISCOUNT_PCT action | Transaction total reduced by % |
| `applyAction_fixedDiscount_applied` | `applyPromotionAction(tenantId, promoId, transaction)` | FIXED_DISCOUNT action | Fixed amount deducted |
| `applyAction_freeItem_addedToTransaction` | `applyPromotionAction(tenantId, promoId, transaction)` | FREE_ITEM action | Free product line added |

---

## 5. CouponService Unit Tests

### 5.1 CRUD

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getCoupons_returnsPaged` | `getCoupons(tenantId, pageable)` | Multiple | Returns paged |
| `getActiveCoupons_returnsOnlyActive` | `getActiveCoupons(tenantId)` | Mix | Returns active only |
| `createCoupon_success` | `createCoupon(tenantId, request)` | Valid | Returns dto |
| `updateCoupon_success` | `updateCoupon(tenantId, id, request)` | Valid | Returns updated |
| `updateCoupon_notFound_throwsNotFoundException` | `updateCoupon(tenantId, id, request)` | Missing | Throws `NotFoundException` |
| `deactivateCoupon_success` | `deactivateCoupon(tenantId, id)` | Active | Status INACTIVE |

### 5.2 Validation & Redemption

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `validateCoupon_valid_returnsCouponDto` | `validateCoupon(tenantId, code, customerId)` | Valid active coupon | Returns `CouponDto` |
| `validateCoupon_expired_throwsBusinessException` | `validateCoupon(tenantId, code, customerId)` | Past expiry date | Throws `BusinessException` "Coupon expired" |
| `validateCoupon_fullyRedeemed_throwsBusinessException` | `validateCoupon(tenantId, code, customerId)` | usageCount >= maxUsage | Throws `BusinessException` |
| `validateCoupon_wrongCustomer_throwsBusinessException` | `validateCoupon(tenantId, code, customerId)` | Customer-specific coupon; wrong customer | Throws `BusinessException` |
| `validateCoupon_inactive_throwsBusinessException` | `validateCoupon(tenantId, code, customerId)` | INACTIVE coupon | Throws `BusinessException` |
| `validateCoupon_notFound_throwsNotFoundException` | `validateCoupon(tenantId, code, customerId)` | Code missing | Throws `NotFoundException` |
| `redeemCoupon_success_incrementsUsageCount` | `redeemCoupon(tenantId, couponId, transactionId)` | Valid redemption | `usageCount` incremented by 1 |
| `redeemCoupon_sameTransaction_idempotent` | `redeemCoupon(tenantId, couponId, transactionId)` | Already redeemed for same txn | No double increment |

---

## 6. Repository Tests (`@DataJpaTest`)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `POSTerminalRepository_findByCode` | `findByCode(tenantId, code)` | "TERM-01" exists | Returns Optional |
| `POSTerminalRepository_findActiveTerminals` | `findActiveTerminals(tenantId)` | Mix | Returns only ACTIVE |
| `POSTerminalRepository_findByLocationId` | `findByLocationId(locId)` | 2 terminals in location | Returns 2 |
| `CouponRepository_findActiveByCode` | `findActiveByCode(tenantId, code)` | Active code | Returns Optional |
| `CouponRepository_findActiveByCode_expired` | `findActiveByCode(tenantId, code)` | Expired coupon | Returns empty Optional |
| `CouponRedemptionRepository_findByCouponId` | `findByCouponId(couponId)` | 3 redemptions | Returns 3 |
| `PriceListRepository_findActiveByCustomer` | `findActiveByCustomer(tenantId, customerId)` | Customer-assigned list | Returns matching |
| `PromotionRepository_findActivePromotions` | `findActivePromotions(tenantId, date)` | 2 active in date range, 1 expired | Returns 2 |

---

## 7. Integration Tests

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getTerminals_returns200` | `GET /api/v1/pos/terminals` | Bearer `POS_READ` | `200 OK`; paged |
| `getTerminals_returns403` | `GET /api/v1/pos/terminals` | No permission | `403 Forbidden` |
| `createTerminal_returns201` | `POST /api/v1/pos/terminals` | Bearer `POS_WRITE`; valid | `201 Created` |
| `createTerminal_returns409_dupCode` | `POST /api/v1/pos/terminals` | Bearer `POS_WRITE`; dup | `409 Conflict` |
| `activateTerminal_returns200` | `PUT /api/v1/pos/terminals/{id}/activate` | Bearer `POS_WRITE` | `200 OK`; status=ACTIVE |
| `deactivateTerminal_returns200` | `PUT /api/v1/pos/terminals/{id}/deactivate` | Bearer `POS_WRITE`; no shift | `200 OK`; status=INACTIVE |
| `deactivateTerminal_returns422_hasShift` | `PUT /api/v1/pos/terminals/{id}/deactivate` | Bearer `POS_WRITE`; has shift | `422 Unprocessable Entity` |
| `getPriceLists_returns200` | `GET /api/v1/pos/price-lists` | Bearer `POS_READ` | `200 OK` |
| `createPriceList_returns201` | `POST /api/v1/pos/price-lists` | Bearer `POS_WRITE` | `201 Created` |
| `addPriceListItem_returns201` | `POST /api/v1/pos/price-lists/{id}/items` | Bearer `POS_WRITE` | `201 Created` |
| `addPriceListItem_returns409_dup` | `POST /api/v1/pos/price-lists/{id}/items` | Bearer `POS_WRITE`; dup | `409 Conflict` |
| `removePriceListItem_returns204` | `DELETE /api/v1/pos/price-lists/{id}/items/{itemId}` | Bearer `POS_WRITE` | `204 No Content` |
| `calculatePrice_returns200` | `POST /api/v1/pos/pricing/calculate` | Bearer `POS_READ` | `200 OK`; price field |
| `applyDiscount_returns200` | `POST /api/v1/pos/pricing/apply-discount` | Bearer `POS_READ` | `200 OK`; discounted amount |
| `applyDiscount_returns400_invalidPct` | `POST /api/v1/pos/pricing/apply-discount` | Bearer `POS_READ`; pct=101 | `400 Bad Request` |
| `getActivePromotions_returns200` | `GET /api/v1/pos/promotions/active` | Bearer `POS_READ` | `200 OK`; list |
| `evaluatePromotion_returns200_true` | `POST /api/v1/pos/promotions/{id}/evaluate` | Bearer `POS_READ`; conditions met | `200 OK`; `{"result":true}` |
| `evaluatePromotion_returns200_false` | `POST /api/v1/pos/promotions/{id}/evaluate` | Bearer `POS_READ`; not met | `200 OK`; `{"result":false}` |
| `validateCoupon_returns200_valid` | `POST /api/v1/pos/coupons/validate` | Bearer `POS_READ`; valid code | `200 OK`; coupon dto |
| `validateCoupon_returns422_expired` | `POST /api/v1/pos/coupons/validate` | Bearer `POS_READ`; expired | `422 Unprocessable Entity` |
| `redeemCoupon_returns200` | `POST /api/v1/pos/coupons/redeem` | Bearer `POS_WRITE` | `200 OK` |
