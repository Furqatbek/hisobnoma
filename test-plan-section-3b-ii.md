# Section 3b-ii: Inventory — Purchase Orders, Receiving, Counts, Planning & Barcode — Test Plan

---

## 1. PurchaseOrderService Unit Tests

Framework: JUnit 5 + Mockito.

### 1.1 CRUD

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getPurchaseOrders_returnsPaged` | `getPurchaseOrders(tenantId, pageable)` | Multiple POs | Returns paged `PurchaseOrderDto` |
| `getPurchaseOrder_found_returnsDto` | `getPurchaseOrder(tenantId, id)` | PO exists | Returns dto |
| `getPurchaseOrder_notFound_throwsNotFoundException` | `getPurchaseOrder(tenantId, id)` | Missing | Throws `NotFoundException` |
| `createPurchaseOrder_success_returnsDraft` | `createPurchaseOrder(tenantId, request)` | Valid vendor + lines | Returns dto with `status=DRAFT` |
| `createPurchaseOrder_vendorNotFound_throwsNotFoundException` | `createPurchaseOrder(tenantId, request)` | Vendor missing | Throws `NotFoundException` |
| `createPurchaseOrder_emptyLines_throwsValidationException` | `createPurchaseOrder(tenantId, request)` | No lines | Throws `ValidationException` |
| `updatePurchaseOrder_success` | `updatePurchaseOrder(tenantId, id, request)` | PO in DRAFT | Returns updated dto |
| `updatePurchaseOrder_released_throwsBusinessException` | `updatePurchaseOrder(tenantId, id, request)` | Status=RELEASED | Throws `BusinessException` |

### 1.2 Status Transitions

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `releasePurchaseOrder_draft_becomesReleased` | `releasePurchaseOrder(tenantId, id)` | DRAFT | Status RELEASED |
| `releasePurchaseOrder_alreadyReleased_throwsBusinessException` | `releasePurchaseOrder(tenantId, id)` | Already RELEASED | Throws `BusinessException` |
| `releasePurchaseOrder_cancelled_throwsBusinessException` | `releasePurchaseOrder(tenantId, id)` | CANCELLED | Throws `BusinessException` |
| `cancelPurchaseOrder_draft_becomesCancelled` | `cancelPurchaseOrder(tenantId, id)` | DRAFT | Status CANCELLED |
| `cancelPurchaseOrder_released_becomesCancelled` | `cancelPurchaseOrder(tenantId, id)` | RELEASED | Status CANCELLED |
| `cancelPurchaseOrder_fullyReceived_throwsBusinessException` | `cancelPurchaseOrder(tenantId, id)` | All lines received | Throws `BusinessException` |

### 1.3 `receivePurchaseOrder`

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `receivePurchaseOrder_released_validQty_updatesStock` | `receivePurchaseOrder(tenantId, id, request)` | RELEASED; qty ≤ ordered | Stock updated; movement RECEIPT created |
| `receivePurchaseOrder_notReleased_throwsBusinessException` | `receivePurchaseOrder(tenantId, id, request)` | Not RELEASED | Throws `BusinessException` |
| `receivePurchaseOrder_overReceive_throwsBusinessException` | `receivePurchaseOrder(tenantId, id, request)` | qty > ordered | Throws `BusinessException` |

---

## 2. ReceivingService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getReceivingOrders_returnsPaged` | `getReceivingOrders(tenantId, pageable)` | Multiple | Returns paged |
| `getReceivingOrder_found_returnsDto` | `getReceivingOrder(tenantId, id)` | Exists | Returns dto |
| `getReceivingOrder_notFound_throwsNotFoundException` | `getReceivingOrder(tenantId, id)` | Missing | Throws `NotFoundException` |
| `getReceivingOrdersByPO_returnsList` | `getReceivingOrdersForPO(tenantId, poId)` | 2 receiving orders for PO | Returns 2 |
| `createReceivingOrder_success` | `createReceivingOrder(tenantId, request)` | PO in RELEASED status | Returns dto with PENDING status |
| `createReceivingOrder_poNotFound_throwsNotFoundException` | `createReceivingOrder(tenantId, request)` | PO missing | Throws `NotFoundException` |
| `createReceivingOrder_poNotReleased_throwsBusinessException` | `createReceivingOrder(tenantId, request)` | PO DRAFT | Throws `BusinessException` |
| `updateReceivingOrder_success` | `updateReceivingOrder(tenantId, id, request)` | Not COMPLETED | Returns updated |
| `updateReceivingOrder_completed_throwsBusinessException` | `updateReceivingOrder(tenantId, id, request)` | COMPLETED | Throws `BusinessException` |
| `receiveLines_validQty_createsMovement` | `receiveLines(tenantId, id, lines)` | qty>0 per line | StockMovement RECEIPT created |
| `receiveLines_zeroQty_lineSkipped` | `receiveLines(tenantId, id, lines)` | qty=0 for line | Line skipped; no movement |
| `receiveLines_overQuantity_throwsBusinessException` | `receiveLines(tenantId, id, lines)` | qty > ordered qty | Throws `BusinessException` |
| `completeReceiving_success_stockPosted` | `completeReceiving(tenantId, id)` | Lines received | Status COMPLETED; stock posted |
| `completeReceiving_alreadyCompleted_throwsBusinessException` | `completeReceiving(tenantId, id)` | Already COMPLETED | Throws `BusinessException` |

---

## 3. InventoryCountService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getInventoryCounts_returnsPaged` | `getInventoryCounts(tenantId, pageable)` | Multiple | Returns paged |
| `createInventoryCount_success_returnsDraft` | `createInventoryCount(tenantId, request)` | Valid location | Returns dto with DRAFT |
| `createInventoryCount_locationNotFound_throwsNotFoundException` | `createInventoryCount(tenantId, request)` | Location missing | Throws `NotFoundException` |
| `updateInventoryCount_success` | `updateInventoryCount(tenantId, id, request)` | Not COMPLETED | Returns updated |
| `updateInventoryCount_completed_throwsBusinessException` | `updateInventoryCount(tenantId, id, request)` | COMPLETED | Throws `BusinessException` |
| `recordLineCount_success` | `recordLineCount(tenantId, id, lineId, qty)` | Count IN_PROGRESS | Qty recorded |
| `recordLineCount_negativeQty_throwsValidationException` | `recordLineCount(tenantId, id, lineId, qty)` | qty < 0 | Throws `ValidationException` |
| `recordLineCount_notInProgress_throwsBusinessException` | `recordLineCount(tenantId, id, lineId, qty)` | Count is DRAFT | Throws `BusinessException` |
| `completeCount_success_createsAdjustments` | `completeCount(tenantId, id)` | Lines counted, variances exist | COMPLETED; ADJUSTMENT movements created |
| `completeCount_noLinesCounted_throwsBusinessException` | `completeCount(tenantId, id)` | No lines counted | Throws `BusinessException` |

---

## 4. InventoryPlanningService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `getReorderSuggestions_returnsBelowReorderPoint` | `getReorderSuggestions(tenantId)` | 2 below reorder point | Returns 2 suggestions |
| `getReorderSuggestions_allAbove_returnsEmpty` | `getReorderSuggestions(tenantId)` | All above reorder | Returns empty list |
| `performAbcAnalysis_classifiesCorrectly` | `performAbcAnalysis(tenantId)` | Top 20% by revenue = A, next 30% = B, rest = C | A/B/C correctly assigned |
| `getSlowMovingProducts_returnsNoMovementIn90Days` | `getSlowMovingProducts(tenantId, 90)` | No movement for 90 days | Returns those products |
| `getSlowMovingProducts_allActive_returnsEmpty` | `getSlowMovingProducts(tenantId, 90)` | All products have recent movement | Returns empty list |
| `getDeadStock_returnsNeverMovedProducts` | `getDeadStock(tenantId)` | Zero movement ever | Returns dead stock |
| `getDeadStock_allMoving_returnsEmpty` | `getDeadStock(tenantId)` | All have movements | Returns empty list |

---

## 5. BarcodeService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `generateBarcode_success` | `generateBarcode(tenantId, productId)` | Product exists | Returns barcode bytes |
| `generateBarcode_productNotFound_throwsNotFoundException` | `generateBarcode(tenantId, productId)` | Product missing | Throws `NotFoundException` |
| `validateBarcode_validEan13_returnsTrue` | `validateBarcode("4006381333931")` | Valid EAN-13 | Returns true |
| `validateBarcode_invalidChecksum_returnsFalse` | `validateBarcode("4006381333932")` | Wrong check digit | Returns false |
| `validateBarcode_empty_returnsFalse` | `validateBarcode("")` | Empty string | Returns false |
| `validateBarcode_null_returnsFalse` | `validateBarcode(null)` | Null input | Returns false |
| `getProductByBarcode_found_returnsDto` | `getProductByBarcode(tenantId, barcode)` | Product has barcode | Returns `ProductDto` |
| `getProductByBarcode_notFound_throwsNotFoundException` | `getProductByBarcode(tenantId, barcode)` | No product | Throws `NotFoundException` |

---

## 6. SkuGeneratorService Unit Tests

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `generateSku_returnsUniqueFormattedSku` | `generateSku(tenantId)` | First call | Returns "PROD-000001" |
| `generateSequentialSku_firstCall` | `generateSequentialSku(tenantId, "PROD")` | First for prefix | Returns "PROD-000001" |
| `generateSequentialSku_secondCall` | `generateSequentialSku(tenantId, "PROD")` | Existing sequence | Returns "PROD-000002" |
| `generateVariantSku_appendsSuffix` | `generateVariantSku("SHIRT-001", "RED-M")` | Valid parent + variant | Returns "SHIRT-001-RED-M" |
| `generateSkuFromName_normalizesToSlug` | `generateSkuFromName(tenantId, "Apple iPhone")` | Mixed case with space | Returns slug+sequence |
| `skuExists_returnsTrue_whenExists` | `skuExists(tenantId, sku)` | SKU in DB | Returns true |
| `skuExists_returnsFalse_whenNotExists` | `skuExists(tenantId, sku)` | SKU not in DB | Returns false |

---

## 7. Repository Tests (`@DataJpaTest`)

| Test Name | Method | Scenario | Expected Outcome |
|---|---|---|---|
| `PurchaseOrderRepository_findByVendorId` | `findByVendorId(vendorId)` | 3 POs for vendor | Returns 3 |
| `PurchaseOrderRepository_findByStatus` | `findByStatus(tenantId, RELEASED)` | Mix | Returns only RELEASED |
| `PurchaseOrderLineRepository_findByPurchaseOrderId` | `findByPurchaseOrderId(poId)` | 4 lines | Returns 4 |
| `ReceivingOrderRepository_findByPurchaseOrderId` | `findByPurchaseOrderId(poId)` | 2 receivings | Returns 2 |
| `ReceivingOrderRepository_findByStatus` | `findByStatus(tenantId, COMPLETED)` | Mix | Returns COMPLETED |
| `InventoryCountRepository_findByLocationId` | `findByLocationId(locId)` | 2 counts | Returns 2 |
| `InventoryCountRepository_findByStatus` | `findByStatus(tenantId, DRAFT)` | Mix | Returns DRAFT |

---

## 8. Integration Tests — PO, Receiving, Count Controllers

| Test Name | HTTP Method + Path | Auth/Permission | Expected HTTP Status + Body |
|---|---|---|---|
| `getPurchaseOrders_returns200` | `GET /api/v1/inventory/purchase-orders` | Bearer `INVENTORY_READ` | `200 OK`; paged |
| `getPurchaseOrders_returns403` | `GET /api/v1/inventory/purchase-orders` | No permission | `403 Forbidden` |
| `createPurchaseOrder_returns201` | `POST /api/v1/inventory/purchase-orders` | Bearer `INVENTORY_WRITE`; valid | `201 Created`; status=DRAFT |
| `createPurchaseOrder_returns404_badVendor` | `POST /api/v1/inventory/purchase-orders` | Bearer `INVENTORY_WRITE` | `404 Not Found` |
| `createPurchaseOrder_returns400_emptyLines` | `POST /api/v1/inventory/purchase-orders` | Bearer `INVENTORY_WRITE` | `400 Bad Request` |
| `releasePurchaseOrder_returns200` | `PUT /api/v1/inventory/purchase-orders/{id}/release` | Bearer `INVENTORY_WRITE` | `200 OK`; status=RELEASED |
| `releasePurchaseOrder_returns422_alreadyReleased` | `PUT /api/v1/inventory/purchase-orders/{id}/release` | Bearer `INVENTORY_WRITE` | `422 Unprocessable Entity` |
| `cancelPurchaseOrder_returns200` | `PUT /api/v1/inventory/purchase-orders/{id}/cancel` | Bearer `INVENTORY_WRITE` | `200 OK`; status=CANCELLED |
| `cancelPurchaseOrder_returns422_fullyReceived` | `PUT /api/v1/inventory/purchase-orders/{id}/cancel` | Bearer `INVENTORY_WRITE` | `422 Unprocessable Entity` |
| `receivePurchaseOrder_returns200` | `POST /api/v1/inventory/purchase-orders/{id}/receive` | Bearer `INVENTORY_WRITE` | `200 OK`; stock updated |
| `receivePurchaseOrder_returns422_notReleased` | `POST /api/v1/inventory/purchase-orders/{id}/receive` | Bearer `INVENTORY_WRITE` | `422 Unprocessable Entity` |
| `getReceivingOrders_returns200` | `GET /api/v1/inventory/receiving` | Bearer `INVENTORY_READ` | `200 OK` |
| `createReceivingOrder_returns201` | `POST /api/v1/inventory/receiving` | Bearer `INVENTORY_WRITE`; RELEASED PO | `201 Created` |
| `createReceivingOrder_returns422_poNotReleased` | `POST /api/v1/inventory/receiving` | Bearer `INVENTORY_WRITE` | `422 Unprocessable Entity` |
| `receiveLines_returns200` | `POST /api/v1/inventory/receiving/{id}/receive-lines` | Bearer `INVENTORY_WRITE` | `200 OK` |
| `receiveLines_returns422_overQty` | `POST /api/v1/inventory/receiving/{id}/receive-lines` | Bearer `INVENTORY_WRITE` | `422 Unprocessable Entity` |
| `completeReceiving_returns200` | `POST /api/v1/inventory/receiving/{id}/complete` | Bearer `INVENTORY_WRITE` | `200 OK`; status=COMPLETED |
| `getInventoryCounts_returns200` | `GET /api/v1/inventory/counts` | Bearer `INVENTORY_READ` | `200 OK` |
| `createInventoryCount_returns201` | `POST /api/v1/inventory/counts` | Bearer `INVENTORY_WRITE`; valid location | `201 Created`; status=DRAFT |
| `recordLineCount_returns200` | `PUT /api/v1/inventory/counts/{id}/lines/{lineId}` | Bearer `INVENTORY_WRITE` | `200 OK` |
| `recordLineCount_returns400_negativeQty` | `PUT /api/v1/inventory/counts/{id}/lines/{lineId}` | Bearer `INVENTORY_WRITE`; qty=-1 | `400 Bad Request` |
| `completeCount_returns200` | `POST /api/v1/inventory/counts/{id}/complete` | Bearer `INVENTORY_WRITE` | `200 OK`; status=COMPLETED |
